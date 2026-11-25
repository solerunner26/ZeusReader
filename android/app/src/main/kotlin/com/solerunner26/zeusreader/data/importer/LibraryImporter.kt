package com.solerunner26.zeusreader.data.importer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.solerunner26.zeusreader.data.local.EditionEntity
import com.solerunner26.zeusreader.data.local.FileAssetEntity
import com.solerunner26.zeusreader.data.local.ImageBookPageEntity
import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.WorkEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ImportSource(
    val displayName: String,
    val origin: String = "local_import",
    val openStream: () -> InputStream?
)

data class ImportedBookInfo(
    val workId: String,
    val editionId: String,
    val title: String,
    val format: String,
    val fileCount: Int
)

data class DuplicateImportInfo(
    val displayName: String,
    val sha256: String
)

data class RejectedImportInfo(
    val displayName: String,
    val reason: String
)

data class ImportBatchReport(
    val importedBooks: List<ImportedBookInfo> = emptyList(),
    val duplicates: List<DuplicateImportInfo> = emptyList(),
    val rejected: List<RejectedImportInfo> = emptyList()
) {
    val importedFileCount: Int
        get() = importedBooks.sumOf { it.fileCount }
}

@Singleton
class LibraryImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao
) {
    private val originalsDir: File
        get() = File(context.filesDir, "library_originals").apply { mkdirs() }

    suspend fun importUris(
        uris: List<Uri>,
        origin: String = "picker",
        imageBookTitleOverride: String? = null
    ): ImportBatchReport {
        val sources = uris.map { uri ->
            ImportSource(
                displayName = resolveDisplayName(context.contentResolver, uri),
                origin = origin,
                openStream = { context.contentResolver.openInputStream(uri) }
            )
        }
        return importSources(sources, imageBookTitleOverride = imageBookTitleOverride)
    }

    suspend fun importSources(
        sources: List<ImportSource>,
        imageBookTitleOverride: String? = null
    ): ImportBatchReport = withContext(Dispatchers.IO) {
        if (sources.isEmpty()) return@withContext ImportBatchReport()

        val importedBooks = mutableListOf<ImportedBookInfo>()
        val duplicates = mutableListOf<DuplicateImportInfo>()
        val rejected = mutableListOf<RejectedImportInfo>()

        data class ValidatedTempFile(
            val displayName: String,
            val origin: String,
            val format: SupportedImportFormat,
            val tempFile: File,
            val sha256: String,
            val byteSize: Long
        )

        val pendingImages = mutableListOf<ValidatedTempFile>()

        for (source in sources) {
            val partFile = File(originalsDir, "${UUID.randomUUID()}.part")
            try {
                val stream = source.openStream()
                if (stream == null) {
                    rejected.add(RejectedImportInfo(source.displayName, "Unable to open input stream"))
                    continue
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var totalBytes = 0L
                stream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            totalBytes += read
                        }
                    }
                }

                when (val validation = ImportValidator.validateFile(partFile, source.displayName)) {
                    is ImportValidationResult.Rejected -> {
                        partFile.delete()
                        rejected.add(RejectedImportInfo(source.displayName, validation.reason))
                    }

                    is ImportValidationResult.Valid -> {
                        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                        val existing = libraryDao.findFileAssetBySha256(sha256)
                        if (existing != null) {
                            partFile.delete()
                            duplicates.add(DuplicateImportInfo(source.displayName, sha256))
                        } else {
                            val validated = ValidatedTempFile(
                                displayName = source.displayName,
                                origin = source.origin,
                                format = validation.format,
                                tempFile = partFile,
                                sha256 = sha256,
                                byteSize = totalBytes
                            )
                            if (validation.format.isImage) {
                                pendingImages.add(validated)
                            } else {
                                val finalFile = commitImmutableFile(validated.tempFile, validated.sha256, validated.format.extension)
                                val book = persistSingleDocument(
                                    title = cleanTitleFromFilename(validated.displayName),
                                    format = validated.format.dbFormatName,
                                    byteSize = validated.byteSize,
                                    sha256 = validated.sha256,
                                    origin = validated.origin,
                                    localPath = finalFile.absolutePath
                                )
                                importedBooks.add(book)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                partFile.delete()
                rejected.add(
                    RejectedImportInfo(
                        source.displayName,
                        t.message ?: "Import failed"
                    )
                )
            }
        }

        // Group imported images from this batch into one Image Book Edition with ordered pages.
        if (pendingImages.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val workId = UUID.randomUUID().toString()
            val editionId = UUID.randomUUID().toString()
            val title = imageBookTitleOverride?.takeIf { it.isNotBlank() }
                ?: if (pendingImages.size == 1) {
                    cleanTitleFromFilename(pendingImages.first().displayName)
                } else {
                    "${cleanTitleFromFilename(pendingImages.first().displayName)} (${pendingImages.size} pages)"
                }

            // Unknown author/language/year stay blank per GEMINI.md §2.3
            libraryDao.upsertWork(
                WorkEntity(
                    id = workId,
                    title = title,
                    authors = "",
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
            )
            libraryDao.upsertEdition(
                EditionEntity(
                    id = editionId,
                    workId = workId
                )
            )

            val pageEntities = mutableListOf<ImageBookPageEntity>()
            pendingImages.forEachIndexed { index, img ->
                val finalFile = commitImmutableFile(img.tempFile, img.sha256, img.format.extension)
                val assetId = UUID.randomUUID().toString()
                libraryDao.insertFileAsset(
                    FileAssetEntity(
                        id = assetId,
                        editionId = editionId,
                        format = img.format.dbFormatName,
                        byteSize = img.byteSize,
                        sha256 = img.sha256,
                        origin = img.origin,
                        localUri = finalFile.absolutePath,
                        transferStage = "AvailableOffline",
                        createdAtEpochMs = now + index
                    )
                )
                pageEntities.add(
                    ImageBookPageEntity(
                        id = UUID.randomUUID().toString(),
                        editionId = editionId,
                        fileAssetId = assetId,
                        pageOrder = index,
                        displayName = img.displayName,
                        localPath = finalFile.absolutePath
                    )
                )
            }
            libraryDao.insertImageBookPages(pageEntities)
            importedBooks.add(
                ImportedBookInfo(
                    workId = workId,
                    editionId = editionId,
                    title = title,
                    format = "IMAGE_BOOK",
                    fileCount = pendingImages.size
                )
            )
        }

        ImportBatchReport(
            importedBooks = importedBooks,
            duplicates = duplicates,
            rejected = rejected
        )
    }

    private suspend fun persistSingleDocument(
        title: String,
        format: String,
        byteSize: Long,
        sha256: String,
        origin: String,
        localPath: String
    ): ImportedBookInfo {
        val now = System.currentTimeMillis()
        val workId = UUID.randomUUID().toString()
        val editionId = UUID.randomUUID().toString()
        val assetId = UUID.randomUUID().toString()

        // Unknown metadata stays blank per GEMINI.md §2.3
        libraryDao.upsertWork(
            WorkEntity(
                id = workId,
                title = title,
                authors = "",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        libraryDao.upsertEdition(
            EditionEntity(
                id = editionId,
                workId = workId
            )
        )
        libraryDao.insertFileAsset(
            FileAssetEntity(
                id = assetId,
                editionId = editionId,
                format = format,
                byteSize = byteSize,
                sha256 = sha256,
                origin = origin,
                localUri = localPath,
                transferStage = "AvailableOffline",
                createdAtEpochMs = now
            )
        )
        return ImportedBookInfo(
            workId = workId,
            editionId = editionId,
            title = title,
            format = format,
            fileCount = 1
        )
    }

    private fun commitImmutableFile(partFile: File, sha256: String, extension: String): File {
        val target = File(originalsDir, "$sha256.$extension")
        if (!target.exists()) {
            if (!partFile.renameTo(target)) {
                partFile.copyTo(target, overwrite = false)
                partFile.delete()
            }
            target.setReadOnly()
        } else {
            partFile.delete()
        }
        return target
    }

    private fun cleanTitleFromFilename(fileName: String): String =
        fileName.substringBeforeLast('.').ifBlank { "Untitled" }.replace('_', ' ').trim()

    private fun resolveDisplayName(resolver: ContentResolver, uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "document"
    }
}

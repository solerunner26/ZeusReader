package com.solerunner26.zeusreader.data.importer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.WatchedFolderEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class WatchedFolderSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val watchedFolderManager: WatchedFolderManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            watchedFolderManager.scanAllFolders()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "zeusreader_watched_folder_periodic"
        const val IMMEDIATE_WORK_NAME = "zeusreader_watched_folder_now"
    }
}

@Singleton
class WatchedFolderManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryDao: LibraryDao,
    private val libraryImporter: LibraryImporter
) {

    suspend fun registerFolder(treeUri: Uri): WatchedFolderEntity {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        val docTree = DocumentFile.fromTreeUri(context, treeUri)
        val displayName = docTree?.name?.takeIf { it.isNotBlank() }
            ?: treeUri.lastPathSegment?.substringAfterLast(':')
            ?: "Watched Folder"

        val entity = WatchedFolderEntity(
            id = UUID.randomUUID().toString(),
            treeUri = treeUri.toString(),
            displayName = displayName,
            addedAtEpochMs = System.currentTimeMillis()
        )
        libraryDao.insertWatchedFolder(entity)
        schedulePeriodicSync()
        return entity
    }

    suspend fun removeFolder(folderId: String) {
        libraryDao.deleteWatchedFolder(folderId)
    }

    suspend fun scanAllFolders(): ImportBatchReport {
        val folders = libraryDao.getWatchedFolders()
        val allBooks = mutableListOf<ImportedBookInfo>()
        val allDuplicates = mutableListOf<DuplicateImportInfo>()
        val allRejected = mutableListOf<RejectedImportInfo>()

        for (folder in folders) {
            val uri = Uri.parse(folder.treeUri)
            val tree = DocumentFile.fromTreeUri(context, uri) ?: continue
            val candidateFiles = tree.listFiles()
                .filter { it.isFile && !it.name.isNullOrBlank() }
                .sortedBy { it.name.orEmpty() }

            val sources = candidateFiles.map { file ->
                ImportSource(
                    displayName = file.name ?: "unknown",
                    origin = "watched_folder:${folder.displayName}",
                    openStream = { context.contentResolver.openInputStream(file.uri) }
                )
            }

            if (sources.isNotEmpty()) {
                val report = libraryImporter.importSources(
                    sources = sources,
                    imageBookTitleOverride = folder.displayName
                )
                allBooks.addAll(report.importedBooks)
                allDuplicates.addAll(report.duplicates)
                allRejected.addAll(report.rejected)
            }
            libraryDao.markWatchedFolderScanned(folder.id, System.currentTimeMillis())
        }

        return ImportBatchReport(
            importedBooks = allBooks,
            duplicates = allDuplicates,
            rejected = allRejected
        )
    }

    fun triggerImmediateSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            WatchedFolderSyncWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WatchedFolderSyncWorker>().build()
        )
    }

    fun schedulePeriodicSync() {
        val workManager = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<WatchedFolderSyncWorker>(30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WatchedFolderSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

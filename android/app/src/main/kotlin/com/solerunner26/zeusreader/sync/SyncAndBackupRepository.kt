package com.solerunner26.zeusreader.sync

import android.util.Base64
import com.solerunner26.zeusreader.data.local.PassageDao
import com.solerunner26.zeusreader.data.local.PassageEntity
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.data.local.PassageTagEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupManifest(
    val mode: String,
    val schemaVersion: Int = 8,
    val passageFolderCount: Int,
    val passageCount: Int,
    val passageTagCount: Int,
    val sha256: String
)

@Serializable
data class PassageBackupBundle(
    val folders: List<PassageFolderDto>,
    val passages: List<PassageDto>,
    val tags: List<PassageTagDto>
)

@Serializable
data class PassageFolderDto(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
data class PassageDto(
    val id: String,
    val bookId: String,
    val editionId: String? = null,
    val folderId: String? = null,
    val text: String,
    val originalText: String,
    val locatorJson: String,
    val pageLabel: String? = null,
    val chapterTitle: String? = null,
    val language: String,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val shareCount: Int = 0
)

@Serializable
data class PassageTagDto(
    val passageId: String,
    val tag: String
)

@Singleton
class SyncAndBackupRepository @Inject constructor(
    private val passageDao: PassageDao,
    private val serverClient: PrivateServerClient
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun buildPassageSyncRecords(): List<SyncRecordDto> {
        val folders = passageDao.observeActiveFolders().first()
        val passages = passageDao.observeActivePassages().first()
        val tags = passageDao.observeAllTags().first()

        val out = mutableListOf<SyncRecordDto>()
        folders.forEach { f ->
            out += SyncRecordDto(
                record_type = "PassageFolder",
                record_id = f.id,
                payload_json = json.encodeToString(
                    PassageFolderDto.serializer(),
                    PassageFolderDto(f.id, f.name, f.sortOrder, f.createdAt, f.updatedAt, f.deletedAt)
                ),
                updated_at_ms = f.updatedAt,
                deleted_at_ms = f.deletedAt
            )
        }
        passages.forEach { p ->
            out += SyncRecordDto(
                record_type = "Passage",
                record_id = p.id,
                edition_id = p.editionId.orEmpty(),
                payload_json = json.encodeToString(
                    PassageDto.serializer(),
                    PassageDto(
                        p.id, p.bookId, p.editionId, p.folderId, p.text, p.originalText,
                        p.locatorJson, p.pageLabel, p.chapterTitle, p.language, p.note,
                        p.createdAt, p.updatedAt, p.deletedAt, p.shareCount
                    )
                ),
                updated_at_ms = p.updatedAt,
                deleted_at_ms = p.deletedAt
            )
        }
        tags.forEach { t ->
            out += SyncRecordDto(
                record_type = "PassageTag",
                record_id = "${t.passageId}::${t.tag}",
                payload_json = json.encodeToString(PassageTagDto.serializer(), PassageTagDto(t.passageId, t.tag)),
                updated_at_ms = System.currentTimeMillis()
            )
        }
        return out
    }

    suspend fun syncWithServer(baseUrl: String, token: String, sinceMs: Long = 0L): Result<Int> {
        val local = buildPassageSyncRecords()
        return serverClient.sync(baseUrl, token, SyncRequestDto(since_ms = sinceMs, records = local))
            .map { resp -> resp.records.size }
    }

    suspend fun exportBackupBundle(mode: String = "data_only"): Pair<BackupManifest, ByteArray> {
        val folders = passageDao.observeActiveFolders().first()
            .map { PassageFolderDto(it.id, it.name, it.sortOrder, it.createdAt, it.updatedAt, it.deletedAt) }
        val passages = passageDao.observeActivePassages().first()
            .map {
                PassageDto(
                    it.id, it.bookId, it.editionId, it.folderId, it.text, it.originalText,
                    it.locatorJson, it.pageLabel, it.chapterTitle, it.language, it.note,
                    it.createdAt, it.updatedAt, it.deletedAt, it.shareCount
                )
            }
        val tags = passageDao.observeAllTags().first()
            .map { PassageTagDto(it.passageId, it.tag) }

        val bundleBytes = json.encodeToString(
            PassageBackupBundle.serializer(),
            PassageBackupBundle(folders, passages, tags)
        ).toByteArray(Charsets.UTF_8)

        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(bundleBytes)
            .joinToString("") { "%02x".format(it) }

        val manifest = BackupManifest(
            mode = mode,
            passageFolderCount = folders.size,
            passageCount = passages.size,
            passageTagCount = tags.size,
            sha256 = sha256
        )
        return manifest to bundleBytes
    }

    suspend fun restoreBackupBundle(manifest: BackupManifest, bundleBytes: ByteArray): Boolean {
        val actualSha = MessageDigest.getInstance("SHA-256")
            .digest(bundleBytes)
            .joinToString("") { "%02x".format(it) }
        if (!actualSha.equals(manifest.sha256, ignoreCase = true)) return false

        val bundle = json.decodeFromString(PassageBackupBundle.serializer(), bundleBytes.toString(Charsets.UTF_8))
        bundle.folders.forEach { f ->
            passageDao.upsertFolder(
                PassageFolderEntity(f.id, f.name, f.sortOrder, f.createdAt, f.updatedAt, f.deletedAt)
            )
        }
        bundle.passages.forEach { p ->
            passageDao.upsertPassage(
                PassageEntity(
                    p.id, p.bookId, p.editionId, p.folderId, p.text, p.originalText,
                    p.locatorJson, p.pageLabel, p.chapterTitle, p.language, p.note,
                    p.createdAt, p.updatedAt, p.deletedAt, p.shareCount
                )
            )
        }
        if (bundle.tags.isNotEmpty()) {
            passageDao.insertTags(bundle.tags.map { PassageTagEntity(it.passageId, it.tag) })
        }
        return true
    }

    suspend fun uploadBackupToServer(baseUrl: String, token: String, mode: String = "data_only"): Result<BackupSummaryDto> {
        val (manifest, bytes) = exportBackupBundle(mode)
        val req = BackupCreateDto(
            mode = mode,
            manifest_json = json.encodeToString(BackupManifest.serializer(), manifest),
            payload_b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        )
        return serverClient.uploadBackup(baseUrl, token, req)
    }
}

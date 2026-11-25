package com.solerunner26.zeusreader.data

import com.solerunner26.zeusreader.data.local.PassageDao
import com.solerunner26.zeusreader.data.local.PassageEntity
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.data.local.PassageFtsEntity
import com.solerunner26.zeusreader.data.local.PassageTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PassageWithTags(
    val passage: PassageEntity,
    val tags: List<String>
)

@Singleton
class PassageRepository @Inject constructor(
    private val passageDao: PassageDao
) {
    val activeFolders: Flow<List<PassageFolderEntity>> = passageDao.observeActiveFolders()

    val activePassagesWithTags: Flow<List<PassageWithTags>> = combine(
        passageDao.observeActivePassages(),
        passageDao.observeAllTags()
    ) { passages, allTags ->
        val tagsByPassage = allTags.groupBy({ it.passageId }, { it.tag })
        passages.map { passage ->
            PassageWithTags(
                passage = passage,
                tags = tagsByPassage[passage.id].orEmpty().sorted()
            )
        }
    }

    fun observePassage(passageId: String): Flow<PassageEntity?> =
        passageDao.observePassageById(passageId)

    suspend fun getTags(passageId: String): List<String> =
        passageDao.getTagsForPassage(passageId)

    suspend fun createFolder(name: String, sortOrder: Int = 0): PassageFolderEntity {
        val trimmed = name.trim().ifBlank { "Folder" }
        val now = System.currentTimeMillis()
        val folder = PassageFolderEntity(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now
        )
        passageDao.upsertFolder(folder)
        return folder
    }

    suspend fun renameFolder(folder: PassageFolderEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        passageDao.upsertFolder(folder.copy(name = trimmed, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteFolderMovePassagesToUnsorted(folderId: String) {
        passageDao.deleteFolderAndMovePassagesToUnsorted(folderId, System.currentTimeMillis())
    }

    suspend fun savePassage(
        bookId: String,
        editionId: String?,
        folderId: String?,
        text: String,
        originalText: String,
        locatorJson: String,
        pageLabel: String?,
        chapterTitle: String?,
        language: String,
        note: String?,
        tags: List<String> = emptyList()
    ): PassageEntity {
        val now = System.currentTimeMillis()
        val entity = PassageEntity(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            editionId = editionId,
            folderId = folderId,
            text = text.trim(),
            originalText = originalText,
            locatorJson = locatorJson,
            pageLabel = pageLabel,
            chapterTitle = chapterTitle,
            language = language,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now
        )
        passageDao.upsertPassage(entity)
        updateTagsAndFts(entity, tags)
        return entity
    }

    suspend fun updatePassage(
        passage: PassageEntity,
        newText: String,
        newNote: String?,
        newFolderId: String?,
        newTags: List<String>
    ) {
        val updated = passage.copy(
            text = newText.trim().ifBlank { passage.text },
            note = newNote?.trim()?.takeIf { it.isNotEmpty() },
            folderId = newFolderId,
            updatedAt = System.currentTimeMillis()
        )
        passageDao.upsertPassage(updated)
        updateTagsAndFts(updated, newTags)
    }

    suspend fun softDeletePassage(passageId: String) {
        passageDao.softDeletePassage(passageId, System.currentTimeMillis())
    }

    suspend fun restorePassage(passageId: String) {
        passageDao.restorePassage(passageId, System.currentTimeMillis())
    }

    suspend fun incrementShareCount(passageId: String) {
        passageDao.incrementShareCount(passageId, System.currentTimeMillis())
    }

    suspend fun searchPassages(query: String): List<PassageEntity> {
        val sanitized = query.trim().replace("\"", "")
        if (sanitized.isBlank()) return emptyList()
        return passageDao.searchActivePassages("$sanitized*")
    }

    suspend fun hardDeleteExpired(retentionDays: Long = 30L): Int {
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        val deletedPassages = passageDao.hardDeleteExpiredPassages(cutoff)
        passageDao.hardDeleteExpiredFolders(cutoff)
        return deletedPassages
    }

    private suspend fun updateTagsAndFts(passage: PassageEntity, rawTags: List<String>) {
        val cleanTags = rawTags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        passageDao.deleteTagsForPassage(passage.id)
        if (cleanTags.isNotEmpty()) {
            passageDao.insertTags(cleanTags.map { PassageTagEntity(passage.id, it) })
        }
        passageDao.deleteFts(passage.id)
        passageDao.upsertFts(
            PassageFtsEntity(
                passageId = passage.id,
                text = passage.text,
                note = passage.note.orEmpty(),
                tagsText = cleanTags.joinToString(" ")
            )
        )
    }
}

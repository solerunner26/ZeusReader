package com.solerunner26.zeusreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PassageDao {

    @Query("SELECT * FROM passage_folders WHERE deleted_at IS NULL ORDER BY sort_order ASC, created_at ASC")
    fun observeActiveFolders(): Flow<List<PassageFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: PassageFolderEntity)

    @Query("UPDATE passages SET folder_id = NULL, updated_at = :updatedAt WHERE folder_id = :folderId AND deleted_at IS NULL")
    suspend fun moveFolderPassagesToUnsorted(folderId: String, updatedAt: Long)

    @Query("UPDATE passage_folders SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :folderId")
    suspend fun softDeleteFolder(folderId: String, deletedAt: Long)

    @Transaction
    suspend fun deleteFolderAndMovePassagesToUnsorted(folderId: String, nowEpochMs: Long) {
        moveFolderPassagesToUnsorted(folderId, nowEpochMs)
        softDeleteFolder(folderId, nowEpochMs)
    }

    @Query("SELECT * FROM passages WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    fun observeActivePassages(): Flow<List<PassageEntity>>

    @Query("SELECT * FROM passages WHERE deleted_at IS NULL AND (:editionId IS NULL OR edition_id = :editionId)")
    fun observeActivePassagesForEdition(editionId: String?): Flow<List<PassageEntity>>

    @Query("SELECT * FROM passages WHERE id = :passageId LIMIT 1")
    suspend fun getPassageById(passageId: String): PassageEntity?

    @Query("SELECT * FROM passages WHERE id = :passageId LIMIT 1")
    fun observePassageById(passageId: String): Flow<PassageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPassage(passage: PassageEntity)

    @Query("UPDATE passages SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :passageId")
    suspend fun softDeletePassage(passageId: String, deletedAt: Long)

    @Query("UPDATE passages SET deleted_at = NULL, updated_at = :updatedAt WHERE id = :passageId")
    suspend fun restorePassage(passageId: String, updatedAt: Long)

    @Query("UPDATE passages SET share_count = share_count + 1, updated_at = :updatedAt WHERE id = :passageId")
    suspend fun incrementShareCount(passageId: String, updatedAt: Long)

    @Query("SELECT tag FROM passage_tags WHERE passage_id = :passageId ORDER BY tag ASC")
    suspend fun getTagsForPassage(passageId: String): List<String>

    @Query("SELECT * FROM passage_tags")
    fun observeAllTags(): Flow<List<PassageTagEntity>>

    @Query("DELETE FROM passage_tags WHERE passage_id = :passageId")
    suspend fun deleteTagsForPassage(passageId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<PassageTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFts(fts: PassageFtsEntity)

    @Query("DELETE FROM passage_search_fts WHERE passage_id = :passageId")
    suspend fun deleteFts(passageId: String)

    @Query(
        """
        SELECT p.* FROM passages p
        INNER JOIN passage_search_fts fts ON p.id = fts.passage_id
        WHERE p.deleted_at IS NULL AND passage_search_fts MATCH :ftsQuery
        ORDER BY p.updated_at DESC
        """
    )
    suspend fun searchActivePassages(ftsQuery: String): List<PassageEntity>

    @Query("DELETE FROM passages WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffEpochMs")
    suspend fun hardDeleteExpiredPassages(cutoffEpochMs: Long): Int

    @Query("DELETE FROM passage_folders WHERE deleted_at IS NOT NULL AND deleted_at < :cutoffEpochMs")
    suspend fun hardDeleteExpiredFolders(cutoffEpochMs: Long): Int
}

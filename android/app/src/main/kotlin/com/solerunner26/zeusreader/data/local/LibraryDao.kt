package com.solerunner26.zeusreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query(
        """
        SELECT * FROM works
        WHERE deleted_at_epoch_ms IS NULL
        ORDER BY updated_at_epoch_ms DESC
        """
    )
    fun observeActiveWorks(): Flow<List<WorkEntity>>

    @Query(
        """
        SELECT works.* FROM works
        JOIN works_fts ON works.rowid = works_fts.rowid
        WHERE works_fts MATCH :ftsQuery
          AND works.deleted_at_epoch_ms IS NULL
        ORDER BY works.updated_at_epoch_ms DESC
        """
    )
    fun searchActiveWorks(ftsQuery: String): Flow<List<WorkEntity>>

    @Query(
        """
        SELECT
            w.id AS workId,
            e.id AS editionId,
            w.title AS title,
            w.authors AS authors,
            COALESCE(NULLIF(e.language, ''), w.language) AS language,
            e.publication_year AS publicationYear,
            COALESCE(
                (SELECT CASE WHEN COUNT(*) > 0 THEN 'IMAGE_BOOK' ELSE NULL END FROM image_book_pages ibp WHERE ibp.edition_id = e.id),
                (SELECT fa.format FROM file_assets fa WHERE fa.edition_id = e.id ORDER BY fa.created_at_epoch_ms ASC LIMIT 1),
                ''
            ) AS primaryFormat,
            COALESCE((SELECT SUM(fa.byte_size) FROM file_assets fa WHERE fa.edition_id = e.id), 0) AS totalByteSize,
            (SELECT COUNT(*) FROM file_assets fa WHERE fa.edition_id = e.id) AS assetCount,
            (SELECT COUNT(*) FROM image_book_pages ibp WHERE ibp.edition_id = e.id) AS imagePageCount,
            (SELECT fa.local_uri FROM file_assets fa WHERE fa.edition_id = e.id ORDER BY fa.created_at_epoch_ms ASC LIMIT 1) AS primaryLocalPath,
            rp.page_index AS lastPageIndex,
            w.updated_at_epoch_ms AS updatedAtEpochMs
        FROM works w
        JOIN editions e ON e.work_id = w.id AND e.deleted_at_epoch_ms IS NULL
        LEFT JOIN reading_progress rp ON rp.edition_id = e.id
        WHERE w.deleted_at_epoch_ms IS NULL
        ORDER BY w.updated_at_epoch_ms DESC
        """
    )
    fun observeLibrarySummaries(): Flow<List<LibraryBookSummary>>

    @Query(
        """
        SELECT
            w.id AS workId,
            e.id AS editionId,
            w.title AS title,
            w.authors AS authors,
            COALESCE(NULLIF(e.language, ''), w.language) AS language,
            e.publication_year AS publicationYear,
            COALESCE(
                (SELECT CASE WHEN COUNT(*) > 0 THEN 'IMAGE_BOOK' ELSE NULL END FROM image_book_pages ibp WHERE ibp.edition_id = e.id),
                (SELECT fa.format FROM file_assets fa WHERE fa.edition_id = e.id ORDER BY fa.created_at_epoch_ms ASC LIMIT 1),
                ''
            ) AS primaryFormat,
            COALESCE((SELECT SUM(fa.byte_size) FROM file_assets fa WHERE fa.edition_id = e.id), 0) AS totalByteSize,
            (SELECT COUNT(*) FROM file_assets fa WHERE fa.edition_id = e.id) AS assetCount,
            (SELECT COUNT(*) FROM image_book_pages ibp WHERE ibp.edition_id = e.id) AS imagePageCount,
            (SELECT fa.local_uri FROM file_assets fa WHERE fa.edition_id = e.id ORDER BY fa.created_at_epoch_ms ASC LIMIT 1) AS primaryLocalPath,
            rp.page_index AS lastPageIndex,
            w.updated_at_epoch_ms AS updatedAtEpochMs
        FROM works w
        JOIN works_fts ON w.rowid = works_fts.rowid
        JOIN editions e ON e.work_id = w.id AND e.deleted_at_epoch_ms IS NULL
        LEFT JOIN reading_progress rp ON rp.edition_id = e.id
        WHERE works_fts MATCH :ftsQuery
          AND w.deleted_at_epoch_ms IS NULL
        ORDER BY w.updated_at_epoch_ms DESC
        """
    )
    fun searchLibrarySummaries(ftsQuery: String): Flow<List<LibraryBookSummary>>

    @Query("SELECT * FROM file_assets WHERE sha256 = :sha256 LIMIT 1")
    suspend fun findFileAssetBySha256(sha256: String): FileAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWork(work: WorkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEdition(edition: EditionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFileAsset(fileAsset: FileAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageBookPages(pages: List<ImageBookPageEntity>)

    @Query("SELECT * FROM image_book_pages WHERE edition_id = :editionId ORDER BY page_order ASC")
    fun observeImageBookPages(editionId: String): Flow<List<ImageBookPageEntity>>

    @Query("SELECT * FROM image_book_pages WHERE edition_id = :editionId ORDER BY page_order ASC")
    suspend fun getImageBookPages(editionId: String): List<ImageBookPageEntity>

    @Query("UPDATE image_book_pages SET page_order = :newOrder WHERE id = :pageId")
    suspend fun updateImagePageOrder(pageId: String, newOrder: Int)

    @Transaction
    suspend fun reorderImageBookPages(editionId: String, orderedPageIds: List<String>) {
        val existing = getImageBookPages(editionId).associateBy { it.id }
        var order = 0
        orderedPageIds.forEach { pageId ->
            if (existing.containsKey(pageId)) {
                updateImagePageOrder(pageId, order++)
            }
        }
        // Preserve any trailing pages not explicitly included in orderedPageIds
        existing.keys.filterNot { it in orderedPageIds }.forEach { pageId ->
            updateImagePageOrder(pageId, order++)
        }
    }

    @Query("SELECT * FROM watched_folders ORDER BY added_at_epoch_ms DESC")
    fun observeWatchedFolders(): Flow<List<WatchedFolderEntity>>

    @Query("SELECT * FROM watched_folders")
    suspend fun getWatchedFolders(): List<WatchedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWatchedFolder(folder: WatchedFolderEntity): Long

    @Query("UPDATE watched_folders SET last_scanned_epoch_ms = :scannedAtEpochMs WHERE id = :folderId")
    suspend fun markWatchedFolderScanned(folderId: String, scannedAtEpochMs: Long)

    @Query("DELETE FROM watched_folders WHERE id = :folderId")
    suspend fun deleteWatchedFolder(folderId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE edition_id = :editionId LIMIT 1")
    suspend fun getReadingProgress(editionId: String): ReadingProgressEntity?

    @Query("SELECT COUNT(*) FROM works WHERE deleted_at_epoch_ms IS NULL")
    suspend fun countWorks(): Int

    @Query("SELECT COUNT(*) FROM editions WHERE deleted_at_epoch_ms IS NULL")
    suspend fun countEditions(): Int

    @Query("SELECT COUNT(*) FROM file_assets")
    suspend fun countFileAssets(): Int

    @Query(
        """
        SELECT * FROM highlights
        WHERE edition_id = :editionId AND deleted_at_epoch_ms IS NULL
        ORDER BY created_at_epoch_ms ASC
        """
    )
    fun observeHighlights(editionId: String): Flow<List<HighlightEntity>>

    @Query(
        """
        SELECT * FROM highlights
        WHERE edition_id = :editionId AND deleted_at_epoch_ms IS NULL
        ORDER BY created_at_epoch_ms ASC
        """
    )
    suspend fun getHighlights(editionId: String): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHighlight(highlight: HighlightEntity)

    @Query(
        """
        UPDATE highlights
        SET deleted_at_epoch_ms = :deletedAtEpochMs,
            updated_at_epoch_ms = :deletedAtEpochMs,
            version = version + 1
        WHERE id = :highlightId
        """
    )
    suspend fun softDeleteHighlight(highlightId: String, deletedAtEpochMs: Long)

    @Query("SELECT * FROM unsupported_files ORDER BY created_at_epoch_ms DESC")
    fun observeUnsupportedFiles(): Flow<List<UnsupportedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnsupportedFile(entry: UnsupportedFileEntity)

    @Query("DELETE FROM unsupported_files")
    suspend fun clearUnsupportedFiles()

    @Query(
        """
        SELECT * FROM vocabulary_words
        WHERE deleted_at_epoch_ms IS NULL
        ORDER BY next_review_epoch_ms ASC, created_at_epoch_ms DESC
        """
    )
    fun observeVocabularyWords(): Flow<List<VocabWordEntity>>

    @Query(
        """
        SELECT * FROM vocabulary_words
        WHERE deleted_at_epoch_ms IS NULL AND next_review_epoch_ms <= :nowEpochMs
        ORDER BY next_review_epoch_ms ASC
        """
    )
    suspend fun getDueVocabularyWords(nowEpochMs: Long): List<VocabWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVocabularyWord(word: VocabWordEntity)

    @Query(
        """
        UPDATE vocabulary_words
        SET deleted_at_epoch_ms = :deletedAtEpochMs,
            updated_at_epoch_ms = :deletedAtEpochMs,
            version = version + 1
        WHERE id = :wordId
        """
    )
    suspend fun softDeleteVocabularyWord(wordId: String, deletedAtEpochMs: Long)

    @Query(
        """
        SELECT * FROM translation_cache
        WHERE file_version_sha256 = :fileSha256
          AND chapter_or_section_id = :sectionId
          AND source_passage_hash = :passageHash
          AND target_lang = :targetLang
          AND engine_id_version = :engineVersion
          AND glossary_revision = :glossaryRevision
        ORDER BY sentence_index ASC
        """
    )
    suspend fun getCachedPassageTranslations(
        fileSha256: String,
        sectionId: String,
        passageHash: String,
        targetLang: String,
        engineVersion: String,
        glossaryRevision: Int
    ): List<TranslationCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTranslationCacheEntries(entries: List<TranslationCacheEntity>)

    @Query("SELECT * FROM book_glossary WHERE edition_id = :editionId ORDER BY term ASC")
    fun observeBookGlossary(editionId: String): Flow<List<BookGlossaryEntity>>

    @Query("SELECT * FROM book_glossary WHERE edition_id = :editionId ORDER BY term ASC")
    suspend fun getBookGlossary(editionId: String): List<BookGlossaryEntity>

    @Query("SELECT COALESCE(MAX(glossary_revision), 0) FROM book_glossary WHERE edition_id = :editionId")
    suspend fun getCurrentGlossaryRevision(editionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlossaryTerm(term: BookGlossaryEntity)

    @Query("UPDATE book_glossary SET glossary_revision = :newRevision WHERE edition_id = :editionId")
    suspend fun bumpGlossaryRevisionForEdition(editionId: String, newRevision: Int)

    @Query("DELETE FROM book_glossary WHERE id = :termId")
    suspend fun deleteGlossaryTerm(termId: String)

    @Query("SELECT fa.sha256 FROM file_assets fa WHERE fa.edition_id = :editionId ORDER BY fa.created_at_epoch_ms ASC LIMIT 1")
    suspend fun getPrimaryFileSha256(editionId: String): String?
}

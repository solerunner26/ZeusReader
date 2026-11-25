package com.solerunner26.zeusreader.data

import com.solerunner26.zeusreader.data.local.HighlightEntity
import com.solerunner26.zeusreader.data.local.ImageBookPageEntity
import com.solerunner26.zeusreader.data.local.LibraryBookSummary
import com.solerunner26.zeusreader.data.local.LibraryDao
import com.solerunner26.zeusreader.data.local.ReadingProgressEntity
import com.solerunner26.zeusreader.data.local.UnsupportedFileEntity
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.data.local.WatchedFolderEntity
import com.solerunner26.zeusreader.data.local.WorkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface LibraryRepository {
    fun observeWorks(): Flow<List<WorkEntity>>
    fun searchWorks(ftsQuery: String): Flow<List<WorkEntity>>
    fun observeLibrarySummaries(): Flow<List<LibraryBookSummary>>
    fun searchLibrarySummaries(query: String): Flow<List<LibraryBookSummary>>
    fun observeImageBookPages(editionId: String): Flow<List<ImageBookPageEntity>>
    suspend fun reorderImageBookPages(editionId: String, orderedPageIds: List<String>)
    fun observeWatchedFolders(): Flow<List<WatchedFolderEntity>>
    suspend fun getReadingProgress(editionId: String): ReadingProgressEntity?
    suspend fun saveReadingProgress(progress: ReadingProgressEntity)
    fun observeHighlights(editionId: String): Flow<List<HighlightEntity>>
    suspend fun getHighlights(editionId: String): List<HighlightEntity>
    suspend fun saveHighlight(highlight: HighlightEntity)
    suspend fun deleteHighlight(highlightId: String)
    fun observeUnsupportedFiles(): Flow<List<UnsupportedFileEntity>>
    suspend fun clearUnsupportedFiles()
    fun observeVocabularyWords(): Flow<List<VocabWordEntity>>
    suspend fun saveVocabularyWord(word: VocabWordEntity)
    suspend fun deleteVocabularyWord(wordId: String)
}

class RoomLibraryRepository @Inject constructor(
    private val libraryDao: LibraryDao
) : LibraryRepository {

    override fun observeWorks(): Flow<List<WorkEntity>> = libraryDao.observeActiveWorks()

    override fun searchWorks(ftsQuery: String): Flow<List<WorkEntity>> =
        libraryDao.searchActiveWorks(toFtsMatchExpression(ftsQuery))

    override fun observeLibrarySummaries(): Flow<List<LibraryBookSummary>> =
        libraryDao.observeLibrarySummaries()

    override fun searchLibrarySummaries(query: String): Flow<List<LibraryBookSummary>> =
        libraryDao.searchLibrarySummaries(toFtsMatchExpression(query))

    override fun observeImageBookPages(editionId: String): Flow<List<ImageBookPageEntity>> =
        libraryDao.observeImageBookPages(editionId)

    override suspend fun reorderImageBookPages(editionId: String, orderedPageIds: List<String>) {
        libraryDao.reorderImageBookPages(editionId, orderedPageIds)
    }

    override fun observeWatchedFolders(): Flow<List<WatchedFolderEntity>> =
        libraryDao.observeWatchedFolders()

    override suspend fun getReadingProgress(editionId: String): ReadingProgressEntity? =
        libraryDao.getReadingProgress(editionId)

    override suspend fun saveReadingProgress(progress: ReadingProgressEntity) {
        libraryDao.upsertReadingProgress(progress)
    }

    override fun observeHighlights(editionId: String): Flow<List<HighlightEntity>> =
        libraryDao.observeHighlights(editionId)

    override suspend fun getHighlights(editionId: String): List<HighlightEntity> =
        libraryDao.getHighlights(editionId)

    override suspend fun saveHighlight(highlight: HighlightEntity) {
        libraryDao.upsertHighlight(highlight)
    }

    override suspend fun deleteHighlight(highlightId: String) {
        libraryDao.softDeleteHighlight(highlightId, System.currentTimeMillis())
    }

    override fun observeUnsupportedFiles(): Flow<List<UnsupportedFileEntity>> =
        libraryDao.observeUnsupportedFiles()

    override suspend fun clearUnsupportedFiles() {
        libraryDao.clearUnsupportedFiles()
    }

    override fun observeVocabularyWords(): Flow<List<VocabWordEntity>> =
        libraryDao.observeVocabularyWords()

    override suspend fun saveVocabularyWord(word: VocabWordEntity) {
        libraryDao.upsertVocabularyWord(word)
    }

    override suspend fun deleteVocabularyWord(wordId: String) {
        libraryDao.softDeleteVocabularyWord(wordId, System.currentTimeMillis())
    }

    companion object {
        /** Converts raw user search input into safe prefix FTS4 tokens. */
        internal fun toFtsMatchExpression(rawQuery: String): String {
            val tokens = rawQuery
                .trim()
                .split(Regex("\\s+"))
                .map { it.replace(Regex("[^\\p{L}\\p{Nd}_]"), "") }
                .filter { it.isNotBlank() }
            return tokens.joinToString(" ") { "$it*" }
        }
    }
}

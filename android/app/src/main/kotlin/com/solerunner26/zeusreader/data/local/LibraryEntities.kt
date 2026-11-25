package com.solerunner26.zeusreader.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** Canonical intellectual work grouping (never auto-merged purely by title+author across editions). */
@Entity(
    tableName = "works",
    indices = [
        Index(value = ["title"]),
        Index(value = ["language"])
    ]
)
data class WorkEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val authors: String,
    val description: String = "",
    val language: String = "",
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
    val version: Long = 1L,
    @ColumnInfo(name = "deleted_at_epoch_ms")
    val deletedAtEpochMs: Long? = null
)

/** Full-text search index over Work title, authors, and description. */
@Fts4(contentEntity = WorkEntity::class)
@Entity(tableName = "works_fts")
data class LibrarySearchFtsEntity(
    val title: String,
    val authors: String,
    val description: String
)

/** Specific edition of a Work (publisher, year, language, ISBN, edition notes). */
@Entity(
    tableName = "editions",
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["work_id"]),
        Index(value = ["isbn"]),
        Index(value = ["publication_year"]),
        Index(value = ["language"])
    ]
)
data class EditionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "work_id")
    val workId: String,
    val publisher: String = "",
    @ColumnInfo(name = "publication_year")
    val publicationYear: Int? = null,
    val language: String = "",
    val isbn: String = "",
    @ColumnInfo(name = "edition_notes")
    val editionNotes: String = "",
    val version: Long = 1L,
    @ColumnInfo(name = "deleted_at_epoch_ms")
    val deletedAtEpochMs: Long? = null
)

/** Physical file asset tied to an Edition, deduplicated by SHA-256. */
@Entity(
    tableName = "file_assets",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["edition_id"]),
        Index(value = ["sha256"], unique = true),
        Index(value = ["format"]),
        Index(value = ["transfer_stage"])
    ]
)
data class FileAssetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "edition_id")
    val editionId: String,
    val format: String,
    @ColumnInfo(name = "byte_size")
    val byteSize: Long,
    val sha256: String,
    val origin: String,
    @ColumnInfo(name = "source_file_sha256")
    val sourceFileSha256: String? = null,
    @ColumnInfo(name = "local_uri")
    val localUri: String? = null,
    @ColumnInfo(name = "transfer_stage")
    val transferStage: String = "AvailableOffline",
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

/** Ordered page entry inside an image book Edition (JPG/PNG). */
@Entity(
    tableName = "image_book_pages",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["file_asset_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["edition_id", "page_order"]),
        Index(value = ["file_asset_id"])
    ]
)
data class ImageBookPageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "edition_id")
    val editionId: String,
    @ColumnInfo(name = "file_asset_id")
    val fileAssetId: String,
    @ColumnInfo(name = "page_order")
    val pageOrder: Int,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "local_path")
    val localPath: String
)

/** Persisted SAF tree URI monitored for automatic local imports. */
@Entity(
    tableName = "watched_folders",
    indices = [Index(value = ["tree_uri"], unique = true)]
)
data class WatchedFolderEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "tree_uri")
    val treeUri: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "added_at_epoch_ms")
    val addedAtEpochMs: Long,
    @ColumnInfo(name = "last_scanned_epoch_ms")
    val lastScannedEpochMs: Long? = null
)

/** Persisted viewport & page resume state per Edition. */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReadingProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "edition_id")
    val editionId: String,
    @ColumnInfo(name = "page_index")
    val pageIndex: Int = 0,
    @ColumnInfo(name = "viewport_scale")
    val viewportScale: Float = 1f,
    @ColumnInfo(name = "scroll_x")
    val scrollX: Float = 0f,
    @ColumnInfo(name = "scroll_y")
    val scrollY: Float = 0f,
    @ColumnInfo(name = "locator_json")
    val locatorJson: String? = null,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)

/** Read-model summary displayed in the Library screen. */
data class LibraryBookSummary(
    val workId: String,
    val editionId: String,
    val title: String,
    val authors: String,
    val language: String,
    val publicationYear: Int?,
    val primaryFormat: String,
    val totalByteSize: Long,
    val assetCount: Int,
    val imagePageCount: Int,
    val primaryLocalPath: String?,
    val lastPageIndex: Int?,
    val updatedAtEpochMs: Long
) {
    val isImageBook: Boolean
        get() = imagePageCount > 0 || primaryFormat.uppercase() in setOf("JPG", "JPEG", "PNG", "IMAGE_BOOK")
}

/** User highlight & optional note anchored by UnifiedLocator JSON or page number. */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["edition_id"]),
        Index(value = ["updated_at_epoch_ms"])
    ]
)
data class HighlightEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "edition_id")
    val editionId: String,
    @ColumnInfo(name = "locator_json")
    val locatorJson: String = "",
    @ColumnInfo(name = "selected_text")
    val selectedText: String,
    val note: String = "",
    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#FFE082",
    @ColumnInfo(name = "chapter_title")
    val chapterTitle: String = "",
    @ColumnInfo(name = "page_number")
    val pageNumber: Int? = null,
    val version: Long = 1L,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
    @ColumnInfo(name = "deleted_at_epoch_ms")
    val deletedAtEpochMs: Long? = null
)

/** Unsupported or rejected format retained for transparent user feedback (e.g., .oxp, .kfx, DRM, oversized TIFF). */
@Entity(
    tableName = "unsupported_files",
    indices = [Index(value = ["created_at_epoch_ms"])]
)
data class UnsupportedFileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "detected_type")
    val detectedType: String,
    val reason: String,
    @ColumnInfo(name = "byte_size")
    val byteSize: Long,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

/** Saved vocabulary word with book + sentence context and SM-2 spaced repetition state. */
@Entity(
    tableName = "vocabulary_words",
    indices = [
        Index(value = ["lemma", "target_lang"]),
        Index(value = ["next_review_epoch_ms"])
    ]
)
data class VocabWordEntity(
    @PrimaryKey
    val id: String,
    val word: String,
    val lemma: String,
    @ColumnInfo(name = "target_lang")
    val targetLang: String,
    @ColumnInfo(name = "english_meaning")
    val englishMeaning: String,
    @ColumnInfo(name = "translated_meaning")
    val translatedMeaning: String,
    @ColumnInfo(name = "book_title")
    val bookTitle: String = "",
    @ColumnInfo(name = "edition_id")
    val editionId: String? = null,
    @ColumnInfo(name = "sentence_context")
    val sentenceContext: String = "",
    val repetitions: Int = 0,
    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 0,
    @ColumnInfo(name = "ease_factor")
    val easeFactor: Float = 2.5f,
    @ColumnInfo(name = "next_review_epoch_ms")
    val nextReviewEpochMs: Long,
    val version: Long = 1L,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
    @ColumnInfo(name = "deleted_at_epoch_ms")
    val deletedAtEpochMs: Long? = null
)

/** Passage-anchored sentence translation cache keyed per GEMINI.md §3.2. */
@Entity(
    tableName = "translation_cache",
    primaryKeys = [
        "file_version_sha256",
        "chapter_or_section_id",
        "source_passage_hash",
        "sentence_index",
        "target_lang",
        "engine_id_version",
        "glossary_revision"
    ],
    indices = [
        Index(value = ["file_version_sha256", "chapter_or_section_id"])
    ]
)
data class TranslationCacheEntity(
    @ColumnInfo(name = "file_version_sha256")
    val fileVersionSha256: String,
    @ColumnInfo(name = "chapter_or_section_id")
    val chapterOrSectionId: String,
    @ColumnInfo(name = "source_passage_hash")
    val sourcePassageHash: String,
    @ColumnInfo(name = "sentence_index")
    val sentenceIndex: Int,
    @ColumnInfo(name = "target_lang")
    val targetLang: String,
    @ColumnInfo(name = "engine_id_version")
    val engineIdVersion: String,
    @ColumnInfo(name = "glossary_revision")
    val glossaryRevision: Int,
    @ColumnInfo(name = "source_sentence")
    val sourceSentence: String,
    @ColumnInfo(name = "translated_sentence")
    val translatedSentence: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

/** Per-book "Don't translate" glossary term; changing terms bumps glossary revision. */
@Entity(
    tableName = "book_glossary",
    foreignKeys = [
        ForeignKey(
            entity = EditionEntity::class,
            parentColumns = ["id"],
            childColumns = ["edition_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["edition_id"]),
        Index(value = ["edition_id", "term"], unique = true)
    ]
)
data class BookGlossaryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "edition_id")
    val editionId: String,
    val term: String,
    @ColumnInfo(name = "glossary_revision")
    val glossaryRevision: Int,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long
)

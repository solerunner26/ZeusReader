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

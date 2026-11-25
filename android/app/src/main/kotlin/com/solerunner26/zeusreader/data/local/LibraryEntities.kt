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

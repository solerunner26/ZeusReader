package com.solerunner26.zeusreader.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "passage_folders")
data class PassageFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)

@Entity(
    tableName = "passages",
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["folder_id"])
    ]
)
data class PassageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "edition_id") val editionId: String?,
    @ColumnInfo(name = "folder_id") val folderId: String?,
    val text: String,
    @ColumnInfo(name = "original_text") val originalText: String,
    @ColumnInfo(name = "locator_json") val locatorJson: String,
    @ColumnInfo(name = "page_label") val pageLabel: String?,
    @ColumnInfo(name = "chapter_title") val chapterTitle: String?,
    val language: String,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "share_count") val shareCount: Int = 0
)

@Entity(
    tableName = "passage_tags",
    primaryKeys = ["passage_id", "tag"]
)
data class PassageTagEntity(
    @ColumnInfo(name = "passage_id") val passageId: String,
    val tag: String
)

@Fts4
@Entity(tableName = "passage_search_fts")
data class PassageFtsEntity(
    @ColumnInfo(name = "passage_id") val passageId: String,
    val text: String,
    val note: String,
    @ColumnInfo(name = "tags_text") val tagsText: String
)

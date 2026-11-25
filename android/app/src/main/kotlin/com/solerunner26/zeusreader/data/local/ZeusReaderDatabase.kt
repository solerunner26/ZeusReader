package com.solerunner26.zeusreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkEntity::class,
        EditionEntity::class,
        FileAssetEntity::class,
        LibrarySearchFtsEntity::class,
        ImageBookPageEntity::class,
        WatchedFolderEntity::class,
        ReadingProgressEntity::class,
        HighlightEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class ZeusReaderDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `highlights` (
                        `id` TEXT NOT NULL,
                        `edition_id` TEXT NOT NULL,
                        `locator_json` TEXT NOT NULL,
                        `selected_text` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `color_hex` TEXT NOT NULL,
                        `chapter_title` TEXT NOT NULL,
                        `page_number` INTEGER,
                        `version` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        `deleted_at_epoch_ms` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`edition_id`) REFERENCES `editions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_highlights_edition_id` ON `highlights` (`edition_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_highlights_updated_at_epoch_ms` ON `highlights` (`updated_at_epoch_ms`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `reading_progress` ADD COLUMN `locator_json` TEXT")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `image_book_pages` (
                        `id` TEXT NOT NULL,
                        `edition_id` TEXT NOT NULL,
                        `file_asset_id` TEXT NOT NULL,
                        `page_order` INTEGER NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `local_path` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`edition_id`) REFERENCES `editions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`file_asset_id`) REFERENCES `file_assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_image_book_pages_edition_id_page_order` ON `image_book_pages` (`edition_id`, `page_order`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_image_book_pages_file_asset_id` ON `image_book_pages` (`file_asset_id`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `watched_folders` (
                        `id` TEXT NOT NULL,
                        `tree_uri` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `added_at_epoch_ms` INTEGER NOT NULL,
                        `last_scanned_epoch_ms` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_watched_folders_tree_uri` ON `watched_folders` (`tree_uri`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_progress` (
                        `edition_id` TEXT NOT NULL,
                        `page_index` INTEGER NOT NULL,
                        `viewport_scale` REAL NOT NULL,
                        `scroll_x` REAL NOT NULL,
                        `scroll_y` REAL NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`edition_id`),
                        FOREIGN KEY(`edition_id`) REFERENCES `editions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

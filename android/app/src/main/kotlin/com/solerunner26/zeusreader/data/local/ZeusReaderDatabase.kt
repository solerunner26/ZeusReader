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
        HighlightEntity::class,
        UnsupportedFileEntity::class,
        VocabWordEntity::class,
        TranslationCacheEntity::class,
        BookGlossaryEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class ZeusReaderDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `translation_cache` (
                        `file_version_sha256` TEXT NOT NULL,
                        `chapter_or_section_id` TEXT NOT NULL,
                        `source_passage_hash` TEXT NOT NULL,
                        `sentence_index` INTEGER NOT NULL,
                        `target_lang` TEXT NOT NULL,
                        `engine_id_version` TEXT NOT NULL,
                        `glossary_revision` INTEGER NOT NULL,
                        `source_sentence` TEXT NOT NULL,
                        `translated_sentence` TEXT NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`file_version_sha256`, `chapter_or_section_id`, `source_passage_hash`, `sentence_index`, `target_lang`, `engine_id_version`, `glossary_revision`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_translation_cache_file_version_sha256_chapter_or_section_id` ON `translation_cache` (`file_version_sha256`, `chapter_or_section_id`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_glossary` (
                        `id` TEXT NOT NULL,
                        `edition_id` TEXT NOT NULL,
                        `term` TEXT NOT NULL,
                        `glossary_revision` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`edition_id`) REFERENCES `editions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_book_glossary_edition_id` ON `book_glossary` (`edition_id`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_book_glossary_edition_id_term` ON `book_glossary` (`edition_id`, `term`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vocabulary_words` (
                        `id` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `lemma` TEXT NOT NULL,
                        `target_lang` TEXT NOT NULL,
                        `english_meaning` TEXT NOT NULL,
                        `translated_meaning` TEXT NOT NULL,
                        `book_title` TEXT NOT NULL,
                        `edition_id` TEXT,
                        `sentence_context` TEXT NOT NULL,
                        `repetitions` INTEGER NOT NULL,
                        `interval_days` INTEGER NOT NULL,
                        `ease_factor` REAL NOT NULL,
                        `next_review_epoch_ms` INTEGER NOT NULL,
                        `version` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        `deleted_at_epoch_ms` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_words_lemma_target_lang` ON `vocabulary_words` (`lemma`, `target_lang`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_words_next_review_epoch_ms` ON `vocabulary_words` (`next_review_epoch_ms`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `unsupported_files` (
                        `id` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `detected_type` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `byte_size` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_unsupported_files_created_at_epoch_ms` ON `unsupported_files` (`created_at_epoch_ms`)"
                )
            }
        }

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

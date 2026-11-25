package com.solerunner26.zeusreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkEntity::class,
        EditionEntity::class,
        FileAssetEntity::class,
        LibrarySearchFtsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ZeusReaderDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}

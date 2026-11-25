package com.solerunner26.zeusreader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM file_assets WHERE sha256 = :sha256 LIMIT 1")
    suspend fun findFileAssetBySha256(sha256: String): FileAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWork(work: WorkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEdition(edition: EditionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFileAsset(fileAsset: FileAssetEntity): Long
}

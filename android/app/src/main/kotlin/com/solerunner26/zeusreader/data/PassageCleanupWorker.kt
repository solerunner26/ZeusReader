package com.solerunner26.zeusreader.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import androidx.room.Room
import java.util.concurrent.TimeUnit

/**
 * Periodically hard-deletes soft-deleted passages and folders older than 30 days.
 */
class PassageCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            ZeusReaderDatabase::class.java,
            "zeusreader.db"
        )
            .addMigrations(
                ZeusReaderDatabase.MIGRATION_1_2,
                ZeusReaderDatabase.MIGRATION_2_3,
                ZeusReaderDatabase.MIGRATION_3_4,
                ZeusReaderDatabase.MIGRATION_4_5,
                ZeusReaderDatabase.MIGRATION_5_6,
                ZeusReaderDatabase.MIGRATION_6_7,
                ZeusReaderDatabase.MIGRATION_7_8
            )
            .build()
        return try {
            val cutoff = System.currentTimeMillis() - RETENTION_MS
            db.passageDao().hardDeleteExpiredPassages(cutoff)
            db.passageDao().hardDeleteExpiredFolders(cutoff)
            Result.success()
        } finally {
            db.close()
        }
    }

    companion object {
        private const val WORK_NAME = "zeus_passage_cleanup_30d"
        private const val RETENTION_MS = 30L * 24L * 60L * 60L * 1000L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PassageCleanupWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

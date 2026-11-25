package com.solerunner26.zeusreader.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solerunner26.zeusreader.data.PassageRepository
import com.solerunner26.zeusreader.data.local.ZeusReaderDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SyncAndBackupTest {

    private lateinit var sourceDb: ZeusReaderDatabase
    private lateinit var targetDb: ZeusReaderDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sourceDb = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        targetDb = Room.inMemoryDatabaseBuilder(context, ZeusReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
    }

    @Test
    fun `backup exports passages folders and tags and restores cleanly on fresh install`() = runTest {
        val passageRepo = PassageRepository(sourceDb.passageDao())
        val folder = passageRepo.createFolder("Synced Quotes")
        passageRepo.savePassage(
            bookId = "w1",
            editionId = "ed1",
            folderId = folder.id,
            text = "Clean backup restore test.",
            originalText = "Clean backup restore test.",
            locatorJson = """{"href":"c1.xhtml"}""",
            pageLabel = "p. 2",
            chapterTitle = "Ch 1",
            language = "en",
            note = "Verify SHA-256",
            tags = listOf("sync", "backup")
        )

        val client = PrivateServerClient()
        val syncRepo = SyncAndBackupRepository(sourceDb.passageDao(), client)
        val syncRecords = syncRepo.buildPassageSyncRecords()
        assertTrue(syncRecords.any { it.record_type == "PassageFolder" })
        assertTrue(syncRecords.any { it.record_type == "Passage" })
        assertTrue(syncRecords.any { it.record_type == "PassageTag" })

        val (manifest, bytes) = syncRepo.exportBackupBundle("data_only")
        assertEquals(1, manifest.passageFolderCount)
        assertEquals(1, manifest.passageCount)
        assertEquals(2, manifest.passageTagCount)

        val targetSyncRepo = SyncAndBackupRepository(targetDb.passageDao(), client)
        assertFalse(targetSyncRepo.restoreBackupBundle(manifest.copy(sha256 = "0000"), bytes))
        assertTrue(targetSyncRepo.restoreBackupBundle(manifest, bytes))

        val restoredPassages = targetDb.passageDao().observeActivePassages().first()
        assertEquals(1, restoredPassages.size)
        assertEquals("Clean backup restore test.", restoredPassages.first().text)
    }

    @Test
    fun `server offline degrades gracefully without throwing`() = runTest {
        val syncRepo = SyncAndBackupRepository(sourceDb.passageDao(), PrivateServerClient())
        val res = syncRepo.syncWithServer("http://127.0.0.1:59999", "token")
        assertTrue(res.isFailure)
    }
}

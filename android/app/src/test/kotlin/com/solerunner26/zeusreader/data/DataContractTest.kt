package com.solerunner26.zeusreader.data

import com.solerunner26.zeusreader.data.local.EditionEntity
import com.solerunner26.zeusreader.data.local.FileAssetEntity
import com.solerunner26.zeusreader.data.local.WorkEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataContractTest {

    @Test
    fun workEditionFileAsset_preserveBlankUnknownMetadataAndLineage() {
        val work = WorkEntity(
            id = "w-1",
            title = "Unknown Provenance Treatise",
            authors = "",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        val edition = EditionEntity(
            id = "e-1",
            workId = work.id
        )
        val originalAsset = FileAssetEntity(
            id = "f-1",
            editionId = edition.id,
            format = "EPUB",
            byteSize = 1024L,
            sha256 = "abc123",
            origin = "local_import",
            createdAtEpochMs = 1000L
        )

        assertEquals("", work.authors)
        assertNull(edition.publicationYear)
        assertNull(originalAsset.sourceFileSha256)
        assertEquals("AvailableOffline", originalAsset.transferStage)
    }
}

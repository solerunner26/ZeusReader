package com.solerunner26.zeusreader.reader.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageBitmapCacheTest {

    @Test
    fun enforcesMaxThreePageBitmapsInMemory() {
        val evicted = mutableListOf<Int>()
        val cache = PageBitmapCache<ByteArray>(
            maxPages = 3,
            maxBytes = 10_000L,
            sizeOf = { it.size.toLong() },
            onEvict = { page, _ -> evicted.add(page) }
        )

        cache.put(0, ByteArray(100))
        cache.put(1, ByteArray(100))
        cache.put(2, ByteArray(100))
        // Touch page 0 so page 1 becomes LRU
        assertNotNull(cache.get(0))

        cache.put(3, ByteArray(100))

        assertEquals(3, cache.size())
        assertEquals(listOf(1), evicted)
        assertNull(cache.get(1))
        assertNotNull(cache.get(0))
        assertNotNull(cache.get(2))
        assertNotNull(cache.get(3))
    }

    @Test
    fun enforcesDecodedMemoryBudgetAndRejectsOversizedSingleEntry() {
        val evicted = mutableListOf<Int>()
        val cache = PageBitmapCache<ByteArray>(
            maxPages = 3,
            maxBytes = 1_000L,
            sizeOf = { it.size.toLong() },
            onEvict = { page, _ -> evicted.add(page) }
        )

        assertTrue(cache.put(0, ByteArray(600)))
        assertTrue(cache.put(1, ByteArray(600)))

        assertEquals(1, cache.size())
        assertEquals(600L, cache.totalBytes())
        assertEquals(listOf(0), evicted)

        assertFalse(cache.put(2, ByteArray(1_500)))
        assertEquals(1, cache.size())
    }

    @Test
    fun computeBoundedDimensions_downsamplesOversizedScannedPageToBudget() {
        val (w, h) = MuPdfDocumentSession.computeBoundedDimensions(
            pageWidthPts = 2000f,
            pageHeightPts = 4000f,
            requestedWidthPx = 2200,
            maxBytes = 4L * 1024L * 1024L
        )
        val decodedBytes = w.toLong() * h.toLong() * 4L
        assertTrue("Expected decoded bytes $decodedBytes <= 4MB", decodedBytes <= 4L * 1024L * 1024L)
    }
}

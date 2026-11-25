package com.solerunner26.zeusreader.reader.pdf

import android.graphics.Bitmap

/**
 * Thread-safe bounded LRU cache enforcing both a strict max-page count (3 bitmaps per GEMINI.md §6)
 * and a decoded-byte memory budget.
 */
class PageBitmapCache<V : Any>(
    private val maxPages: Int = MAX_CACHED_PAGES,
    private val maxBytes: Long = DEFAULT_MEMORY_BUDGET_BYTES,
    private val sizeOf: (V) -> Long,
    private val onEvict: (Int, V) -> Unit = { _, _ -> }
) {
    private data class Entry<V>(val value: V, val bytes: Long)

    private val entries = LinkedHashMap<Int, Entry<V>>(maxPages + 1, 0.75f, true)
    private var currentBytes: Long = 0L

    @Synchronized
    fun get(pageIndex: Int): V? = entries[pageIndex]?.value

    @Synchronized
    fun put(pageIndex: Int, value: V): Boolean {
        val bytes = sizeOf(value).coerceAtLeast(0L)
        if (bytes > maxBytes) {
            // Do not retain a single entry exceeding the entire memory budget
            return false
        }

        entries.remove(pageIndex)?.let { previous ->
            currentBytes -= previous.bytes
            if (previous.value !== value) {
                onEvict(pageIndex, previous.value)
            }
        }

        entries[pageIndex] = Entry(value, bytes)
        currentBytes += bytes
        trimToBudget()
        return true
    }

    @Synchronized
    fun evictAll() {
        val snapshot = entries.entries.toList()
        entries.clear()
        currentBytes = 0L
        snapshot.forEach { (key, entry) -> onEvict(key, entry.value) }
    }

    @Synchronized
    fun size(): Int = entries.size

    @Synchronized
    fun totalBytes(): Long = currentBytes

    private fun trimToBudget() {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext() && (entries.size > maxPages || currentBytes > maxBytes)) {
            val eldest = iterator.next()
            iterator.remove()
            currentBytes -= eldest.value.bytes
            onEvict(eldest.key, eldest.value.value)
        }
    }

    companion object {
        const val MAX_CACHED_PAGES = 3
        const val DEFAULT_MEMORY_BUDGET_BYTES = 48L * 1024L * 1024L

        fun forAndroidBitmaps(
            maxPages: Int = MAX_CACHED_PAGES,
            maxBytes: Long = DEFAULT_MEMORY_BUDGET_BYTES
        ): PageBitmapCache<Bitmap> = PageBitmapCache(
            maxPages = maxPages,
            maxBytes = maxBytes,
            sizeOf = { bmp -> bmp.allocationByteCount.toLong() },
            onEvict = { _, _ -> /* Compose/GC reclaims cleanly without invalidating drawn frames */ }
        )
    }
}

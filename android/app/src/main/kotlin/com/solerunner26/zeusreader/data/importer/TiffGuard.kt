package com.solerunner26.zeusreader.data.importer

import java.io.File
import java.io.RandomAccessFile

sealed interface TiffGuardResult {
    data class Allowed(val pageCount: Int, val byteSize: Long) : TiffGuardResult
    data class Rejected(val reason: String) : TiffGuardResult
}

/**
 * Enforces TIFF limits from GEMINI.md §4.1 before decoding:
 * - byte size <= 30 MB
 * - pages <= 100
 * - decoded bitmap budget guarded by [com.solerunner26.zeusreader.reader.pdf.MuPdfDocumentSession]
 */
object TiffGuard {
    const val MAX_TIFF_BYTES = 30L * 1024L * 1024L
    const val MAX_TIFF_PAGES = 100

    fun validate(file: File, reportedByteSize: Long = file.length()): TiffGuardResult {
        if (reportedByteSize > MAX_TIFF_BYTES) {
            return TiffGuardResult.Rejected(
                "TIFF rejected: file size ${reportedByteSize / (1024 * 1024)} MB exceeds 30 MB limit."
            )
        }
        val pages = countPages(file)
        if (pages <= 0) {
            return TiffGuardResult.Rejected("TIFF rejected: unreadable IFD structure.")
        }
        if (pages > MAX_TIFF_PAGES) {
            return TiffGuardResult.Rejected(
                "TIFF rejected: $pages pages exceeds 100-page limit."
            )
        }
        return TiffGuardResult.Allowed(pageCount = pages, byteSize = reportedByteSize)
    }

    /** Counts classic TIFF IFD pages without decoding any raster image data. */
    fun countPages(file: File): Int = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val len = raf.length()
            if (len < 8L) return@use 0
            val b0 = raf.readUnsignedByte()
            val b1 = raf.readUnsignedByte()
            val littleEndian = when {
                b0 == 0x49 && b1 == 0x49 -> true  // II
                b0 == 0x4D && b1 == 0x4D -> false // MM
                else -> return@use 0
            }
            val magic = readUInt16(raf, littleEndian)
            if (magic != 42) return@use 0 // standard TIFF 42

            var ifdOffset = readUInt32(raf, littleEndian)
            var count = 0
            val visited = hashSetOf<Long>()
            while (ifdOffset in 8L until len && visited.add(ifdOffset) && count < 10_000) {
                raf.seek(ifdOffset)
                if (ifdOffset + 2L > len) break
                val entryCount = readUInt16(raf, littleEndian)
                val nextOffsetPos = ifdOffset + 2L + (entryCount.toLong() * 12L)
                if (nextOffsetPos + 4L > len) {
                    count++
                    break
                }
                count++
                raf.seek(nextOffsetPos)
                ifdOffset = readUInt32(raf, littleEndian)
            }
            count
        }
    }.getOrDefault(0)

    private fun readUInt16(raf: RandomAccessFile, le: Boolean): Int {
        val a = raf.readUnsignedByte()
        val b = raf.readUnsignedByte()
        return if (le) (b shl 8) or a else (a shl 8) or b
    }

    private fun readUInt32(raf: RandomAccessFile, le: Boolean): Long {
        val a = raf.readUnsignedByte().toLong()
        val b = raf.readUnsignedByte().toLong()
        val c = raf.readUnsignedByte().toLong()
        val d = raf.readUnsignedByte().toLong()
        return if (le) {
            (d shl 24) or (c shl 16) or (b shl 8) or a
        } else {
            (a shl 24) or (b shl 16) or (c shl 8) or d
        }
    }
}

package com.solerunner26.zeusreader.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ImportValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun validPdfJpgPng_areAccepted() {
        val pdf = tempFolder.newFile("textbook.pdf").apply {
            writeBytes("%PDF-1.7\n1 0 obj\n<<>>\nendobj\n".toByteArray())
        }
        val jpg = tempFolder.newFile("page_01.jpg").apply {
            writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10))
        }
        val png = tempFolder.newFile("scan.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00))
        }

        assertEquals(
            ImportValidationResult.Valid(SupportedImportFormat.PDF),
            ImportValidator.validateFile(pdf, "textbook.pdf")
        )
        assertEquals(
            ImportValidationResult.Valid(SupportedImportFormat.JPG),
            ImportValidator.validateFile(jpg, "page_01.jpg")
        )
        assertEquals(
            ImportValidationResult.Valid(SupportedImportFormat.PNG),
            ImportValidator.validateFile(png, "scan.png")
        )
    }

    @Test
    fun disguisedApkAsPdf_isRejected() {
        val fakePdf = tempFolder.newFile("disguised.pdf")
        ZipOutputStream(FileOutputStream(fakePdf)).use { zip ->
            zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zip.write("<manifest/>".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("classes.dex"))
            zip.write("dex\n035\u0000".toByteArray())
            zip.closeEntry()
        }

        val result = ImportValidator.validateFile(fakePdf, "disguised.pdf")
        assertTrue(result is ImportValidationResult.Rejected)
        assertEquals("android_apk", (result as ImportValidationResult.Rejected).detectedSignature)
    }

    @Test
    fun disguisedExecutablesAndScripts_areRejected() {
        val exeAsPdf = File(tempFolder.root, "malware.pdf").apply {
            writeBytes(byteArrayOf(0x4D, 0x5A, 0x90.toByte(), 0x00))
        }
        val elfAsJpg = File(tempFolder.root, "elf.jpg").apply {
            writeBytes(byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 0x02))
        }
        val scriptAsPng = File(tempFolder.root, "script.png").apply {
            writeBytes("#!/bin/sh\necho pwn\n".toByteArray())
        }
        val pngAsPdf = File(tempFolder.root, "spoofed.pdf").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        }

        assertTrue(ImportValidator.validateFile(exeAsPdf, "malware.pdf") is ImportValidationResult.Rejected)
        assertTrue(ImportValidator.validateFile(elfAsJpg, "elf.jpg") is ImportValidationResult.Rejected)
        assertTrue(ImportValidator.validateFile(scriptAsPng, "script.png") is ImportValidationResult.Rejected)
        assertTrue(ImportValidator.validateFile(pngAsPdf, "spoofed.pdf") is ImportValidationResult.Rejected)
    }
}

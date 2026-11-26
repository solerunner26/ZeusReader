package com.solerunner26.zeusreader.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SyncRecordDto(
    val record_type: String,
    val record_id: String,
    val edition_id: String = "",
    val payload_json: String,
    val base_version: Int = 0,
    val version: Int = 1,
    val updated_at_ms: Long,
    val deleted_at_ms: Long? = null
)

@Serializable
data class SyncRequestDto(
    val since_ms: Long = 0L,
    val records: List<SyncRecordDto> = emptyList()
)

@Serializable
data class SyncResponseDto(
    val server_time_ms: Long = 0L,
    val records: List<SyncRecordDto> = emptyList()
)

@Serializable
data class BackupCreateDto(
    val mode: String,
    val manifest_json: String,
    val payload_b64: String
)

@Serializable
data class BackupSummaryDto(
    val backup_id: String,
    val mode: String,
    val sha256: String,
    val byte_size: Int,
    val created_at_ms: Long
)

@Serializable
data class SlumDomainDto(
    val host: String,
    val role: String,
    val health: String
)

@Serializable
data class SlumProviderDto(
    val name: String,
    val overall_status: String,
    val domains: List<SlumDomainDto> = emptyList()
)

@Serializable
data class SlumStatusDto(
    val fetched_at_utc: String = "",
    val stale: Boolean = false,
    val providers: List<SlumProviderDto> = emptyList()
)

@Serializable
data class OnlineSearchResultDto(
    val source: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val language: String = "",
    val year: String = "",
    val format: String,
    val byte_size: Long? = null,
    val download_url: String,
    val idempotency_key: String
)

@Serializable
data class OnlineSearchResponseDto(
    val query: String,
    val results: List<OnlineSearchResultDto> = emptyList(),
    val warnings: List<String> = emptyList()
)

@Serializable
data class TransferCreateDto(
    val idempotency_key: String,
    val source: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val language: String = "",
    val year: String = "",
    val format: String,
    val download_url: String
)

@Serializable
data class TransferJobDto(
    val job_id: String,
    val idempotency_key: String,
    val source: String,
    val title: String,
    val format: String,
    val state: String,
    val progress_percent: Int = 0,
    val error_message: String? = null,
    val browser_url: String? = null,
    val file_sha256: String? = null,
    val byte_size: Long? = null
)

@Singleton
class PrivateServerClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun checkHealth(baseUrl: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = openConnection("$baseUrl/health", "GET", token)
            try {
                conn.responseCode in 200..299
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun sync(baseUrl: String, token: String, request: SyncRequestDto): Result<SyncResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection("$baseUrl/v1/sync", "POST", token)
                try {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(json.encodeToString(SyncRequestDto.serializer(), request))
                    }
                    val code = conn.responseCode
                    if (code !in 200..299) error("Sync failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(SyncResponseDto.serializer(), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun uploadBackup(baseUrl: String, token: String, request: BackupCreateDto): Result<BackupSummaryDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection("$baseUrl/v1/backups", "POST", token)
                try {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(json.encodeToString(BackupCreateDto.serializer(), request))
                    }
                    val code = conn.responseCode
                    if (code !in 200..299) error("Backup upload failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(BackupSummaryDto.serializer(), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun getSlumStatus(baseUrl: String, token: String): Result<SlumStatusDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection("${baseUrl.trimEnd('/')}/v1/slum/status", "GET", token)
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) error("SLUM status failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(SlumStatusDto.serializer(), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun searchOnline(baseUrl: String, token: String, query: String): Result<OnlineSearchResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val conn = openConnection("${baseUrl.trimEnd('/')}/v1/search?q=$encoded", "GET", token)
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) error("Search failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(OnlineSearchResponseDto.serializer(), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun enqueueTransfer(baseUrl: String, token: String, request: TransferCreateDto): Result<TransferJobDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection("${baseUrl.trimEnd('/')}/v1/transfers", "POST", token)
                try {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(json.encodeToString(TransferCreateDto.serializer(), request))
                    }
                    val code = conn.responseCode
                    if (code !in 200..299) error("Enqueue transfer failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(TransferJobDto.serializer(), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun listTransfers(baseUrl: String, token: String): Result<List<TransferJobDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection("${baseUrl.trimEnd('/')}/v1/transfers", "GET", token)
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) error("List transfers failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(TransferJobDto.serializer()), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun cancelTransfer(baseUrl: String, token: String, jobId: String): Result<TransferJobDto> =
        postEmpty("${baseUrl.trimEnd('/')}/v1/transfers/$jobId/cancel", token)

    suspend fun retryTransfer(baseUrl: String, token: String, jobId: String): Result<TransferJobDto> =
        postEmpty("${baseUrl.trimEnd('/')}/v1/transfers/$jobId/retry", token)

    suspend fun downloadTransferFile(
        baseUrl: String,
        token: String,
        jobId: String,
        destFile: java.io.File,
        expectedSha256: String? = null
    ): Result<java.io.File> = withContext(Dispatchers.IO) {
        runCatching {
            destFile.parentFile?.mkdirs()
            val tempFile = java.io.File(destFile.parentFile, "${destFile.name}.tmp")
            val conn = openConnection("${baseUrl.trimEnd('/')}/v1/transfers/$jobId/file", "GET", token)
            try {
                val code = conn.responseCode
                if (code !in 200..299) error("Download failed with HTTP $code")
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                conn.inputStream.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        val buf = ByteArray(16_384)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            digest.update(buf, 0, read)
                            output.write(buf, 0, read)
                        }
                    }
                }
                val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                if (!expectedSha256.isNullOrBlank() && !actualSha.equals(expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    error("SHA-256 verification failed")
                }
                if (destFile.exists()) destFile.delete()
                if (!tempFile.renameTo(destFile)) {
                    tempFile.copyTo(destFile, overwrite = true)
                    tempFile.delete()
                }
                destFile
            } finally {
                conn.disconnect()
            }
        }
    }

    private suspend fun postEmpty(url: String, token: String): Result<TransferJobDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection(url, "POST", token)
                try {
                    conn.doOutput = true
                    conn.outputStream.close()
                    val code = conn.responseCode
                    if (code !in 200..299) error("Transfer action failed with HTTP $code")
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    json.decodeFromString(TransferJobDto.serializer(), body)
                } finally {
                    conn.disconnect()
                }
            }
        }

    private fun openConnection(url: String, method: String, token: String): HttpURLConnection {
        val conn = (URL(url.trimEnd('/')).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
        }
        return conn
    }
}

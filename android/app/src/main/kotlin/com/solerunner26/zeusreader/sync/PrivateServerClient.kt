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

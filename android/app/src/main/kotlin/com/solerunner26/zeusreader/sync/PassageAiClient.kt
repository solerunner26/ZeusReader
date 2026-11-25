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
data class AiPassageRequestDto(
    val passage_text: String,
    val book_title: String = "",
    val target_language: String = "en"
)

@Serializable
data class AiExplanationResponseDto(
    val explanation: String,
    val model: String
)

@Serializable
data class AiTagsResponseDto(
    val tags: List<String> = emptyList(),
    val model: String = ""
)

/**
 * Phase 8 optional private-server AI client (Addendum B.6: "Explain this passage" and "Suggest tags").
 * Gracefully returns Result.failure when server is offline or AI is disabled.
 */
@Singleton
class PassageAiClient @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun explainPassage(
        baseUrl: String,
        token: String,
        passageText: String,
        bookTitle: String
    ): Result<String> = postAi(
        url = "${baseUrl.trimEnd('/')}/v1/ai/explain-passage",
        token = token,
        body = json.encodeToString(AiPassageRequestDto.serializer(), AiPassageRequestDto(passageText, bookTitle))
    ) { raw ->
        json.decodeFromString(AiExplanationResponseDto.serializer(), raw).explanation
    }

    suspend fun suggestTags(
        baseUrl: String,
        token: String,
        passageText: String,
        bookTitle: String
    ): Result<List<String>> = postAi(
        url = "${baseUrl.trimEnd('/')}/v1/ai/suggest-tags",
        token = token,
        body = json.encodeToString(AiPassageRequestDto.serializer(), AiPassageRequestDto(passageText, bookTitle))
    ) { raw ->
        json.decodeFromString(AiTagsResponseDto.serializer(), raw).tags
    }

    private suspend fun <T> postAi(
        url: String,
        token: String,
        body: String,
        parse: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }
            try {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
                if (conn.responseCode !in 200..299) {
                    error("AI endpoint unavailable (HTTP ${conn.responseCode})")
                }
                val resp = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                parse(resp)
            } finally {
                conn.disconnect()
            }
        }
    }
}

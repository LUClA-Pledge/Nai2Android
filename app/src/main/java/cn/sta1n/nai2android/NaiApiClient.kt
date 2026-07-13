package cn.sta1n.nai2android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ServiceSettings(
    val serviceName: String,
    val costPerImage: Int,
    val defaultModel: String,
    val defaultNegative: String
)

data class AccountInfo(val balance: Int)

enum class JobStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED,
    UNKNOWN
}

data class JobProgress(val percent: Int = 0)

data class JobResponse(
    val id: String,
    val status: JobStatus,
    val imageUrl: String = "",
    val error: String = "",
    val queuedCount: Int = 0,
    val queuePosition: Int = 0,
    val progress: JobProgress = JobProgress()
)

class NaiApiException(message: String, val statusCode: Int? = null) : Exception(message)

class NaiApiClient(baseUrl: String) {
    private val baseUrl = baseUrl.trim().trimEnd('/').ifEmpty { DEFAULT_BASE_URL }

    suspend fun getSettings(): ServiceSettings = withContext(Dispatchers.IO) {
        requestJson("GET", "/api/settings").let { json ->
            ServiceSettings(
                serviceName = json.optString("serviceName", "Nai2API"),
                costPerImage = json.optInt("costPerImage", 1),
                defaultModel = json.optString("defaultModel", "nai-diffusion-4-5-full"),
                defaultNegative = json.optString("defaultNegative", "")
            )
        }
    }

    suspend fun getMe(token: String): AccountInfo = withContext(Dispatchers.IO) {
        val query = "?token=${encode(token)}"
        requestJson("GET", "/api/me$query").let { json ->
            AccountInfo(balance = json.optInt("balance", 0))
        }
    }

    suspend fun submitJob(payload: JobPayload): JobResponse = withContext(Dispatchers.IO) {
        val json = JSONObject()
            .put("token", payload.token)
            .put("tag", payload.tag)
            .put("model", payload.model)
            .put("artist", payload.artist)
            .put("size", payload.size)
            .put("cost", payload.cost)
            .put("steps", payload.steps)
            .put("scale", payload.scale)
            .put("cfg", payload.cfg)
            .put("sampler", payload.sampler)
            .put("negative", payload.negative)
            .put("nocache", payload.nocache)
            .put("noise_schedule", payload.noiseSchedule)

        parseJob(requestJson("POST", "/api/jobs", json.toString()))
    }

    suspend fun getJob(id: String, token: String): JobResponse = withContext(Dispatchers.IO) {
        val query = "?token=${encode(token)}"
        parseJob(requestJson("GET", "/api/jobs/${encode(id)}$query"))
    }

    suspend fun waitForCompletion(
        initialJob: JobResponse,
        token: String,
        onUpdate: (JobResponse) -> Unit
    ): JobResponse {
        if (initialJob.id.isBlank()) throw NaiApiException("鏈嶅姟鏈繑鍥炴湁鏁堢殑浠诲姟缂栧彿")
        var current = initialJob
        onUpdate(current)
        repeat(MAX_POLL_ATTEMPTS) {
            when (current.status) {
                JobStatus.DONE -> return current
                JobStatus.FAILED -> throw NaiApiException(current.error.ifBlank { "鐢熸垚浠诲姟澶辫触" })
                else -> Unit
            }
            delay(POLL_INTERVAL_MS)
            current = getJob(current.id, token)
            onUpdate(current)
        }
        throw NaiApiException("鐢熸垚浠诲姟绛夊緟瓒呮椂")
    }

    suspend fun downloadImageTo(
        imageUrl: String,
        token: String,
        output: OutputStream
    ): String = withContext(Dispatchers.IO) {
        val connection = openConnection(resolveUrl(imageUrl), "GET")
        connection.setRequestProperty("x-user-token", token)
        connection.setRequestProperty("Accept", "image/*")
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw NaiApiException("鍥剧墖涓嬭浇澶辫触锛圚TTP $status锛?, status)
            }
            BufferedInputStream(connection.inputStream).use { input ->
                BufferedOutputStream(output).use { bufferedOutput ->
                    input.copyTo(bufferedOutput)
                    bufferedOutput.flush()
                }
            }
            connection.contentType.orEmpty()
        } finally {
            connection.disconnect()
        }
    }

    private fun requestJson(method: String, path: String, body: String? = null): JSONObject {
        val connection = openConnection(resolveUrl(path), method)
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(StandardCharsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(responseText).optString("error") }
                    .getOrNull()
                    .orEmpty()
                    .ifBlank { responseText.ifBlank { "鎺ュ彛璇锋眰澶辫触锛圚TTP $status锛? } }
                throw NaiApiException(message, status)
            }
            return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, method: String): HttpURLConnection {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", "Nai2Android/0.1")
        return connection
    }

    private fun resolveUrl(pathOrUrl: String): String {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl
        return "$baseUrl/${pathOrUrl.trimStart('/')}"
    }

    private fun parseJob(json: JSONObject): JobResponse {
        val status = when (json.optString("status").lowercase()) {
            "queued" -> JobStatus.QUEUED
            "running" -> JobStatus.RUNNING
            "done", "completed" -> JobStatus.DONE
            "failed", "error" -> JobStatus.FAILED
            else -> JobStatus.UNKNOWN
        }
        val progressJson = json.optJSONObject("generationProgress")
        return JobResponse(
            id = json.optString("id"),
            status = status,
            imageUrl = json.optString("imageUrl"),
            error = json.optString("error"),
            queuedCount = json.optInt("queuedCount", 0),
            queuePosition = json.optInt("queuePosition", 0),
            progress = JobProgress(progressJson?.optInt("percent", 0) ?: 0)
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 45_000
        const val POLL_INTERVAL_MS = 700L
        const val MAX_POLL_ATTEMPTS = 900
    }
}

const val DEFAULT_BASE_URL = "https://nai.sta1n.cn"


package com.example

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.Response

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GUNDI API Key Rotator - Limitsiz gibi kullan!
 * Manages rotation across 4 different keys from .env and BuildConfig, with automatic retry interceptor.
 */
object GundiApiRotator {
    private const val TAG = "GundiApiRotator"

    var appContext: Context? = null

    // User-provided API key to be used directly
    private val hardcodedFallbackKeys = listOf(
        "910978c5f20b4b55ad3f02d3a8870e3e",
        "AIzaSyDnqFOf6PH8GKilyCUcTuY_u-J41N-B1t4"
    )

    /**
     * Initializes the rotator with the application context to check for user-defined custom keys.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun maskKey(key: String): String {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return "EMPTY"
        return if (trimmed.length <= 4) {
            "..." + trimmed
        } else {
            "..." + trimmed.substring(trimmed.length - 4)
        }
    }

    private fun logRotation(failedKey: String, nextKey: String, reason: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val maskedFailed = maskKey(failedKey)
        val maskedNext = maskKey(nextKey)
        
        val logMessage = "[$timestamp] ROTATION: Failed Key ($maskedFailed) due to [$reason] -> Switched to Key ($maskedNext)"
        
        Log.w(TAG, logMessage)
        
        // Write to local file (api_rotation_log.txt) in internal files directory
        appContext?.let { ctx ->
            try {
                val file = File(ctx.filesDir, "api_rotation_log.txt")
                file.appendText(logMessage + "\n")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write rotation log to file", e)
            }
        }
    }

    val apiKeys: List<String> by lazy {
        val keys = mutableListOf<String>()
        
        // 1. Load keys from BuildConfig using reflection to be fully compile-safe
        try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val fieldsToTry = listOf(
                "GEMINI_API_KEY",
                "GEMINI_API_KEY_1",
                "GEMINI_API_KEY_2",
                "GEMINI_API_KEY_3",
                "GEMINI_API_KEY_4",
                "GEMINI_API_KEY_5"
            )
            for (fieldName in fieldsToTry) {
                try {
                    val field = buildConfigClass.getField(fieldName)
                    val value = field.get(null) as? String
                    if (!value.isNullOrBlank() && 
                        !value.startsWith("YOUR_GEMINI") && 
                        value != "YOUR_KEY_HERE" && 
                        value != "MY_GEMINI_API_KEY" && 
                        value != "GEMINI_API_KEY"
                    ) {
                        keys.add(value)
                    }
                } catch (e: NoSuchFieldException) {
                    // Not found, ignore
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading keys dynamically from BuildConfig", e)
        }
        
        // 2. Fall back to hardcoded keys if no custom keys are configured in build env
        if (keys.isEmpty()) {
            keys.addAll(hardcodedFallbackKeys)
        }
        
        val finalKeys = keys.distinct()
        Log.d(TAG, "Loaded ${finalKeys.size} keys for rotation.")
        finalKeys
    }

    private var currentKeyIndex = 0
    private var failCounts = mutableMapOf<Int, Int>()

    /**
     * Siradaki saglam key'i al (for REST / Retrofit integration)
     */
    fun getActiveApiKey(): String {
        // First check if user configured a custom key in Settings
        appContext?.let { ctx ->
            try {
                val customKey = SettingsManager.getInstance(ctx).customApiKey.value.trim()
                if (customKey.isNotBlank()) {
                    return customKey
                }
            } catch (e: Exception) {
                // SettingsManager might not be initialized yet
            }
        }

        if (apiKeys.isEmpty()) {
            return ""
        }

        val bestIndex = failCounts.entries
            .filter { it.key < apiKeys.size }
            .minByOrNull { it.value }?.key ?: currentKeyIndex
        
        currentKeyIndex = bestIndex % apiKeys.size
        return apiKeys[currentKeyIndex]
    }

    /**
     * Siradaki saglam key'i al ve GenerativeModel dondur
     */
    fun getNextModel(modelName: String = "gemini-1.5-flash"): GenerativeModel {
        val key = getActiveApiKey()
        if (key.isBlank()) {
            throw Exception("Hic API key yok REISIM! .env dosyasina key koy")
        }
        
        Log.d(TAG, "GUNDI key ile baglaniyor...")
        return GenerativeModel(
            modelName = modelName,
            apiKey = key
        )
    }

    /**
     * API hata verirse cagir, bir sonraki key'e gecer ve yeni GenerativeModel dondur
     */
    fun reportFailureAndRotate(error: Exception, modelName: String = "gemini-1.5-flash"): GenerativeModel {
        val failedKey = getActiveApiKey()
        reportFailureAndRotate(failedKey, error.message ?: "Exception: ${error.javaClass.simpleName}")
        return getNextModel(modelName)
    }

    /**
     * API hata verirse cagir (REST / Retrofit destegi)
     */
    fun reportFailureAndRotate() {
        reportFailureAndRotate("", "Unknown Failure")
    }

    /**
     * API hata verirse cagir (REST / Retrofit destegi) with specific details
     */
    fun reportFailureAndRotate(failedKey: String, reason: String) {
        if (apiKeys.isEmpty()) return
        val activeFailedKey = if (failedKey.isNotBlank()) failedKey else getActiveApiKey()
        
        val failedIdx = apiKeys.indexOf(activeFailedKey)
        if (failedIdx != -1) {
            failCounts[failedIdx] = (failCounts[failedIdx] ?: 0) + 1
        } else {
            failCounts[currentKeyIndex] = (failCounts[currentKeyIndex] ?: 0) + 1
        }
        
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
        
        val nextKey = getActiveApiKey()
        logRotation(activeFailedKey, nextKey, reason)
    }

    /**
     * Kullanim - AssistantHub'ta veya baska yerde boyle kullan:
     */
    suspend fun generateWithRetry(prompt: String, maxRetries: Int = 6): String {
        var lastError: Exception? = null
        var model = getNextModel()

        repeat(maxRetries) { attempt ->
            val failedKey = getActiveApiKey()
            try {
                val response = model.generateContent(prompt)
                Log.d(TAG, "GUNDI Basarili! Key ${maskKey(failedKey)} calisti")
                return response.text ?: "Bos cevap geldi"
            } catch (e: Exception) {
                lastError = e
                val msg = e.message?.lowercase() ?: ""
                
                // 404, 429, quota, rate limit gibi hatalarda key degistir
                if (msg.contains("404") || msg.contains("429") || 
                    msg.contains("quota") || msg.contains("rate") ||
                    msg.contains("not found") || msg.contains("resource_exhausted") ||
                    msg.contains("api key") || msg.contains("invalid") || msg.contains("400") || msg.contains("403")) {
                    
                    Log.w(TAG, "GUNDI 404/429/invalid yakalandi, key degistiriliyor... Deneme ${attempt + 1}/$maxRetries")
                    model = reportFailureAndRotate(e)
                    delay(1000L * (attempt + 1)) // Bekle
                } else {
                    throw e // Baska hata ise direkt firlat
                }
            }
        }
        throw lastError ?: Exception("Bilinmeyen hata")
    }

    /**
     * REST / Retrofit calls wrapper with rotation
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        block: suspend (String) -> T
    ): T {
        var lastError: Exception? = null
        
        repeat(maxRetries) { attempt ->
            val currentKey = getActiveApiKey()
            try {
                return block(currentKey)
            } catch (e: Exception) {
                lastError = e
                
                val isKeyOrQuotaError = when {
                    e is retrofit2.HttpException -> {
                        val code = e.code()
                        code == 400 || code == 401 || code == 403 || code == 429
                    }
                    else -> {
                        val msg = e.message?.lowercase() ?: ""
                        msg.contains("400") || msg.contains("401") || msg.contains("403") || msg.contains("429") ||
                        msg.contains("quota") || msg.contains("rate") ||
                        msg.contains("resource_exhausted") || msg.contains("api key") ||
                        msg.contains("invalid")
                    }
                }
                
                if (isKeyOrQuotaError) {
                    Log.w(TAG, "Key failed on attempt ${attempt + 1}: ${e.message}")
                    reportFailureAndRotate(currentKey, e.message ?: "Quota/Key Error")
                    delay(1000L * (attempt + 1))
                } else {
                    throw e
                }
            }
        }
        throw lastError ?: Exception("Rotation failed after $maxRetries attempts")
    }
}

/**
 * Transparent OkHttp Interceptor that dynamically replaces 'key' query param and retries on failure.
 */
class GundiApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val originalUrl = request.url
        val originalKey = originalUrl.queryParameter("key")
        
        if (originalKey != null) {
            val activeKey = GundiApiRotator.getActiveApiKey()
            val newUrl = originalUrl.newBuilder()
                .setQueryParameter("key", activeKey)
                .build()
            
            request = request.newBuilder()
                .url(newUrl)
                .build()
        }
        
        var response = chain.proceed(request)
        var attempt = 1
        val maxRetries = 4
        
        while ((response.code == 400 || response.code == 401 || response.code == 403 || response.code == 429) && attempt < maxRetries) {
            val failedKey = request.url.queryParameter("key") ?: ""
            Log.w("GundiApiKeyInterceptor", "Request failed with code ${response.code}. Rotating key and retrying...")
            response.close()
            
            GundiApiRotator.reportFailureAndRotate(failedKey, "HTTP ${response.code}")
            val nextKey = GundiApiRotator.getActiveApiKey()
            val retriedUrl = request.url.newBuilder()
                .setQueryParameter("key", nextKey)
                .build()
                
            request = request.newBuilder()
                .url(retriedUrl)
                .build()
                
            response = chain.proceed(request)
            attempt++
        }
        
        return response
    }
}

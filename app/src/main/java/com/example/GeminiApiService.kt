package com.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    val googleSearch: GoogleSearch? = null
)

@JsonClass(generateAdapter = true)
data class GoogleSearch(
    val dummy: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val groundingChunks: List<GroundingChunk>? = null,
    val searchEntryPoint: SearchEntryPoint? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    val web: WebSource? = null
)

@JsonClass(generateAdapter = true)
data class WebSource(
    val uri: String? = null,
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchEntryPoint(
    val renderedContent: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    val aspectRatio: String? = null,
    val imageSize: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    val voiceConfig: VoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    val voiceName: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val groundingMetadata: GroundingMetadata? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentDynamic(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateImage(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/imagen-3.0-generate-002:generateImages")
    suspend fun generateImagen3(
        @Query("key") apiKey: String,
        @Body request: ImagenRequest
    ): ImagenResponse
}

@JsonClass(generateAdapter = true)
data class ImagenRequest(
    val prompt: String,
    val numberOfImages: Int? = 1,
    val aspectRatio: String? = "1:1",
    val outputMimeType: String? = "image/jpeg"
)

@JsonClass(generateAdapter = true)
data class ImagenResponse(
    val generatedImages: List<GeneratedImage>? = null
)

@JsonClass(generateAdapter = true)
data class GeneratedImage(
    val image: ImageBytes? = null
)

@JsonClass(generateAdapter = true)
data class ImageBytes(
    val imageBytes: String? = null
)

class GundiProxyInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val context = GundiApiRotator.appContext
        
        if (context == null) return chain.proceed(originalRequest)
        
        val settingsManager = SettingsManager.getInstance(context)
        if (!settingsManager.isProxyEnabled.value) return chain.proceed(originalRequest)

        // Try primary
        val primaryProxyUrl = settingsManager.proxyUrl.value.trim()
        if (primaryProxyUrl.isNotBlank()) {
            val primaryRequest = rebuildRequest(originalRequest, primaryProxyUrl, settingsManager)
            if (primaryRequest != null) {
                try {
                    val response = chain.proceed(primaryRequest)
                    if (response.isSuccessful) return response
                    response.close()
                } catch (e: Exception) {
                    android.util.Log.e("GundiProxyInterceptor", "Primary proxy failed: ${e.message}")
                }
            }
        }
        
        // Try secondary
        val secondaryProxyUrl = settingsManager.secondaryProxyUrl.value.trim()
        if (secondaryProxyUrl.isNotBlank()) {
            val secondaryRequest = rebuildRequest(originalRequest, secondaryProxyUrl, settingsManager)
            if (secondaryRequest != null) {
                try {
                    return chain.proceed(secondaryRequest)
                } catch (e: Exception) {
                    android.util.Log.e("GundiProxyInterceptor", "Secondary proxy failed: ${e.message}")
                }
            }
        }
        
        return chain.proceed(originalRequest)
    }

    private fun rebuildRequest(request: okhttp3.Request, proxyUrl: String, settingsManager: SettingsManager): okhttp3.Request? {
        val originalUrl = request.url
        val stripKey = settingsManager.isSecureKeyStripEnabled.value
        
        var sanitizedUrl = proxyUrl
        // Handle typos or special prefixes like ftp:) or ftp://
        if (sanitizedUrl.contains("ftp:)")) {
            sanitizedUrl = sanitizedUrl.replace("ftp:)", "")
        }
        if (sanitizedUrl.contains("ftp://")) {
            sanitizedUrl = sanitizedUrl.replace("ftp://", "")
        }
        sanitizedUrl = sanitizedUrl.trim()
        if (!sanitizedUrl.startsWith("http://") && !sanitizedUrl.startsWith("https://")) {
            sanitizedUrl = "http://$sanitizedUrl"
        }

        val proxyHttpUrl = sanitizedUrl.toHttpUrlOrNull() ?: return null
        
        val originalPathSegments = originalUrl.pathSegments
        val proxyPathSegments = proxyHttpUrl.pathSegments.filter { it.isNotEmpty() }
        
        val urlBuilder = originalUrl.newBuilder()
            .scheme(proxyHttpUrl.scheme)
            .host(proxyHttpUrl.host)
            .port(proxyHttpUrl.port)
            
        // Rebuild path segments to merge correctly
        urlBuilder.encodedPath("")
        for (segment in proxyPathSegments) {
            urlBuilder.addPathSegment(segment)
        }
        for (segment in originalPathSegments) {
            // Skip segments from api.gundibro.ai base url
            if (segment == "v1" || segment == "connect") {
                continue
            }
            // Avoid repeating path segments if the proxy already includes them (e.g., v1beta)
            if (!proxyPathSegments.contains(segment)) {
                urlBuilder.addPathSegment(segment)
            }
        }
        
        // Handle query parameters: strip key if required
        if (stripKey) {
            urlBuilder.removeAllQueryParameters("key")
        }
        
        val targetUrl = urlBuilder.build()
        
        val requestBuilder = request.newBuilder().url(targetUrl)
        
        // Handle authorization header if provided
        val authToken = settingsManager.proxyAuthToken.value.trim()
        if (authToken.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        } else {
            val username = settingsManager.proxyUsername.value.trim()
            val password = settingsManager.proxyPassword.value.trim()
            if (username.isNotBlank() || password.isNotBlank()) {
                val credentials = okhttp3.Credentials.basic(username, password)
                requestBuilder.header("Authorization", credentials)
            }
        }
        
        return requestBuilder.build()
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://api.gundibro.ai/v1/connect/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(GundiProxyInterceptor())
        .addInterceptor(GundiApiKeyInterceptor())
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

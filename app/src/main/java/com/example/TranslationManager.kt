package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationManager(private val apiKeyProvider: () -> String) {

    suspend fun translateAndMaintainTone(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return@withContext text
        }

        val prompt = """
            Translate the following message into ${targetLanguage}.
            Maintain the friendly, warm, and professional tone of an AI assistant named 'GUNDİ Bro'.
            
            Message: "${text}"
            
            Translate it directly.
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: text
        } catch (e: Exception) {
            e.printStackTrace()
            text // Return original text on error
        }
    }
}

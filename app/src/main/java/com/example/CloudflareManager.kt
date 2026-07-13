package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

data class CloudflareRecord(
    val id: String,
    val type: String,
    val name: String,
    val content: String,
    val ttl: Int,
    val proxied: Boolean
)

object CloudflareManager {
    private val client = OkHttpClient()

    suspend fun fetchDnsRecords(token: String, zoneId: String): List<CloudflareRecord> = withContext(Dispatchers.IO) {
        if (token.isBlank() || zoneId.isBlank() || token.startsWith("MY_") || zoneId.startsWith("CLOUDFLARE_")) {
            return@withContext emptyList()
        }

        val request = Request.Builder()
            .url("https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyString)
                val success = json.optBoolean("success", false)
                if (!success) return@withContext emptyList()

                val resultArr = json.optJSONArray("result") ?: return@withContext emptyList()
                val list = mutableListOf<CloudflareRecord>()
                for (i in 0 until resultArr.length()) {
                    val obj = resultArr.getJSONObject(i)
                    list.add(
                        CloudflareRecord(
                            id = obj.optString("id", ""),
                            type = obj.optString("type", ""),
                            name = obj.optString("name", ""),
                            content = obj.optString("content", ""),
                            ttl = obj.optInt("ttl", 1),
                            proxied = obj.optBoolean("proxied", false)
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createDnsRecord(
        token: String,
        zoneId: String,
        type: String,
        name: String,
        content: String,
        ttl: Int,
        proxied: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank() || zoneId.isBlank()) return@withContext false

        val jsonBody = JSONObject().apply {
            put("type", type)
            put("name", name)
            put("content", content)
            put("ttl", ttl)
            // Proxied is only applicable to A, AAAA, CNAME
            if (type == "A" || type == "AAAA" || type == "CNAME") {
                put("proxied", proxied)
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records")
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    json.optBoolean("success", false)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteDnsRecord(token: String, zoneId: String, recordId: String): Boolean = withContext(Dispatchers.IO) {
        if (token.isBlank() || zoneId.isBlank() || recordId.isBlank()) return@withContext false

        val request = Request.Builder()
            .url("https://api.cloudflare.com/client/v4/zones/$zoneId/dns_records/$recordId")
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    json.optBoolean("success", false)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

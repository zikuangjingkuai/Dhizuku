package com.rosan.dhizuku.cloud

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CloudApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun hmacSha256(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    data class Command(val action: String, val packageName: String)

    suspend fun registerDevice(deviceId: String): Boolean {
        val url = CloudConfig.getServerUrl()
        val secret = CloudConfig.getSecret()
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("platform", "android")
            put("model", android.os.Build.MODEL)
        }
        val body = json.toString()
        val signature = hmacSha256(body, secret)
        val request = Request.Builder()
            .url("$url/api/device/register")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("X-Signature", signature)
            .build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("CloudApi", "Register failed", e)
                false
            }
        }
    }

    suspend fun report(deviceId: String, info: JSONObject): Boolean {
        val url = CloudConfig.getServerUrl()
        val secret = CloudConfig.getSecret()
        val bodyJson = info.put("deviceId", deviceId).toString()
        val signature = hmacSha256(bodyJson, secret)
        val request = Request.Builder()
            .url("$url/api/device/report")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .header("X-Signature", signature)
            .build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("CloudApi", "report failed", e)
                false
            }
        }
    }

    suspend fun fetchCommand(deviceId: String): Command? {
        val url = CloudConfig.getServerUrl()
        val secret = CloudConfig.getSecret()
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signData = "$deviceId$timestamp"
        val signature = hmacSha256(signData, secret)
        val requestUrl = "$url/api/device/command?deviceId=$deviceId&timestamp=$timestamp&sign=$signature"
        val request = Request.Builder().url(requestUrl).get().build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.has("command") && json.has("package")) {
                    Command(json.getString("command"), json.getString("package"))
                } else null
            } catch (e: Exception) {
                Log.e("CloudApi", "fetch command failed", e)
                null
            }
        }
    }

    suspend fun postResult(deviceId: String, action: String, pkg: String, output: String): Boolean {
        val url = CloudConfig.getServerUrl()
        val secret = CloudConfig.getSecret()
        val bodyJson = JSONObject().apply {
            put("deviceId", deviceId)
            put("action", action)
            put("package", pkg)
            put("output", output)
        }.toString()
        val signature = hmacSha256(bodyJson, secret)
        val request = Request.Builder()
            .url("$url/api/device/result")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .header("X-Signature", signature)
            .build()
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("CloudApi", "postResult failed", e)
                false
            }
        }
    }
}

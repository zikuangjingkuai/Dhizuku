package com.rosan.dhizuku.cloud

import android.content.Context
import android.content.SharedPreferences

object CloudConfig {
    // ======== 修改这里为你的服务器信息 ========
    private const val SERVER_URL = "https://your-server.com"  // 你的服务器地址
    private const val SECRET = "your-secret-key"              // 你的通信密钥
    // =======================================

    private const val PREFS_NAME = "cloud_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getServerUrl(): String = SERVER_URL
    fun getSecret(): String = SECRET

    fun getDeviceId(context: Context): String {
        var id = prefs(context).getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs(context).edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}

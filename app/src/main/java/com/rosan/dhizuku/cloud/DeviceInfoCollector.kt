package com.rosan.dhizuku.cloud

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import org.json.JSONObject

object DeviceInfoCollector {

    fun collect(context: Context): JSONObject {
        val json = JSONObject()
        json.put("battery", getBatteryLevel(context))
        json.put("wifi_ssid", getWifiSSID(context))
        json.put("online", isOnline(context))
        return json
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (scale > 0) (level * 100 / scale) else -1
    }

    private fun getWifiSSID(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo: WifiInfo = wifiManager.connectionInfo
            val ssid = connectionInfo.ssid
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid.substring(1, ssid.length - 1)
            } else ssid
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

package com.rosan.dhizuku.cloud

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.*
import com.rosan.dhizuku.DhizukuAdminReceiver
import java.util.concurrent.TimeUnit

class CloudCommandWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val deviceId = CloudConfig.getDeviceId(ctx)

        // 1. 上报设备信息
        val info = DeviceInfoCollector.collect(ctx)
        CloudApiService.report(deviceId, info)

        // 2. 拉取指令
        val command = CloudApiService.fetchCommand(deviceId)
        if (command != null) {
            Log.d("CloudWorker", "Received: ${command.action} -> ${command.packageName}")
            val output = executeCommand(ctx, command)
            CloudApiService.postResult(deviceId, command.action, command.packageName, output)
        }
        return Result.success()
    }

    private fun executeCommand(context: Context, command: CloudApiService.Command): String {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DhizukuAdminReceiver::class.java)

        if (!dpm.isAdminActive(admin)) {
            return "Device owner not active"
        }
        val pkg = command.packageName
        return try {
            when (command.action) {
                "freeze", "hide" -> {
                    dpm.setApplicationHidden(admin, pkg, true)
                    "success"
                }
                "unfreeze", "unhide" -> {
                    dpm.setApplicationHidden(admin, pkg, false)
                    "success"
                }
                "uninstall" -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DELETE)
                    intent.data = android.net.Uri.parse("package:$pkg")
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    "uninstall intent sent"
                }
                else -> "unknown action"
            }
        } catch (e: Exception) {
            Log.e("CloudWorker", "Execute failed", e)
            "error: ${e.message}"
        }
    }

    companion object {
        private const val WORK_NAME = "cloud_report_and_command"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CloudCommandWorker>(3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
    }
}

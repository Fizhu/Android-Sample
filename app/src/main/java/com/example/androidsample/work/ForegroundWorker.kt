package com.example.androidsample.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Created by fizhu on 24 September 2021
 * https://github.com/Fizhu
 */
class ForegroundWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo())
        return@withContext runCatching {
            runTask()
            Result.success()
        }.getOrElse {
            Result.failure()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(id: String, channelName: String) {
        notificationManager.createNotificationChannel(
            NotificationChannel(id, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = "1225"
        val channelName = "Assessment"
        val title = "Assessment"
        val cancel = "Cancel"
        val body = "Submitting your previous assessment"

        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(id, channelName)
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setProgress(100, 0, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(1, notification)
    }

    private suspend fun runTask() {
        delay(10000)
    }

}
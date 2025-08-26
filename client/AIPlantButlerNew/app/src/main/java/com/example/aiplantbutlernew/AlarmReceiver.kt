package com.example.aiplantbutlernew

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskDescription = intent.getStringExtra("TASK_DESCRIPTION") ?: "할 일이 있습니다!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "plant_alarm_channel"

        // 안드로이드 Oreo (API 26) 이상에서는 알림 채널이 필요합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Plant Care Alarms",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘
            .setContentTitle("식물 관리 알림 🌿")
            .setContentText(taskDescription)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 알림 표시 (알림 ID는 현재 시간으로 하여 겹치지 않게 함)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
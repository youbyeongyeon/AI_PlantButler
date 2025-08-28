package com.example.aiplantbutlernew

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            // 저장된 식물 목록을 불러옵니다.
            val sharedPref = context.getSharedPreferences("my_plants", Context.MODE_PRIVATE)
            val json = sharedPref.getString("plant_list", null)
            if (json != null) {
                val type = object : TypeToken<MutableList<Plant>>() {}.type
                val plantList: MutableList<Plant> = Gson().fromJson(json, type)

                // 모든 식물의 모든 할 일을 확인합니다.
                plantList.forEachIndexed { plantIndex, plant ->
                    plant.tasks.forEachIndexed { taskIndex, task ->
                        // 활성화(isDone=true)되어 있고, 알람 시간이 미래인 경우에만 알람을 다시 예약합니다.
                        if (task.isDone && task.alarmTime != null && task.alarmTime!! > System.currentTimeMillis()) {
                            // 각 알람을 고유하게 식별하기 위해 requestCode를 복잡하게 만듭니다.
                            val requestCode = plantIndex * 1000 + taskIndex
                            scheduleAlarm(context, task, requestCode)
                        }
                    }
                }
            }
        }
    }

    // 알람을 예약하는 헬퍼 함수
    private fun scheduleAlarm(context: Context, task: Task, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_DESCRIPTION", task.description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        task.alarmTime?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, it, pendingIntent)
            }
        }
    }
}
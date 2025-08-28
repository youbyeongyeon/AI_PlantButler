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
            val sharedPref = context.getSharedPreferences("my_plants", Context.MODE_PRIVATE)
            val json = sharedPref.getString("plant_list", null)
            if (json != null) {
                val type = object : TypeToken<MutableList<Plant>>() {}.type
                val plantList: MutableList<Plant> = Gson().fromJson(json, type)

                plantList.forEachIndexed { plantIndex, plant ->
                    plant.tasks.forEachIndexed { taskIndex, task ->
                        if (task.isDone && task.alarmTime != null && task.alarmTime!! > System.currentTimeMillis()) {
                            val requestCode = plantIndex * 1000 + taskIndex
                            scheduleAlarm(context, task, requestCode)
                        }
                    }
                }
            }
        }
    }

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
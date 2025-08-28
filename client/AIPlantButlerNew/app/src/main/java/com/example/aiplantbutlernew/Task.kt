package com.example.aiplantbutlernew

data class Task(
    val description: String,
    var isDone: Boolean = false,
    var alarmTime: Long? = null
)
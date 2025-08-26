package com.example.aiplantbutlernew

import android.net.Uri

// 메시지 종류를 구분하기 위한 상수
const val VIEW_TYPE_USER_TEXT = 1
const val VIEW_TYPE_BOT_TEXT = 2
const val VIEW_TYPE_USER_IMAGE = 3

data class Message(
    val text: String?,
    val imageUri: Uri?,
    val viewType: Int
)
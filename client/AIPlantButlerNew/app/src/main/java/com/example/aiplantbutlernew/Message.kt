package com.example.aiplantbutlernew

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val VIEW_TYPE_USER_TEXT = 1
const val VIEW_TYPE_BOT_TEXT = 2
const val VIEW_TYPE_USER_IMAGE = 3

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ChatRoom::class,
        parentColumns = ["id"],
        childColumns = ["roomId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["roomId"])]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long, // 어떤 채팅방에 속한 메시지인지
    val text: String?,
    val imageUriString: String?, // Uri는 String으로 변환하여 저장
    val viewType: Int,
    val timestamp: Long = System.currentTimeMillis()
)
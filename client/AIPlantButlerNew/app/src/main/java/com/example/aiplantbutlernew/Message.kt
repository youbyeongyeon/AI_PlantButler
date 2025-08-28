package com.example.aiplantbutlernew

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val VIEW_TYPE_USER_TEXT = 0
const val VIEW_TYPE_USER_IMAGE = 1
const val VIEW_TYPE_BOT_TEXT  = 2
const val VIEW_TYPE_BOT_IMAGE = 3

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
    val roomId: Long,
    val text: String?,
    val imageUriString: String?,
    val viewType: Int,
    val timestamp: Long = System.currentTimeMillis()
)
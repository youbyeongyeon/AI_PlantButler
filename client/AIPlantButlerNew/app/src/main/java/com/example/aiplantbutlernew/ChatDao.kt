package com.example.aiplantbutlernew

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertChatRoom(chatRoom: ChatRoom): Long

    @Insert
    suspend fun insertMessage(message: Message)

    @Query("SELECT * FROM chat_rooms ORDER BY timestamp DESC")
    fun getAllChatRooms(): Flow<List<ChatRoom>>

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: Long): Flow<List<Message>>

    // --- 이 두 함수가 누락되었습니다 ---
    @Update
    suspend fun updateChatRoom(chatRoom: ChatRoom)

    @Delete
    suspend fun deleteChatRoom(chatRoom: ChatRoom)
}
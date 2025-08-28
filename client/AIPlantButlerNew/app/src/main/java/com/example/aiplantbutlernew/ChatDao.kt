package com.example.aiplantbutlernew

import androidx.room.*
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
    @Update
    suspend fun updateChatRoom(chatRoom: ChatRoom)
    @Delete
    suspend fun deleteChatRoom(chatRoom: ChatRoom)
}
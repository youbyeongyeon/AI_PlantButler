package com.example.aiplantbutlernew.network

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Body
import retrofit2.Response

data class ChatRequest(val message: String)
data class ChatResponse(val reply: String)

interface ApiService {
    @POST("/chat/text")
    suspend fun sendText(@Body request: ChatRequest): Response<ChatResponse>

    @Multipart
    @POST("/chat/image")
    suspend fun sendImage(@Part image: MultipartBody.Part): Response<ChatResponse>
}

// app/src/main/java/com/example/aiplantbutlernew/ApiModels.kt
package com.example.aiplantbutlernew

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// 요청/응답 모델
data class ChatRequest(val text: String)
data class ChatResponse(val reply: String)

data class ImageAnalysisResponse(val result: ImageResult?)
data class ImageResult(val label: String)

// Retrofit 인터페이스
interface ApiService {
    @POST("chat/text")
    suspend fun chat(@Body req: ChatRequest): ChatResponse

    @Multipart
    @POST("chat/image")
    suspend fun analyzeImage(@Part image: MultipartBody.Part): ImageAnalysisResponse
}

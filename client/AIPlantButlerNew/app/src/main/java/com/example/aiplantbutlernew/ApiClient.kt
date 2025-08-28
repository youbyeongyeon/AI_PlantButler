// app/src/main/java/com/example/aiplantbutlernew/ApiClient.kt
package com.example.aiplantbutlernew

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // TODO: 실제 서버 베이스 URL로 교체 (끝에 / 필수)
    private const val BASE_URL = "http://YOUR_SERVER_HOST:PORT/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}

package com.example.aiplantbutlernew

// 이 파일이 누락되어 오류가 발생했습니다.

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val name: String
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double
)
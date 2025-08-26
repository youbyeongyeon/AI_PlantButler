package com.example.aiplantbutlernew

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// OpenWeatherMap API 응답을 위한 데이터 클래스들
data class WeatherResponse(val weather: List<Weather>, val main: Main, val name: String)
data class Weather(val id: Int, val main: String, val description: String, val icon: String)
data class Main(val temp: Double, val humidity: Int)

class HomeFragment : Fragment() {

    private lateinit var textViewTemp: TextView
    private lateinit var textViewWeatherDesc: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewPlantComment: TextView

    // 권한 요청 런처
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                fetchLocationAndWeather()
            } else {
                // 권한 거부 시 처리
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewTemp = view.findViewById(R.id.text_view_temp)
        textViewWeatherDesc = view.findViewById(R.id.text_view_weather_desc)
        textViewLocation = view.findViewById(R.id.text_view_location)
        textViewPlantComment = view.findViewById(R.id.text_view_plant_comment)

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchLocationAndWeather()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndWeather() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                // 코루틴을 사용해 네트워크 작업 수행
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // !!! 중요: "YOUR_API_KEY" 부분에 본인의 OpenWeatherMap API 키를 넣으세요.
                        val apiKey = "388dcec3097a775ed8a28ff805e223fd"
                        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=kr"
                        val json = URL(url).readText()
                        val weatherResponse = Gson().fromJson(json, WeatherResponse::class.java)

                        // UI 업데이트는 메인 스레드에서
                        withContext(Dispatchers.Main) {
                            updateWeatherUI(weatherResponse)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun updateWeatherUI(weather: WeatherResponse) {
        textViewTemp.text = "${weather.main.temp.roundToInt()}°C"
        textViewWeatherDesc.text = weather.weather.firstOrNull()?.description ?: "N/A"
        textViewLocation.text = weather.name
        textViewPlantComment.text = generatePlantComment(weather)
    }

    // 날씨 데이터에 따라 식물 관리 코멘트를 생성하는 함수
    private fun generatePlantComment(weather: WeatherResponse): String {
        val temp = weather.main.temp
        val weatherId = weather.weather.firstOrNull()?.id ?: 0

        return when {
            weatherId in 200..599 -> "🌧️ 비가 오네요. 실외 식물은 잠시 안으로 옮겨주는 게 좋겠어요."
            temp > 30 -> "🥵 날씨가 매우 덥습니다. 잎이 타지 않도록 직사광선을 피해주세요."
            temp < 5 -> "🥶 날씨가 추워요! 냉해를 입지 않도록 식물들을 실내로 옮겨주세요."
            weatherId in 801..804 -> "☁️ 오늘은 구름이 많네요. 물주기는 흙 상태를 보고 조절해주세요."
            else -> "☀️ 날씨가 맑아 식물들이 좋아해요!"
        }
    }
}
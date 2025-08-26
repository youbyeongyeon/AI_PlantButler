package com.example.aiplantbutlernew

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.bumptech.glide.Glide

// --- 데이터 클래스 정의 ---
// 사용자가 등록한 식물 정보
data class Plant(val name: String, val imageUriString: String)

// OpenWeatherMap API 응답을 위한 데이터 클래스들
data class WeatherResponse(val weather: List<Weather>, val main: Main, val name: String)
data class Weather(val id: Int, val main: String, val description: String, val icon: String)
data class Main(val temp: Double)

// --- RecyclerView 어댑터 정의 ---
class PlantAdapter(
    private val plantList: List<Plant>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    // HomeFragment로 클릭 이벤트를 전달하기 위한 인터페이스
    interface OnItemClickListener {
        fun onDeleteClick(position: Int)
    }

    // 각 아이템 뷰의 UI 요소를 담는 ViewHolder
    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantPhoto: ImageView = itemView.findViewById(R.id.image_view_plant_photo)
        val plantName: TextView = itemView.findViewById(R.id.text_view_plant_name)
        val manageButton: ImageButton = itemView.findViewById(R.id.button_manage_plant)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant_task, parent, false)
        return PlantViewHolder(view)
    }



    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plantList[position]
        holder.plantName.text = plant.name

        val uri = Uri.parse(plant.imageUriString)

        Glide.with(holder.itemView.context)
            .load(uri)
            .into(holder.plantPhoto)

        holder.manageButton.setOnClickListener {
            listener.onDeleteClick(position)
        }
    }


    override fun getItemCount() = plantList.size
}


class HomeFragment : Fragment(), PlantAdapter.OnItemClickListener {

    // --- 멤버 변수 선언 ---
    private val plantList = mutableListOf<Plant>()
    private lateinit var plantAdapter: PlantAdapter

    // 날씨 UI 관련
    private lateinit var textViewTemp: TextView
    private lateinit var textViewWeatherDesc: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewPlantComment: TextView

    // AddPlantActivity에서 결과를 받아오면 실행될 런처
    private val addPlantLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val plantName = result.data?.getStringExtra("plantName")
            val plantImageUri = result.data?.getStringExtra("plantImageUri")
            if (plantName != null && plantImageUri != null) {
                plantList.add(Plant(plantName, plantImageUri))
                plantAdapter.notifyItemInserted(plantList.size - 1)
                savePlants() // 변경된 목록을 저장
            }
        }
    }

    // 위치 권한 요청 런처
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            fetchLocationAndWeather()
        } else {
            textViewPlantComment.text = "위치 권한을 허용해야 날씨 정보를 볼 수 있어요."
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- UI 요소 연결 ---
        textViewTemp = view.findViewById(R.id.text_view_temp)
        textViewWeatherDesc = view.findViewById(R.id.text_view_weather_desc)
        textViewLocation = view.findViewById(R.id.text_view_location)
        textViewPlantComment = view.findViewById(R.id.text_view_plant_comment)
        val fabAddPlant: FloatingActionButton = view.findViewById(R.id.fab_add_plant)
        val recyclerViewPlants: RecyclerView = view.findViewById(R.id.recycler_view_plant_tasks) // ID 이름 수정 완료

        // --- 식물 목록 기능 초기화 ---
        loadPlants()
        plantAdapter = PlantAdapter(plantList, this)
        recyclerViewPlants.adapter = plantAdapter
        recyclerViewPlants.layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(
            recyclerViewPlants.context,
            (recyclerViewPlants.layoutManager as LinearLayoutManager).orientation
        )
        recyclerViewPlants.addItemDecoration(dividerItemDecoration)

        fabAddPlant.setOnClickListener {
            val intent = Intent(context, AddPlantActivity::class.java)
            addPlantLauncher.launch(intent)
        }

        // --- 날씨 기능 초기화 ---
        checkLocationPermission()
    }
    // --- PlantAdapter.OnItemClickListener 인터페이스 구현 ---
    override fun onDeleteClick(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("식물 삭제")
            .setMessage("'${plantList[position].name}' 식물을 목록에서 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                plantList.removeAt(position)
                plantAdapter.notifyItemRemoved(position)
                savePlants()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // --- 식물 목록 저장/불러오기 함수 ---
    private fun savePlants() {
        val sharedPref = activity?.getSharedPreferences("my_plants", Context.MODE_PRIVATE) ?: return
        val editor = sharedPref.edit()
        val json = Gson().toJson(plantList)
        editor.putString("plant_list", json)
        editor.apply()
    }

    private fun loadPlants() {
        val sharedPref = activity?.getSharedPreferences("my_plants", Context.MODE_PRIVATE) ?: return
        val json = sharedPref.getString("plant_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Plant>>() {}.type
            val loadedPlants: MutableList<Plant> = Gson().fromJson(json, type)
            plantList.clear()
            plantList.addAll(loadedPlants)
        }
    }

    // --- 날씨 관련 함수 ---
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
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
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val apiKey = "388dcec3097a775ed8a28ff805e223fd" // <--- 이 부분을 본인의 API 키로 꼭 교체하세요!
                        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=kr"
                        val json = URL(url).readText()
                        val weatherResponse = Gson().fromJson(json, WeatherResponse::class.java)
                        withContext(Dispatchers.Main) {
                            updateWeatherUI(weatherResponse)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            textViewPlantComment.text = "날씨 정보를 가져오는 데 실패했습니다."
                        }
                    }
                }
            } else {
                // 위치를 찾지 못했을 때의 처리 추가
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main){
                    textViewPlantComment.text = "위치 정보를 찾을 수 없습니다. GPS를 켜주세요."
                }
            }
        }
    }

    private fun updateWeatherUI(weather: WeatherResponse) {
        textViewTemp.text = "${weather.main.temp.toInt()}°C"
        textViewWeatherDesc.text = weather.weather.firstOrNull()?.description ?: "정보 없음"
        textViewLocation.text = weather.name
        textViewPlantComment.text = generatePlantComment(weather)
    }

    private fun generatePlantComment(weather: WeatherResponse): String {
        val temp = weather.main.temp
        val weatherId = weather.weather.firstOrNull()?.id ?: 0
        return when {
            weatherId in 200..599 -> "🌧️ 비가 오네요. 실외 식물은 잠시 안으로 옮겨주는 게 좋겠어요."
            temp > 30 -> "🥵 날씨가 매우 덥습니다. 잎이 타지 않도록 직사광선을 피해주세요."
            temp < 5 -> "🥶 날씨가 추워요! 냉해를 입지 않도록 식물들을 실내로 옮겨주세요."
            else -> "☀️ 날씨가 맑아 식물들이 좋아해요!"
        }
    }
}
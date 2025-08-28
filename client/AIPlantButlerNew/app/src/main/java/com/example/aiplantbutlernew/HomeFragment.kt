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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

// --- RecyclerView 어댑터 정의 ---
class PlantAdapter(
    private val plantList: List<Plant>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantPhoto: ImageView = itemView.findViewById(R.id.image_view_plant_photo)
        val plantName: TextView = itemView.findViewById(R.id.text_view_plant_name)
        val manageButton: ImageButton = itemView.findViewById(R.id.button_manage_plant)
        val taskSummary: TextView = itemView.findViewById(R.id.text_view_task_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant_task, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plantList[position]
        holder.plantName.text = plant.name
        holder.plantPhoto.setImageURI(Uri.parse(plant.imageUriString))

        val nextCheckedTask = plant.tasks.firstOrNull { it.isDone }
        if (nextCheckedTask != null) {
            holder.taskSummary.text = "할 일: ${nextCheckedTask.description}"
            holder.taskSummary.visibility = View.VISIBLE
        } else {
            holder.taskSummary.text = "활성화된 할 일이 없습니다."
            holder.taskSummary.visibility = View.VISIBLE
        }

        holder.manageButton.setOnClickListener { listener.onDeleteClick(position) }
        holder.itemView.setOnClickListener { listener.onItemClick(position) }
    }

    override fun getItemCount() = plantList.size
}


class HomeFragment : Fragment(), PlantAdapter.OnItemClickListener {

    private val plantList = mutableListOf<Plant>()
    private lateinit var plantAdapter: PlantAdapter

    private lateinit var textViewTemp: TextView
    private lateinit var textViewWeatherDesc: TextView
    private lateinit var textViewLocation: TextView
    private lateinit var textViewPlantComment: TextView

    private val addPlantLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val plantName = result.data?.getStringExtra("plantName")
            val plantImageUri = result.data?.getStringExtra("plantImageUri")
            if (plantName != null && plantImageUri != null) {
                val newPlant = Plant(plantName, plantImageUri, mutableListOf(Task("물주기", true), Task("분갈이 확인")))
                plantList.add(newPlant)
                plantAdapter.notifyItemInserted(plantList.size - 1)
                savePlants()
            }
        }
    }

    private val plantDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val position = result.data?.getIntExtra("plantPosition", -1)
            val plantJson = result.data?.getStringExtra("plantJson")
            if (position != null && position != -1 && plantJson != null) {
                plantList[position] = Gson().fromJson(plantJson, Plant::class.java)
                plantAdapter.notifyItemChanged(position)
                savePlants()
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) { fetchLocationAndWeather() } else { textViewPlantComment.text = "위치 권한을 허용해야 날씨 정보를 볼 수 있어요." }
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
        val fabAddPlant: FloatingActionButton = view.findViewById(R.id.fab_add_plant)
        val recyclerViewPlants: RecyclerView = view.findViewById(R.id.recycler_view_plant_tasks)

        loadPlants()
        plantAdapter = PlantAdapter(plantList, this)
        recyclerViewPlants.adapter = plantAdapter
        val layoutManager = LinearLayoutManager(context)
        recyclerViewPlants.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(recyclerViewPlants.context, layoutManager.orientation)
        recyclerViewPlants.addItemDecoration(dividerItemDecoration)

        fabAddPlant.setOnClickListener {
            val intent = Intent(context, AddPlantActivity::class.java)
            addPlantLauncher.launch(intent)
        }

        checkLocationPermission()
    }

    override fun onItemClick(position: Int) {
        val intent = Intent(context, PlantDetailActivity::class.java).apply {
            putExtra("plantPosition", position)
            putExtra("plantJson", Gson().toJson(plantList[position]))
        }
        plantDetailLauncher.launch(intent)
    }

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

    private fun savePlants() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPref = EncryptedSharedPreferences.create(
                "secret_my_plants",
                masterKeyAlias,
                requireContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val editor = sharedPref.edit()
            val json = Gson().toJson(plantList)
            editor.putString("plant_list", json)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPlants() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPref = EncryptedSharedPreferences.create(
                "secret_my_plants",
                masterKeyAlias,
                requireContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val json = sharedPref.getString("plant_list", null)
            if (json != null) {
                val type = object : TypeToken<MutableList<Plant>>() {}.type
                val loadedPlants: MutableList<Plant> = Gson().fromJson(json, type)
                plantList.clear()
                plantList.addAll(loadedPlants)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            plantList.clear()
            savePlants()
        }
    }

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
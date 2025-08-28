// app/src/main/java/com/example/aiplantbutlernew/ChatFragment.kt
package com.example.aiplantbutlernew

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream

class ChatFragment : Fragment() {

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatDao: ChatDao
    private var roomId: Long = -1
    private var roomTitle: String = "새 채팅"

    private lateinit var cameraImageUri: Uri
    private var diseaseList: List<DiseaseInfo> = emptyList()

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) launchCamera() else
                Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) sendMessage(null, cameraImageUri)
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sendMessage(null, it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            roomId = it.getLong("ROOM_ID")
            roomTitle = it.getString("ROOM_TITLE", "새 채팅")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatDao = AppDatabase.getDatabase(requireContext()).chatDao()
        loadDiseaseJsonOnce()

        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar_chat)
        recyclerView = view.findViewById(R.id.recycler_view_messages)
        val editTextMessage: EditText = view.findViewById(R.id.edit_text_message)
        val buttonSend: ImageButton = view.findViewById(R.id.button_send)
        val buttonAddPhoto: ImageButton = view.findViewById(R.id.button_add_photo)

        toolbar.title = roomTitle
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.adapter = chatAdapter
        val layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        recyclerView.layoutManager = layoutManager

        viewLifecycleOwner.lifecycleScope.launch {
            chatDao.getMessagesForRoom(roomId).collect { messages ->
                chatAdapter.updateMessages(messages)
                if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
            }
        }

        buttonSend.setOnClickListener {
            val text = editTextMessage.text.toString()
            if (text.isNotBlank()) {
                sendMessage(text, null)
                editTextMessage.text.clear()
            }
        }

        buttonAddPhoto.setOnClickListener {
            val options = arrayOf("사진 찍기", "갤러리에서 선택")
            AlertDialog.Builder(requireContext())
                .setTitle("사진 추가")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> checkCameraPermissionAndLaunch()
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }.show()
        }
    }

    private fun sendMessage(text: String?, imageUri: Uri?) {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1) 사용자 메시지 저장
            val viewType = if (text != null) VIEW_TYPE_USER_TEXT else VIEW_TYPE_USER_IMAGE
            val userMsg = Message(
                roomId = roomId,
                text = text,
                imageUriString = imageUri?.toString(),
                viewType = viewType
            )
            chatDao.insertMessage(userMsg)

            // 2) 봇 응답 처리
            if (imageUri != null) {
                insertBotTyping()
                handleImageMessage(imageUri)
            } else if (!text.isNullOrBlank()) {
                insertBotTyping()
                handleTextMessage(text)
            }
        }
    }

    private suspend fun handleTextMessage(text: String) {
        try {
            val reply = withContext(Dispatchers.IO) {
                ApiClient.api.chat(ChatRequest(text)).reply
            }
            replaceTypingWith(reply)
        } catch (e: Exception) {
            replaceTypingWith("답변 중 오류가 발생했어요. 네트워크를 확인하고 다시 시도해 주세요.")
        }
    }

    private suspend fun handleImageMessage(uri: Uri) {
        try {
            val file = withContext(Dispatchers.IO) { copyUriToCacheFile(uri) }
            val part = MultipartBody.Part.createFormData(
                name = "image",
                filename = file.name,
                body = file.asRequestBody("image/*".toMediaType())
            )
            val result = withContext(Dispatchers.IO) {
                ApiClient.api.analyzeImage(part)
            }

            val label = result.result?.label ?: "healthy"
            val info = findDiseaseByLabel(label)

            if (label.equals("healthy", ignoreCase = true) || info == null || info.label.equals("healthy", true)) {
                val extra = try {
                    withContext(Dispatchers.IO) {
                        ApiClient.api.chat(ChatRequest("분석 결과 정상입니다. 관리 팁 알려줘")).reply
                    }
                } catch (_: Exception) {
                    "사진상 특별한 이상은 없어 보입니다. 주기적인 수분/광량 체크를 추천해요."
                }
                replaceTypingWith("정상으로 보입니다.\n$extra")
            } else {
                val msg = buildString {
                    appendLine("진단: ${info.nameKo}")
                    appendLine()
                    appendLine("설명: ${info.description}")
                    appendLine()
                    append("해결책: ${info.solution}")
                }
                replaceTypingWith(msg)
            }
        } catch (e: Exception) {
            replaceTypingWith("이미지 분석 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.")
        }
    }

    // "…"(타이핑) 메시지를 먼저 넣고, 나중에 실제 답변으로 대체
    private suspend fun insertBotTyping() {
        chatDao.insertMessage(
            Message(
                roomId = roomId,
                text = "…",
                imageUriString = null,
                viewType = VIEW_TYPE_BOT_TEXT
            )
        )
    }

    private suspend fun replaceTypingWith(text: String) {
        // 가장 마지막 봇 "…" 메시지를 찾아 교체. (간단히 새 메시지 추가해도 무방)
        chatDao.insertMessage(
            Message(
                roomId = roomId,
                text = text,
                imageUriString = null,
                viewType = VIEW_TYPE_BOT_TEXT
            )
        )
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        cameraImageUri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )!!
        takePhotoLauncher.launch(cameraImageUri)
    }

    private fun copyUriToCacheFile(uri: Uri): File {
        val file = File.createTempFile("upload_", ".jpg", requireContext().cacheDir)
        requireContext().contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(file).use { output -> input?.copyTo(output) }
        }
        return file
    }

    // ---------- 로컬 질병 가이드 로딩/검색 ----------
    private fun loadDiseaseJsonOnce() {
        if (diseaseList.isNotEmpty()) return
        try {
            val json = requireContext().assets.open("care_guides.json")
                .bufferedReader()
                .use(BufferedReader::readText)
            diseaseList = JsonHelper.parseDiseaseJson(json)
        } catch (e: Exception) {
            diseaseList = emptyList()
        }
    }

    private fun findDiseaseByLabel(label: String): DiseaseInfo? {
        val norm = label.trim().lowercase()
        return diseaseList.firstOrNull {
            it.label.trim().lowercase() == norm ||
                    it.nameKo.trim().lowercase() == norm
        }
    }
    // ---------------------------------------------

    companion object {
        fun newInstance(roomId: Long, roomTitle: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong("ROOM_ID", roomId)
                putString("ROOM_TITLE", roomTitle)
            }
        }
    }
}

// ---- DTO & JSON 유틸(간단하게 이 파일에 포함) ----
data class DiseaseInfo(
    val label: String,
    val nameKo: String,
    val description: String,
    val solution: String
)

object JsonHelper {
    // Gson 사용 (이미 앱에 Gson 의존성 있음)
    fun parseDiseaseJson(json: String): List<DiseaseInfo> {
        val type = com.google.gson.reflect.TypeToken.getParameterized(
            List::class.java, DiseaseInfo::class.java
        ).type
        return com.google.gson.Gson().fromJson(json, type)
    }
}

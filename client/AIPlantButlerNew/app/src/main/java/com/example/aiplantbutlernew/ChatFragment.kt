package com.example.aiplantbutlernew

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatFragment : Fragment() {

    private val messageList = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var cameraImageUri: Uri

    // --- 새로운 권한 요청 런처 ---
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한이 승인되면, 카메라를 실행합니다.
                launchCamera()
            } else {
                // 권한이 거부되면 사용자에게 안내 메시지를 보여줍니다.
                Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    // 카메라 촬영 런처
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val message = Message(null, cameraImageUri, VIEW_TYPE_USER_IMAGE)
            addMessageToList(message)
            triggerFakeBotResponse(isImageAnalysis = true)
        }
    }

    // 갤러리 선택 런처
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val message = Message(null, it, VIEW_TYPE_USER_IMAGE)
            addMessageToList(message)
            triggerFakeBotResponse(isImageAnalysis = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ... (findViewById 및 다른 설정들은 이전과 동일) ...
        recyclerView = view.findViewById(R.id.recycler_view_messages)
        val editTextMessage: EditText = view.findViewById(R.id.edit_text_message)
        val buttonSend: ImageButton = view.findViewById(R.id.button_send)
        val buttonAddPhoto: ImageButton = view.findViewById(R.id.button_add_photo)

        chatAdapter = ChatAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter

        buttonSend.setOnClickListener {
            val text = editTextMessage.text.toString()
            if (text.isNotBlank()) {
                val message = Message(text, null, VIEW_TYPE_USER_TEXT)
                addMessageToList(message)
                editTextMessage.text.clear()
                triggerFakeBotResponse(isImageAnalysis = false)
            }
        }

        buttonAddPhoto.setOnClickListener {
            val options = arrayOf("사진 찍기", "갤러리에서 선택")
            AlertDialog.Builder(requireContext())
                .setTitle("사진 추가")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // 사진 찍기
                            checkCameraPermissionAndLaunch() // 카메라 실행 함수 호출
                        }
                        1 -> { // 갤러리
                            pickImageLauncher.launch("image/*")
                        }
                    }
                }
                .show()
        }
    }

    // --- 카메라 권한을 확인하고 실행하는 함수 (새로 추가) ---
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 있다면, 바로 카메라를 실행합니다.
                launchCamera()
            }
            else -> {
                // 권한이 없다면, 사용자에게 요청합니다.
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // --- 카메라를 실행하는 함수 (새로 추가) ---
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


    // ... (addMessageToList, triggerFakeBotResponse 함수는 이전과 동일) ...
    private fun addMessageToList(message: Message) {
        messageList.add(message)
        chatAdapter.notifyItemInserted(messageList.size -1)
        recyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun triggerFakeBotResponse(isImageAnalysis: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            val responseText = if (isImageAnalysis) {
                "사진을 분석 중입니다... 잎이 약간 노란색을 띠는 것 같네요. 과습의 초기 증상일 수 있습니다."
            } else {
                "그렇군요! 식물에 대해 더 궁금한 점이 있으신가요?"
            }
            val botMessage = Message(responseText, null, VIEW_TYPE_BOT_TEXT)
            addMessageToList(botMessage)
        }, 1500)
    }
}
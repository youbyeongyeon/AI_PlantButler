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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatDao: ChatDao
    private var roomId: Long = -1
    private var roomTitle: String = "새 채팅"

    private lateinit var cameraImageUri: Uri

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { launchCamera() } else { Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show() }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            sendMessage(null, cameraImageUri)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            sendMessage(null, it)
        }
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

        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar_chat)
        recyclerView = view.findViewById(R.id.recycler_view_messages)
        val editTextMessage: EditText = view.findViewById(R.id.edit_text_message)
        val buttonSend: ImageButton = view.findViewById(R.id.button_send)
        val buttonAddPhoto: ImageButton = view.findViewById(R.id.button_add_photo)

        toolbar.title = roomTitle
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.adapter = chatAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager


        viewLifecycleOwner.lifecycleScope.launch {
            chatDao.getMessagesForRoom(roomId).collect { messages ->
                chatAdapter.updateMessages(messages)
                recyclerView.scrollToPosition(messages.size - 1)
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
            AlertDialog.Builder(requireContext()).setTitle("사진 추가").setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }.show()
        }
    }

    private fun sendMessage(text: String?, imageUri: Uri?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val viewType = if (text != null) VIEW_TYPE_USER_TEXT else VIEW_TYPE_USER_IMAGE
            val message = Message(
                roomId = roomId,
                text = text,
                imageUriString = imageUri?.toString(),
                viewType = viewType
            )
            chatDao.insertMessage(message)
            triggerFakeBotResponse(imageUri != null)
        }
    }

    private fun triggerFakeBotResponse(isImageAnalysis: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            val responseText = if (isImageAnalysis) {
                "사진을 분석 중입니다... 잎이 약간 노란색을 띠는 것 같네요. 과습의 초기 증상일 수 있습니다."
            } else {
                "그렇군요! 식물에 대해 더 궁금한 점이 있으신가요?"
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val botMessage = Message(
                    roomId = roomId,
                    text = responseText,
                    imageUriString = null,
                    viewType = VIEW_TYPE_BOT_TEXT
                )
                chatDao.insertMessage(botMessage)
            }
        }, 1500)
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
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

    companion object {
        fun newInstance(roomId: Long, roomTitle: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong("ROOM_ID", roomId)
                putString("ROOM_TITLE", roomTitle)
            }
        }
    }
}
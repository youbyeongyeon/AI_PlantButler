package com.example.aiplantbutlernew

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatFragment : Fragment() {

    private val messageList = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView

    // 갤러리에서 사진을 선택하면 실행될 콜백
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // 사용자가 선택한 이미지를 메시지 리스트에 추가
            val message = Message(null, it, VIEW_TYPE_USER_IMAGE)
            addMessageToList(message)
            // 가짜 챗봇 응답 (사진 분석)
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

        recyclerView = view.findViewById(R.id.recycler_view_messages)
        val editTextMessage: EditText = view.findViewById(R.id.edit_text_message)
        val buttonSend: ImageButton = view.findViewById(R.id.button_send)
        val buttonAddPhoto: ImageButton = view.findViewById(R.id.button_add_photo)

        // RecyclerView 설정
        chatAdapter = ChatAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter

        // 보내기 버튼 클릭
        buttonSend.setOnClickListener {
            val text = editTextMessage.text.toString()
            if (text.isNotBlank()) {
                val message = Message(text, null, VIEW_TYPE_USER_TEXT)
                addMessageToList(message)
                editTextMessage.text.clear()
                // 가짜 챗봇 응답 (텍스트)
                triggerFakeBotResponse(isImageAnalysis = false)
            }
        }

        // 사진 추가(+) 버튼 클릭
        buttonAddPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    // 메시지를 리스트에 추가하고 UI를 업데이트하는 함수
    private fun addMessageToList(message: Message) {
        messageList.add(message)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        recyclerView.scrollToPosition(messageList.size - 1)
    }

    // 가짜 챗봇 응답을 생성하는 함수
    private fun triggerFakeBotResponse(isImageAnalysis: Boolean) {
        // 1.5초 딜레이
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
package com.example.aiplantbutlernew

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ChatListFragment : Fragment(), ChatRoomAdapter.OnItemInteractionListener {

    private lateinit var chatDao: ChatDao
    private lateinit var chatRoomAdapter: ChatRoomAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatDao = AppDatabase.getDatabase(requireContext()).chatDao()

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_chat_rooms)
        val fabNewChat: FloatingActionButton = view.findViewById(R.id.fab_new_chat)

        chatRoomAdapter = ChatRoomAdapter(emptyList(), this)
        recyclerView.adapter = chatRoomAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        viewLifecycleOwner.lifecycleScope.launch {
            chatDao.getAllChatRooms().collect { chatRooms ->
                chatRoomAdapter.updateData(chatRooms)
            }
        }

        fabNewChat.setOnClickListener {
            showRenameDialog(null) // 새 채팅방 생성을 위해 null 전달
        }
    }

    // 채팅방 클릭: 해당 채팅방으로 이동
    override fun onItemClick(chatRoom: ChatRoom) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frame, ChatFragment.newInstance(chatRoom.id, chatRoom.title))
            .addToBackStack(null) // 뒤로가기 지원
            .commit()
    }

    // 채팅방 롱클릭: 관리 메뉴(수정/삭제) 표시
    override fun onItemLongClick(chatRoom: ChatRoom) {
        val options = arrayOf("이름 수정", "삭제")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(chatRoom)
                    1 -> showDeleteDialog(chatRoom)
                }
            }
            .show()
    }

    private fun showRenameDialog(chatRoom: ChatRoom?) {
        val editText = EditText(requireContext())
        editText.setText(chatRoom?.title)

        AlertDialog.Builder(requireContext())
            .setTitle(if (chatRoom == null) "새 채팅 시작" else "채팅방 이름 수정")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                val newTitle = editText.text.toString()
                if (newTitle.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (chatRoom == null) {
                            val newRoom = ChatRoom(title = newTitle)
                            val newRoomId = chatDao.insertChatRoom(newRoom)
                            onItemClick(ChatRoom(id = newRoomId, title = newTitle))
                        } else {
                            val updatedRoom = chatRoom.copy(title = newTitle)
                            chatDao.updateChatRoom(updatedRoom)
                        }
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteDialog(chatRoom: ChatRoom) {
        AlertDialog.Builder(requireContext())
            .setTitle("채팅방 삭제")
            .setMessage("'${chatRoom.title}' 채팅방을 삭제하시겠습니까? 대화 내용이 모두 사라집니다.")
            .setPositiveButton("삭제") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    chatDao.deleteChatRoom(chatRoom)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
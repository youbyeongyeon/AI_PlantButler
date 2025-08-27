package com.example.aiplantbutlernew

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRoomAdapter(
    private var chatRooms: List<ChatRoom>,
    private val listener: OnItemInteractionListener // 리스너 이름 변경
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    // 클릭과 롱클릭을 모두 처리하는 인터페이스
    interface OnItemInteractionListener {
        fun onItemClick(chatRoom: ChatRoom)
        fun onItemLongClick(chatRoom: ChatRoom)
    }

    class ChatRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.text_view_chat_title)
        val timestamp: TextView = itemView.findViewById(R.id.text_view_chat_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_room, parent, false)
        return ChatRoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val room = chatRooms[position]
        holder.title.text = room.title
        val sdf = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        holder.timestamp.text = sdf.format(Date(room.timestamp))

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener { listener.onItemClick(room) }

        // 롱클릭 리스너 설정
        holder.itemView.setOnLongClickListener {
            listener.onItemLongClick(room)
            true // 이벤트 처리를 완료했음을 알림
        }
    }

    override fun getItemCount() = chatRooms.size

    fun updateData(newChatRooms: List<ChatRoom>) {
        chatRooms = newChatRooms
        notifyDataSetChanged()
    }
}
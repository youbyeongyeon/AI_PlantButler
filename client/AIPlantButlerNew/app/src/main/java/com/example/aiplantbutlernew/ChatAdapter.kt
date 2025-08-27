package com.example.aiplantbutlernew

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private var messageList: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class SentTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.text_view_message)
    }

    inner class ReceivedTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.text_view_message)
    }

    inner class SentImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageImage: ImageView = itemView.findViewById(R.id.image_view_sent)
    }

    override fun getItemViewType(position: Int): Int {
        return messageList[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
                SentTextViewHolder(view)
            }
            VIEW_TYPE_BOT_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
                ReceivedTextViewHolder(view)
            }
            VIEW_TYPE_USER_IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_image_sent, parent, false)
                SentImageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_USER_TEXT -> {
                val sentHolder = holder as SentTextViewHolder
                sentHolder.messageText.text = message.text
            }
            VIEW_TYPE_BOT_TEXT -> {
                val receivedHolder = holder as ReceivedTextViewHolder
                receivedHolder.messageText.text = message.text
            }
            VIEW_TYPE_USER_IMAGE -> {
                val imageHolder = holder as SentImageViewHolder
                // imageUriString을 Uri로 변환하여 이미지 설정
                message.imageUriString?.let {
                    imageHolder.messageImage.setImageURI(Uri.parse(it))
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    // --- 이 함수가 누락되었습니다 ---
    fun updateMessages(newMessages: List<Message>) {
        messageList.clear()
        messageList.addAll(newMessages)
        notifyDataSetChanged()
    }
}
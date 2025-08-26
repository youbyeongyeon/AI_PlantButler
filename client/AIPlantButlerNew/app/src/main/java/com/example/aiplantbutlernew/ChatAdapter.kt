package com.example.aiplantbutlernew

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messageList: MutableList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
                imageHolder.messageImage.setImageURI(message.imageUri)
            }
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}
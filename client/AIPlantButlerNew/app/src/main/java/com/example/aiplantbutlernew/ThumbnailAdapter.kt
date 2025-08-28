package com.example.aiplantbutlernew

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ThumbnailAdapter(
    private val photoUris: List<Uri>,
    private val onPhotoClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.imageView.setImageURI(photoUris[position])
        holder.itemView.setOnClickListener {
            onPhotoClick(position)
        }
    }

    override fun getItemCount() = photoUris.size
}
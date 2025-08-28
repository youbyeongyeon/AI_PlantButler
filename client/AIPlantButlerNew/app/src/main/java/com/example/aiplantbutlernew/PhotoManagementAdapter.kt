package com.example.aiplantbutlernew

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PhotoManagementAdapter(
    private val originalPhotoList: MutableList<Uri>
) : RecyclerView.Adapter<PhotoManagementAdapter.PhotoViewHolder>() {

    private val selectionOrder = mutableListOf<Uri>()

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view_photo_item)
        val overlay: View = itemView.findViewById(R.id.view_overlay)
        val orderNumber: TextView = itemView.findViewById(R.id.text_view_order_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_management, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val uri = originalPhotoList[position]
        holder.imageView.setImageURI(uri)

        val order = selectionOrder.indexOf(uri)
        if (order != -1) {
            holder.orderNumber.text = (order + 1).toString()
            holder.orderNumber.visibility = View.VISIBLE
            holder.overlay.visibility = View.VISIBLE
        } else {
            holder.orderNumber.visibility = View.GONE
            holder.overlay.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            toggleSelection(uri)
        }
    }

    private fun toggleSelection(uri: Uri) {
        if (selectionOrder.contains(uri)) {
            selectionOrder.remove(uri)
        } else {
            selectionOrder.add(uri)
        }
        notifyDataSetChanged()
    }

    fun getFinalOrderedList(): MutableList<Uri> {
        val unselectedPhotos = originalPhotoList.filter { !selectionOrder.contains(it) }
        val finalList = mutableListOf<Uri>()
        finalList.addAll(selectionOrder)
        finalList.addAll(unselectedPhotos)
        return finalList
    }

    fun getSelectedPhotos(): List<Uri> {
        return selectionOrder.toList()
    }

    override fun getItemCount() = originalPhotoList.size
}
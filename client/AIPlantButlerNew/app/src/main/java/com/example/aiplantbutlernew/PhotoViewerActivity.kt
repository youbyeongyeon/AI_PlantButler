package com.example.aiplantbutlernew

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar

class PhotoViewerAdapter(private val photoUris: List<String>) : RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder>() {
    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view_fullscreen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_viewer, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.imageView.setImageURI(Uri.parse(photoUris[position]))
    }

    override fun getItemCount() = photoUris.size
}

class PhotoViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar_photo_viewer)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager_photos)

        val photoUris = intent.getStringArrayListExtra("PHOTO_URIS") ?: arrayListOf()
        val date = intent.getStringExtra("DATE")
        val currentPosition = intent.getIntExtra("POSITION", 0)

        toolbar.title = date
        toolbar.setNavigationOnClickListener { finish() }

        viewPager.adapter = PhotoViewerAdapter(photoUris)
        viewPager.setCurrentItem(currentPosition, false)
    }
}
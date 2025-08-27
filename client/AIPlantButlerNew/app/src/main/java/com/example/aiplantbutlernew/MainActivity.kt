package com.example.aiplantbutlernew

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fabHome: FloatingActionButton = findViewById(R.id.fab_home)
        val buttonChat: ImageButton = findViewById(R.id.button_chat)
        val buttonCalendar: ImageButton = findViewById(R.id.button_calendar)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        buttonChat.setOnClickListener {
            loadFragment(ChatListFragment()) // ChatFragment 대신 ChatListFragment를 로드
        }

        buttonCalendar.setOnClickListener {
            loadFragment(CalendarFragment())
        }

        fabHome.setOnClickListener {
            loadFragment(HomeFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
    }
}
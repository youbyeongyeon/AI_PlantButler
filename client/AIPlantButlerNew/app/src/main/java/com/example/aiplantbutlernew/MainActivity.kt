package com.example.aiplantbutlernew

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // XML에 있는 UI 부품들을 가져오기
        val bottomAppBar: BottomAppBar = findViewById(R.id.bottom_app_bar)
        val fabHome: FloatingActionButton = findViewById(R.id.fab_home)

        // 앱이 처음 켜졌을 때 홈 화면을 기본으로 보여주기
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // 하단 앱 바의 메뉴(챗봇, 캘린더) 아이템 클릭 리스너 설정
        bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_chat -> {
                    loadFragment(ChatFragment())
                    true
                }
                R.id.navigation_calendar -> {
                    loadFragment(CalendarFragment())
                    true
                }
                else -> false
            }
        }

        // 원형 홈 버튼(FAB) 클릭 리스너 설정
        fabHome.setOnClickListener {
            loadFragment(HomeFragment())
        }
    }

    // 화면(Fragment)을 교체해주는 함수
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frame, fragment)
            .commit()
    }
}
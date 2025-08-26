package com.example.aiplantbutlernew

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

// 데이터를 주고받는 복잡한 코드를 모두 제거한 가장 기본적인 형태의 Fragment 입니다.
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 이 Fragment가 화면에 보여줄 UI로 fragment_home.xml 파일을 사용하라고 지정합니다.
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
}
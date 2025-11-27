package com.hehe.travel

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SammySecondQuestionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sammy_second_question)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // XML에서 설정한 패딩 값 유지하면서 시스템 바 insets 추가
            val originalPaddingLeft = v.paddingLeft
            val originalPaddingTop = v.paddingTop
            val originalPaddingRight = v.paddingRight
            val originalPaddingBottom = v.paddingBottom
            v.setPadding(
                originalPaddingLeft + systemBars.left,
                originalPaddingTop + systemBars.top,
                originalPaddingRight + systemBars.right,
                originalPaddingBottom + systemBars.bottom
            )
            insets
        }

        setupHeader()
    }

    private fun setupHeader() {
        val tvHeader = findViewById<TextView>(R.id.tvHeader)

        // Intent에서 사용자 이름 가져오기 (이전 화면에서 전달된 경우)
        val userName = intent.getStringExtra("USER_NAME") ?: "사용자"

        // 헤더 텍스트 설정
        tvHeader.text = "${userName}님께 여행 일정을 추천드리기 전,여행 상태는 어때요?"
    }
}
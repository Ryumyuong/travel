package com.hehe.travel

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class plan_without_sammy_pass : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_plan_recommended_without_sammy_pass)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 비행기 추천 클릭
        findViewById<LinearLayout>(R.id.layoutFlight).setOnClickListener {
            // 제주항공 웹사이트나 상세 페이지로 이동
            Toast.makeText(this, "제주항공 7C1603편 상세 정보", Toast.LENGTH_SHORT).show()
        }

        // 숙소 추천 클릭
        findViewById<LinearLayout>(R.id.layoutAccommodation).setOnClickListener {
            // Hotels.com 웹사이트로 이동
            Toast.makeText(this, "호시노야 발리 상세 정보", Toast.LENGTH_SHORT).show()
        }

        // 맛집 추천 클릭
        findViewById<LinearLayout>(R.id.layoutRestaurant).setOnClickListener {
            // 맛집 상세 정보 페이지로 이동
            Toast.makeText(this, "Cretya Ubud 상세 정보", Toast.LENGTH_SHORT).show()
        }

        // 여행 저장하기 버튼
        findViewById<AppCompatButton>(R.id.btnSaveTrip).setOnClickListener {
            Toast.makeText(this, "여행이 저장되었습니다", Toast.LENGTH_SHORT).show()
            // TODO: 여행 저장 로직 구현
        }

        // 새미패스 작성하기 버튼
        findViewById<AppCompatButton>(R.id.btnCreateSammyPass).setOnClickListener {
            // TODO: 새미패스 작성 화면으로 이동
            Toast.makeText(this, "새미패스 작성 페이지로 이동", Toast.LENGTH_SHORT).show()
        }
    }
}
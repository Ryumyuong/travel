package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PreferenceRecommendActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference_recommend)

        // TODO: 실제 취향 값은 설문(QuestionnaireActivity) 저장값/SharedPreferences/Firestore 등에서 로드
        val likes = setOf("미식", "자연") // 예시

        val rec = when {
            "미식" in likes && "자연" in likes -> "이탈리아 남부 해안 · 스위스 루체른"
            "미식" in likes -> "프랑스 파리 · 스페인 바르셀로나"
            "자연" in likes -> "스위스 인터라켄 · 호주 태즈매니아"
            else -> "일본 교토 · 태국 방콕"
        }
        findViewById<TextView>(R.id.tvResult).text = "추천 목적지: $rec"
        setupBottomNav(R.id.tab_profile)
    }

    private fun AppCompatActivity.setupBottomNav(selectedId: Int) {
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.tab_country -> {
                    startActivity(
                        Intent(this, SearchActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_search -> {
                    startActivity(
                        Intent(this, StartActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_profile -> {
                    startActivity(
                        Intent(this, MyInfoActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }
}

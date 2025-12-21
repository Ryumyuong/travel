package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivityMyInfoBinding
import java.text.SimpleDateFormat
import java.util.*

class MyInfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMyInfoBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        initAuthAndGoogleClient()

        loadUserProfile()
        loadPlanHistory()
        setupClickListeners()
        setupBottomNav(R.id.tab_profile)
    }

    // 사용자 프로필 로드
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nickname = document.getString("nickname") ?: "여행자"
                    binding.tvGreeting.text = "$nickname 님"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "프로필 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // planhistory에서 최근 여행 로드
    private fun loadPlanHistory() {
        val uid = auth.currentUser?.uid ?: return

        android.util.Log.d("MyInfo", "Loading planhistory for uid: $uid")

        // orderBy 없이 쿼리 (복합 인덱스 불필요)
        Firebase.firestore.collection("planhistory")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("MyInfo", "Documents found: ${documents.size()}")

                if (!documents.isEmpty) {
                    // savedAt 기준으로 클라이언트에서 정렬 (최신순)
                    val sortedDocs = documents.documents.sortedByDescending {
                        it.getTimestamp("savedAt")?.toDate()?.time ?: 0L
                    }
                    val doc = sortedDocs.first()

                    val country = doc.getString("country") ?: ""
                    val nights = doc.getLong("nights")?.toInt() ?: 0
                    val daysCount = doc.getLong("daysCount")?.toInt() ?: (nights + 1)
                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val travelStyle = doc.getString("travelStyle") ?: ""
                    val savedAt = doc.getTimestamp("savedAt")

                    android.util.Log.d("MyInfo", "Trip loaded: $country ${nights}박${daysCount}일")

                    // 여행 제목: "발리 4박5일 휴양 여행"
                    val styleKeyword = extractStyleKeyword(travelStyle)
                    val tripTitle = "$country ${nights}박${daysCount}일 $styleKeyword 여행"
                    binding.tvTripTitle.text = tripTitle

                    // 날짜: "2025.08.01 - 2025.08.05"
                    val formattedDate = formatDateRange(startDate, endDate)
                    binding.tvTripDate.text = formattedDate

                    // 저장 시간으로부터 얼마나 지났는지
//                    val timeAgo = getTimeAgo(savedAt?.toDate())
//                    binding.tvTimeAgo.text = timeAgo
//                    binding.tvTimeAgo.visibility = if (timeAgo.isNotEmpty()) View.VISIBLE else View.GONE

                    // 카드 표시
                    binding.tvDescription.visibility = View.VISIBLE
                    binding.cardHistory.visibility = View.VISIBLE

                } else {
                    android.util.Log.d("MyInfo", "No planhistory documents found")
                    // 저장된 여행이 없으면 카드 숨기고 empty state 표시
                    binding.cardHistory.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MyInfo", "Error: ${e.message}", e)
                Toast.makeText(this, "여행 기록 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 여행 스타일에서 키워드 추출 (예: "계획적인 여행자 • 활동적인..." → "휴양" 또는 첫번째 키워드)
    private fun extractStyleKeyword(travelStyle: String): String {
        return when {
            travelStyle.contains("여유로운") -> "휴양"
            travelStyle.contains("활동적") -> "액티비티"
            travelStyle.contains("모험") -> "모험"
            travelStyle.contains("계획적") -> "알찬"
            else -> "힐링"
        }
    }

    // 날짜 포맷: "2025-08-01" → "2025.08.01"
    private fun formatDateRange(startDate: String, endDate: String): String {
        val formatted1 = startDate.replace("-", ".")
        val formatted2 = endDate.replace("-", ".")
        return "$formatted1 - $formatted2"
    }

    // 시간 경과 계산: "7월 전", "3일 전", "방금 전"
    private fun getTimeAgo(date: Date?): String {
        if (date == null) return ""

        val now = Date()
        val diffMillis = now.time - date.time
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24
        val diffMonths = diffDays / 30

        return when {
            diffMonths > 0 -> "${diffMonths}월 전"
            diffDays > 0 -> "${diffDays}일 전"
            diffHours > 0 -> "${diffHours}시간 전"
            diffMinutes > 0 -> "${diffMinutes}분 전"
            else -> "방금 전"
        }
    }

    private fun setupClickListeners() {
        // 여행 기록 카드 클릭 → 여행 히스토리 목록
        binding.cardHistory.setOnClickListener {
            startActivity(Intent(this, TravelHistoryActivity::class.java))
        }

        // 정보수정 클릭 → SammyFirstQuestionActivity로 이동 (기존 내용 수정)
        binding.btnEditInfo.setOnClickListener {
            val intent = Intent(this, SammyFirstQuestionActivity::class.java)
            intent.putExtra("isEditMode", true)  // 수정 모드 플래그
            startActivity(intent)
            finish()
        }

        // 로그아웃 클릭
        binding.btnLogout.setOnClickListener {
            signOut()
        }
    }

    private fun initAuthAndGoogleClient() {
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun AppCompatActivity.setupBottomNav(selectedId: Int) {
        val bottom = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottom.selectedItemId = selectedId

        bottom.setOnItemSelectedListener { item ->
            if (item.itemId == selectedId) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.tab_country -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    finish()
                    true
                }
                R.id.tab_search -> {
                    startActivity(Intent(this, StartActivity::class.java))
                    finish()
                    true
                }
                R.id.tab_profile -> {
                    true
                }
                else -> false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            super.onBackPressed()
            finishAffinity()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()
            goToLogin()
        }.addOnFailureListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
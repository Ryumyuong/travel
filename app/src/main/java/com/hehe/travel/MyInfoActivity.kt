package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
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
    private lateinit var historyAdapter: TravelHistoryAdapter
    private var backPressedTime: Long = 0

    // 여행 기록 리스트
    private val allHistoryItems = mutableListOf<TravelHistoryItem>()
    private var historyLoaded = false
    private var planHistoryLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        initAuthAndGoogleClient()

        setupRecyclerView()
        loadUserProfile()
        loadAllHistory()
        setupClickListeners()
        setupBottomNav(R.id.tab_profile)
    }

    private fun setupRecyclerView() {
        historyAdapter = TravelHistoryAdapter { item ->
            // 아이템 클릭 시 바로 PlanActivity로 이동
            loadAndNavigateToPlan(item)
        }
        binding.rvTravelHistory.layoutManager = LinearLayoutManager(this)
        binding.rvTravelHistory.adapter = historyAdapter
    }

    // Firestore에서 상세 데이터 로드 후 적절한 Activity로 이동
    private fun loadAndNavigateToPlan(item: TravelHistoryItem) {
        val uid = auth.currentUser?.uid ?: return

        // 로딩 표시
        Toast.makeText(this, "일정을 불러오는 중...", Toast.LENGTH_SHORT).show()

        // type에 따라 다른 컬렉션에서 로드
        val docRef = when (item.type) {
            3 -> Firebase.firestore.collection("planhistory").document(item.documentId)
            else -> Firebase.firestore.collection("history").document(uid)
                .collection("trips").document(item.documentId)
        }

        docRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val country = doc.getString("country") ?: ""
                val nickname = doc.getString("nickname") ?: ""
                val nights = doc.getLong("nights")?.toInt() ?: 0
                val startDate = doc.getString("startDate") ?: ""
                val endDate = doc.getString("endDate") ?: ""
                val travelStyle = doc.getString("travelStyle") ?: ""
                val keywords = doc.get("keywords") as? List<String> ?: emptyList()
                val hasSemiPass = doc.getBoolean("hasSemiPass") ?: false
                val budgetLabel = doc.getString("budgetLabel") ?: ""

                // days 데이터 확인
                val days = doc.get("days") as? List<Map<String, Any>> ?: emptyList()

                if (days.isNotEmpty()) {
                    // days 데이터가 있으면 → PlanActivity로 이동
                    val daysJson = convertDaysToJson(days)
                    val intent = Intent(this, PlanActivity::class.java).apply {
                        putExtra("country", country)
                        putExtra("login", nickname)
                        putExtra("startDate", startDate)
                        putExtra("endDate", endDate)
                        putExtra("nights", nights)
                        putExtra("travelStyle", travelStyle)
                        putStringArrayListExtra("keywords", ArrayList(keywords))
                        putExtra("itineraryJson", daysJson)
                    }
                    startActivity(intent)
                } else {
                    // days 데이터가 없으면 → TravelResultDetailActivity로 이동 (항공/숙소/맛집)
                    val flight = doc.getString("flight") ?: ""
                    val accommodation = doc.getString("accommodation") ?: ""
                    val restaurant = doc.getString("restaurant") ?: ""

                    val intent = Intent(this, TravelResultDetailActivity::class.java).apply {
                        putExtra("country", country)
                        putExtra("nickname", nickname)
                        putExtra("flight", flight)
                        putExtra("accommodation", accommodation)
                        putExtra("restaurant", restaurant)
                        putExtra("hasSemiPass", hasSemiPass)
                        putExtra("budgetLabel", budgetLabel)
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestore days 데이터를 JSON 문자열로 변환
    private fun convertDaysToJson(days: List<Map<String, Any>>): String {
        val jsonArray = org.json.JSONArray()

        for (day in days) {
            val dayObj = org.json.JSONObject()
            dayObj.put("day", day["day"])
            dayObj.put("title", day["title"])

            val placesArray = org.json.JSONArray()
            val places = day["places"] as? List<Map<String, Any>> ?: emptyList()

            for (place in places) {
                val placeObj = org.json.JSONObject()
                placeObj.put("name", place["name"] ?: "")
                placeObj.put("description", place["description"] ?: "")
                placeObj.put("time", place["time"] ?: "")
                placeObj.put("duration", place["duration"] ?: "")
                placeObj.put("tip", place["tip"] ?: "")
                placesArray.put(placeObj)
            }

            dayObj.put("places", placesArray)
            jsonArray.put(dayObj)
        }

        return jsonArray.toString()
    }

    // 모든 히스토리 로드 (history + planhistory)
    private fun loadAllHistory() {
        allHistoryItems.clear()
        historyLoaded = false
        planHistoryLoaded = false

        loadHistoryCollection()
        loadPlanHistoryCollection()
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
                    binding.tvDescription.text = "${nickname}님이 찾은 여행,\n한눈에 보기 쉽게 정리했어요!"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "프로필 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // history 컬렉션에서 로드 (AI 생성 일정 - 새미패스 유/무)
    private fun loadHistoryCollection() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("history")
            .document(uid)
            .collection("trips")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MyInfo", "History trips found: ${documents.size()}")

                for (doc in documents) {
                    // createdAt이 없는 이전 데이터는 건너뛰기
                    val savedAt = doc.getTimestamp("createdAt")?.toDate() ?: continue

                    val hasSemiPass = doc.getBoolean("hasSemiPass") ?: false
                    val country = doc.getString("country") ?: ""
                    val nights = doc.getLong("nights")?.toInt() ?: 0
                    val daysCount = doc.getLong("daysCount")?.toInt() ?: 0
                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val travelStyle = doc.getString("travelStyle") ?: ""
                    val budgetLabel = doc.getString("budgetLabel") ?: ""

                    val type = if (hasSemiPass) 2 else 1  // 2: 새미패스, 1: 일반

                    allHistoryItems.add(
                        TravelHistoryItem(
                            documentId = doc.id,
                            country = country,
                            nights = nights,
                            daysCount = daysCount,
                            startDate = startDate,
                            endDate = endDate,
                            travelStyle = travelStyle,
                            hasSemiPass = hasSemiPass,
                            type = type,
                            savedAt = savedAt,
                            budgetLabel = budgetLabel
                        )
                    )
                }

                historyLoaded = true
                checkAndDisplayHistory()
            }
            .addOnFailureListener { e ->
                Log.e("MyInfo", "History 로드 실패: ${e.message}")
                historyLoaded = true
                checkAndDisplayHistory()
            }
    }

    // planhistory 컬렉션에서 로드 (취향맞춤 저장)
    private fun loadPlanHistoryCollection() {
        val uid = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("planhistory")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MyInfo", "PlanHistory found: ${documents.size()}")

                for (doc in documents) {
                    val country = doc.getString("country") ?: ""
                    val nights = doc.getLong("nights")?.toInt() ?: 0
                    val daysCount = doc.getLong("daysCount")?.toInt() ?: 0
                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val travelStyle = doc.getString("travelStyle") ?: ""
                    val savedAt = doc.getTimestamp("savedAt")?.toDate()

                    allHistoryItems.add(
                        TravelHistoryItem(
                            documentId = doc.id,
                            country = country,
                            nights = nights,
                            daysCount = daysCount,
                            startDate = startDate,
                            endDate = endDate,
                            travelStyle = travelStyle,
                            hasSemiPass = true,
                            type = 3,  // 3: 취향맞춤 (planhistory)
                            savedAt = savedAt
                        )
                    )
                }

                planHistoryLoaded = true
                checkAndDisplayHistory()
            }
            .addOnFailureListener { e ->
                Log.e("MyInfo", "PlanHistory 로드 실패: ${e.message}")
                planHistoryLoaded = true
                checkAndDisplayHistory()
            }
    }

    // 두 컬렉션 모두 로드 완료되면 UI 업데이트
    private fun checkAndDisplayHistory() {
        if (!historyLoaded || !planHistoryLoaded) return

        if (allHistoryItems.isEmpty()) {
            // 저장된 여행 없음
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvDescription.visibility = View.GONE
            binding.rvTravelHistory.visibility = View.GONE
        } else {
            // 최신순 정렬
            val sorted = allHistoryItems.sortedByDescending { it.savedAt?.time ?: 0L }
            historyAdapter.submitList(sorted)

            binding.layoutEmptyState.visibility = View.GONE
            binding.tvDescription.visibility = View.VISIBLE
            binding.rvTravelHistory.visibility = View.VISIBLE
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
        // 정보수정 클릭 → SammyFirstQuestionActivity로 이동 (기존 내용 수정)
        binding.btnEditInfo.setOnClickListener {
            val intent = Intent(this, SammySecondQuestionActivity::class.java)
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
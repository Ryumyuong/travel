package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.common.reflect.TypeToken
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.gson.GsonBuilder
import com.hehe.travel.Retrofit.retrofit
import com.hehe.travel.databinding.ActivityDateRangeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.text.get

class DateRangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDateRangeBinding
    private lateinit var adapter: DayAdapter
    private val range = RangeState()
    private var curYM: YearMonth = YearMonth.now()
    private lateinit var startUtc: String
    private lateinit var name:String
    private var gender:String? = ""
    private var age:Int = 0
    private lateinit var tags:List<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar(binding.rvCalendar, binding.tvMonth)
        setupButtons()
        setupBottomNav()

        startUtc = intent.getStringExtra("country").toString()
        name = intent.getStringExtra("login").toString()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = doc.toObject(Profile::class.java)
                    gender = profile?.gender ?: ""
                    age = profile?.ageDecade ?: 0
                    tags = profile?.tags.orEmpty()
                    // 여기서 확인 버튼 활성화 등
                }
            }

    }

    private fun setupCalendar(rv: RecyclerView, tvMonth: TextView) {
        rv.setupCalendarGrid()
        adapter = DayAdapter(range) { onDayClick(it) }
        rv.adapter = adapter
        val barColor = getColor(R.color.range_bar_12) // 연한 파란 배경 색 (예: #E8EEF9 계열로)

        binding.rvCalendar.addItemDecoration(RangeCapsuleDecoration(range, barColor))


        renderMonth(tvMonth)

        rv.setupCalendarGrid()
        adapter = DayAdapter(range) { onDayClick(it) }
        rv.adapter = adapter
        renderMonth(tvMonth)



        binding.btnPrevMonth.setOnClickListener {
            curYM = curYM.minusMonths(1)
            renderMonth(tvMonth)
        }
        binding.btnNextMonth.setOnClickListener {
            curYM = curYM.plusMonths(1)
            renderMonth(tvMonth)
        }
    }

    private fun renderMonth(tvMonth: TextView) {
        tvMonth.text = MONTH_FMT.format(curYM.atDay(1))
        adapter.submitList(buildMonth(curYM))
        updatePickedText()
    }

    private fun onDayClick(date: LocalDate) {
        // 범위 선택 토글: start 비었으면 start, 있으면 end 설정, 둘 다 있으면 초기화 후 start
        if (range.start == null) {
            range.start = date
            range.end = null
        } else if (range.end == null) {
            range.end = date
            // start > end인 경우도 허용(표시는 내부에서 자동 정렬)
        } else {
            range.start = date
            range.end = null
        }
        adapter.notifyDataSetChanged()
        updatePickedText()
    }

    private fun updatePickedText() {
        val s = range.start; val e = range.end ?: range.start
        binding.tvPicked.text = if (s == null) {
            "기간을 선택해주세요."
        } else {
            val a = if (e == null) s else minOf(s, e)
            val b = if (e == null) s else maxOf(s, e)
            "선택한 기간: $a ~ $b"
        }
    }

    private fun setupButtons() {
        binding.btnPrev.setOnClickListener { finish() }

        binding.btnConfirm.setOnClickListener {
            val s = range.start
            val e = range.end ?: range.start
            if (s == null || e == null) {
                Toast.makeText(this, "날짜 범위를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 프로필이 아직 안 왔으면 막기
            if (gender.isNullOrBlank()) {
                Toast.makeText(this, "프로필을 불러오는 중입니다. 잠시만요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val a = minOf(s, e)
            val b = maxOf(s, e)
            val startStr = a.toString()   // "YYYY-MM-DD"
            val endStr   = b.toString()

            val api = retrofit.create(RecommendApi::class.java)

            lifecycleScope.launch {
                try {
                    // 1) 토큰 획득 (코루틴에서 await)
                    val user  = FirebaseAuth.getInstance().currentUser ?: run {
                        Toast.makeText(this@DateRangeActivity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val token = user.getIdToken(false).await().token ?: run {
                        Toast.makeText(this@DateRangeActivity, "토큰 오류", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // 2) 요청 바디 구성
                    val request = RecommendRequest(
                        country = startUtc,
                        startDate = startStr,
                        endDate = endStr,
                        gender = gender,
                        age = age,
                        preference = tags           // 혹은 collectCheckedChips() 사용
                    )

                    val resp1 = api.getRecommendationLegacy(request, "Bearer $token")
                    if (resp1.isSuccessful) {
                        val rawText = resp1.body()?.result ?: ""
                        val days = parseDaysFromResult(rawText)
                        if (!days.isNullOrEmpty()) {
                            days.forEach { d -> Log.d("AI_TRIP", "${d.day}일차: ${d.title} / 장소=${d.places.map { it.name }}") }
                                if (days.isNotEmpty()) {
                                    saveItineraryToHistory(
                                        country = startUtc,        // 너가 쓰는 나라 값
                                        startDate = startStr,      // "YYYY-MM-DD"
                                        endDate   = endStr,
                                        days = days,
                                        onDone = { tripId ->
                                            Log.d("AI_TRIP", "히스토리 저장 완료: $tripId")
                                            val countryKey = intent.getStringExtra("countryKey")
                                            val go = Intent(this@DateRangeActivity, PlanActivity::class.java).apply {
                                                putExtra("startUtcMillis", startStr)
                                                putExtra("endUtcMillis", endStr)
                                                putExtra("countryKey", countryKey)
                                                putExtra("country", startUtc)
                                                putExtra("login", name)
                                                putStringArrayListExtra("keywords", ArrayList(tags))
                                            }
                                            startActivity(go)
                                        },
                                        onError = { e ->
                                            Log.e("AI_TRIP", "히스토리 저장 실패: ${e.message}")
                                        }
                                    )
                                }

                        } else {
                            Log.e("AI_TRIP", "파싱 실패 또는 빈 일정")
                        }
                    }


                } catch (e: HttpException) {
                    Log.e("AI_TRIP", "네트워크 오류: ${e.code()} ${e.message()}")
                } catch (e: Exception) {
                    Log.e("AI_TRIP", "요청 실패: ${e.message}")
                }
            }
        }
    }

    private fun setupBottomNav() {
        val bottom = binding.bottomNav
        bottom.selectedItemId = R.id.tab_country
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_country -> true
                R.id.tab_search -> {
                    startActivity(Intent(this, StartActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_profile -> {
                    startActivity(Intent(this, MyInfoActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }

    private fun extractJsonArray(text: String): String? {
        val rx = Regex("```json\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL)
        return rx.find(text)?.groupValues?.get(1)
    }

    private fun parseDaysFromResult(resultText: String): List<DayPlan>? {
        val json = extractJsonArray(resultText) ?: return null
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapter(Place::class.java, PlaceAdapter())
                .create()
            val type = object : TypeToken<List<DayPlan>>() {}.type
            gson.fromJson<List<DayPlan>>(json, type)
        } catch (e: Exception) {
            Log.e("AI_TRIP", "JSON 파싱 실패: ${e.message}")
            null
        }
    }

    private fun saveItineraryToHistory(
        country: String,
        startDate: String,   // "YYYY-MM-DD"
        endDate: String,     // "YYYY-MM-DD"
        days: List<DayPlan>, // 파싱된 결과
        onDone: (String) -> Unit = {}, // 저장된 문서ID 콜백
        onError: (Exception) -> Unit = {}
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return onError(IllegalStateException("로그인이 필요합니다."))

        // Firestore에 넣기 좋은 형태로 변환
        val daysMap = days.map { d ->
            mapOf(
                "day" to d.day,
                "title" to d.title,
                // Place 객체 → 문자열 배열로 저장(원하면 객체 배열 그대로 넣어도 OK)
                "places" to d.places.map { it.name }
            )
        }

        val doc = hashMapOf(
            "uid" to uid,
            "country" to country,
            "startDate" to startDate,
            "endDate" to endDate,
            "nights" to (/* (end-start) */ kotlin.runCatching {
                val s = java.time.LocalDate.parse(startDate)
                val e = java.time.LocalDate.parse(endDate)
                java.time.temporal.ChronoUnit.DAYS.between(s, e).toInt()
            }.getOrDefault(0)),
            "daysCount" to days.size,
            "days" to daysMap, // 👈 한 문서에 통째로 저장
            "createdAt" to FieldValue.serverTimestamp()
        )

        Firebase.firestore
            .collection("history")
            .document(uid)
            .collection("trips")
            .add(doc)
            .addOnSuccessListener { ref ->
                onDone(ref.id) // 저장된 trip 문서ID
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}

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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.hehe.travel.Retrofit.retrofit
import com.hehe.travel.databinding.ActivityDateRangeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
            val s = range.start; val e = range.end ?: range.start
            if (s == null || e == null) {
                Toast.makeText(this, "날짜 범위를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1) 프로필 로딩이 끝났는지 체크 (간단 예: gender가 비었으면 막기)
            if (gender.isNullOrBlank()) {
                Toast.makeText(this, "프로필을 불러오는 중입니다. 잠시만요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val a = minOf(s, e); val b = maxOf(s, e)
            val startStr = a.toString()
            val endStr = b.toString()

            val keywords = collectCheckedChips()    // <- 화면에서 체크한 칩들

            val api = retrofit.create(RecommendApi::class.java)
            val request = RecommendRequest(
                country = startUtc,
                startDate = startStr,
                endDate = endStr,
                gender = gender,
                age = age,
                // 여기!! 의도대로 수정
                preference = keywords  // 혹은 tags, 의도한 쪽으로
            )

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        api.getRecommendation(request)
                    }

                    if (response.isSuccessful) {
                        val body = response.body()

                        // (B) 문자열 응답 우선 처리
                        if (!body?.result.isNullOrBlank()) {
                            Log.d("AI_TRIP", "itinerary (text):\n${body?.result}")

                            // (A) days 구조
                        } else if (body?.success == true && !body.days.isNullOrEmpty()) {
                            body.days!!.forEach { day ->
                                Log.d("AI_TRIP", "${day.day}일차 ${day.title} / 장소=${day.places.size}")
                            }

                        } else {
                            Log.e("AI_TRIP", "서버 success=false or empty: success=${body?.success}, msg=${body?.message}")
                        }
                    } else {
                        Log.e("AI_TRIP", "HTTP 오류: ${response.code()} ${response.errorBody()?.string()}")
                    }


                    // 네트워크 처리가 끝난 뒤에 화면 전환하고 싶다면 여기에서!
                    val country = intent.getStringExtra("countryKey")
                    val go = Intent(this@DateRangeActivity, PlanActivity::class.java).apply {
                        putExtra("startUtcMillis", startStr)
                        putExtra("endUtcMillis", endStr)
                        putExtra("countryKey", country)
                        putExtra("country", startUtc)
                        putExtra("login", name)
                        putStringArrayListExtra("keywords", ArrayList(keywords))
                        // 필요하면 body 결과도 같이
                    }
                    startActivity(go)

                } catch (e: Exception) {
                    Log.e("AI_TRIP", "요청 실패: ${e.message}")
                }
            }
        }

    }

    private fun collectCheckedChips(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until binding.chipGroup.childCount) {
            val v = binding.chipGroup.getChildAt(i)
            if (v is Chip && v.isChecked) list += v.text.toString()
        }
        return list
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
}

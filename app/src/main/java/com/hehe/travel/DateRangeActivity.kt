package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.hehe.travel.databinding.ActivityDateRangeBinding
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateRangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar(binding.rvCalendar, binding.tvMonth)
        setupButtons()
        setupBottomNav()

        startUtc = intent.getStringExtra("country").toString()
        name = intent.getStringExtra("login").toString()

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
            val a = minOf(s, e); val b = maxOf(s, e)
            val startMillis = a.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = b.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val keywords = collectCheckedChips()

            val country = intent.getStringExtra("countryKey")
            val go = Intent(this, PlanActivity::class.java).apply {
                putExtra("startUtcMillis", startMillis)
                putExtra("endUtcMillis", endMillis)
                putExtra("countryKey", country)
                putExtra("country", startUtc)
                putExtra("login",name)
                putStringArrayListExtra("keywords", ArrayList(keywords))
            }
            startActivity(go)
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

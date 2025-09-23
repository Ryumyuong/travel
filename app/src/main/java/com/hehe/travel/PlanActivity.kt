package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hehe.travel.databinding.ActivityPlanBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.android.material.tabs.TabLayout
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// PlanActivity.kt
class PlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanBinding
    private var isSelectionMode = false // 선택 모드 상태
    private val selectedItems = mutableSetOf<Int>() // 선택된 아이템들
    private lateinit var adapter: PlanAdapter
    private lateinit var country: String
    private lateinit var name:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        setupRecyclerView()
        setupClickListeners()
        setupBottomNav()

        val startUtc = intent.getLongExtra("startUtcMillis", 0L)
        val endUtc = intent.getLongExtra("endUtcMillis", 0L)

        // 일정표시 테스트 코드
        // val startUtc = intent.getLongExtra("startUtcMillis", System.currentTimeMillis())
        // val endUtc = intent.getLongExtra("endUtcMillis", System.currentTimeMillis() + (4 * 24 * 60 * 60 * 1000L))

        country = intent.getStringExtra("country").toString()
        name = intent.getStringExtra("login").toString()
        binding.headcountry.text = country




        binding.name.text = "${name}님의"
        binding.country.text = country

        binding.btnPrev.setOnClickListener {
            val go = Intent(this, DateRangeActivity::class.java).apply {
                putExtra("country", country)
                putExtra("login", name)
            }
            startActivity(go)
            finish()
        }


        val zone = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(startUtc).atZone(zone).toLocalDate()
        val endDate   = Instant.ofEpochMilli(endUtc).atZone(zone).toLocalDate()

        val fmt = DateTimeFormatter.ofPattern("yyyy.M.d ", Locale.KOREAN)
        val startStr = startDate.format(fmt)
        val endStr   = endDate.format(fmt)

        setupDayTabs(startDate,endDate)

        binding.date.text = if (startDate == endDate) startStr else " 일정 $startStr - $endStr"
    }

    private fun setupRecyclerView() {
        adapter = PlanAdapter(
            isSelectionMode = isSelectionMode,
            selectedItems = selectedItems,
            onItemClick = { position ->
                if (isSelectionMode) {
                    toggleSelection(position)
                } else {
                    // 일반 클릭 처리
                    handleNormalClick(position)
                }
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        // "내 일정 추가" 버튼
        binding.btnAddToSchedule.setOnClickListener {
            toggleSelectionMode()
        }

//        // "선택완료" 버튼 (선택 모드일 때만 보임)
//        binding.btnComplete.setOnClickListener {
//            completeSelection()
//        }
//
//        // "선택취소" 버튼
//        binding.btnCancel.setOnClickListener {
//            cancelSelection()
//        }
    }

    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        selectedItems.clear()

        // UI 업데이트
        updateUI()
        adapter.updateSelectionMode(isSelectionMode, selectedItems)
    }

    private fun updateUI() {
        if (isSelectionMode) {
            // 선택 모드 UI
            binding.preButton.visibility = View.GONE
            binding.layoutSelectionButtons.visibility = View.VISIBLE
        } else {
            // 일반 모드 UI
            binding.preButton.visibility = View.VISIBLE
            binding.layoutSelectionButtons.visibility = View.GONE
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        adapter.notifyItemChanged(position)
    }

    private fun completeSelection() {
        // 선택된 항목들을 내 일정에 추가하는 로직
        if (selectedItems.isNotEmpty()) {
            // TODO: 선택된 항목들을 처리
            Toast.makeText(this, "${selectedItems.size}개 항목이 일정에 추가되었습니다", Toast.LENGTH_SHORT).show()
            val country = intent.getStringExtra("countryKey")
            val go = Intent(this, SearchActivity::class.java).apply {
                putExtra("login",name)
            }
            startActivity(go)
        }
        cancelSelection()
    }

    private fun cancelSelection() {
        isSelectionMode = false
        selectedItems.clear()
        updateUI()
        adapter.updateSelectionMode(isSelectionMode, selectedItems)
    }

    private fun handleNormalClick(position: Int) {
        // 일반 모드에서의 클릭 처리 (상세 페이지 이동 등)
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

    private fun setupDayTabs(startDate: LocalDate, endDate: LocalDate) {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val tabs = binding.tabDays

        tabs.removeAllTabs()
        repeat(days) { i ->
            tabs.addTab(tabs.newTab().setText("${i + 1}일차"))
        }

        // 처음 탭 선택
        if (days > 0) tabs.getTabAt(0)?.select()

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val dayIndex = tab.position // 0-based (0=1일차)
                // TODO: dayIndex 기준으로 리사이클러뷰 데이터 갱신
                // e.g., viewModel.loadDay(dayIndex)
                // adapter.submitList(itemsFor(dayIndex))
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 하루만 선택된 경우 탭바 숨기고 싶다면:
        binding.tabDays.visibility = if (days <= 1) View.GONE else View.VISIBLE

        // 탭바 항상 표시 (디버깅용)
//         binding.tabDays.visibility = View.VISIBLE
    }
}

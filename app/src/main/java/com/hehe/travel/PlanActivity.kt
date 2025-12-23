package com.hehe.travel

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivityPlanBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanBinding
    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<Int>()
    private lateinit var adapter: PlanAdapter

    private val client = OkHttpClient()

    private lateinit var country: String
    private lateinit var userName: String
    private var travelStyle: String = ""
    private var keywords: List<String> = emptyList()
    private var nights: Int = 0
    private var startDate: String = ""
    private var endDate: String = ""
    private val GEMINI_API_KEY = BuildConfig.API_KEY

    // AI가 생성한 일정 데이터
    private var allDaysData: MutableList<DayPlanData> = mutableListOf()
    private var currentDayIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 받기
        country = intent.getStringExtra("country") ?: ""
        userName = intent.getStringExtra("login") ?: ""
        travelStyle = intent.getStringExtra("travelStyle") ?: ""
        keywords = intent.getStringArrayListExtra("keywords") ?: emptyList()
        nights = intent.getIntExtra("nights", 0)
        startDate = intent.getStringExtra("startDate") ?: ""
        endDate = intent.getStringExtra("endDate") ?: ""

        val itineraryJson = intent.getStringExtra("itineraryJson") ?: ""

        // JSON 파싱
        parseItineraryJson(itineraryJson)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        setupHeader()
        setupDayTabs()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNav()

        // 첫 번째 탭 데이터 로드
        if (allDaysData.isNotEmpty()) {
            loadDayData(0)
        }
    }

    // JSON 파싱
    private fun parseItineraryJson(jsonString: String) {
        if (jsonString.isEmpty()) {
            Log.e("PlanActivity", "빈 JSON")
            return
        }

        try {
            val daysArray = JSONArray(jsonString)
            allDaysData.clear()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val day = dayObj.getInt("day")
                val title = dayObj.getString("title")
                val placesArray = dayObj.getJSONArray("places")

                val places = mutableListOf<PlaceData>()
                for (j in 0 until placesArray.length()) {
                    val placeObj = placesArray.getJSONObject(j)
                    places.add(PlaceData(
                        name = placeObj.getString("name"),
                        description = placeObj.optString("description", ""),
                        time = placeObj.optString("time", ""),
                        duration = placeObj.optString("duration", ""),
                        tip = placeObj.optString("tip", "")
                    ))
                }

                allDaysData.add(DayPlanData(day, title, places.toMutableList()))
            }

            nights = allDaysData.size - 1
            Log.d("PlanActivity", "파싱 완료: ${allDaysData.size}일 일정")

        } catch (e: Exception) {
            Log.e("PlanActivity", "JSON 파싱 실패: ${e.message}")
        }
    }

    // 헤더 설정
    private fun setupHeader() {
//        binding.headcountry.text = country
        binding.name.text = " ${userName}님의\n"
        binding.country.text = country

        updateDateDisplay()
    }

    // 날짜 표시 업데이트
    private fun updateDateDisplay() {
        val days = allDaysData.size
        val nightsText = if (days > 1) "${days - 1}박 ${days}일" else "당일치기"

        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            binding.date.text = " 일정 $startDate ~ $endDate ($nightsText)"
        } else {
            binding.date.text = " $nightsText 여행"
        }
    }

    // 일차별 탭 설정
    private fun setupDayTabs() {
        val tabs = binding.tabDays
        tabs.removeAllTabs()

        allDaysData.forEachIndexed { index, _ ->
            tabs.addTab(tabs.newTab().setText("${index + 1}일차"))
        }

        if (allDaysData.isNotEmpty()) {
            tabs.getTabAt(0)?.select()
        }

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentDayIndex = tab.position
                loadDayData(currentDayIndex)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.tabDays.visibility = if (allDaysData.size <= 1) View.GONE else View.VISIBLE
    }

    // 특정 일차 데이터 로드
    private fun loadDayData(dayIndex: Int) {
        if (dayIndex < 0 || dayIndex >= allDaysData.size) return

        val dayData = allDaysData[dayIndex]

        val planItems = dayData.places.map { place ->
            PlanItem(
                title = place.name,
                description = place.description,
                time = place.time,
                duration = place.duration,
                tip = place.tip
            )
        }

        adapter.updateItems(planItems)
        Log.d("PlanActivity", "${dayIndex + 1}일차 로드: ${planItems.size}개 장소")
    }

    private fun setupRecyclerView() {
        adapter = PlanAdapter(
            isSelectionMode = isSelectionMode,
            selectedItems = selectedItems,
            onItemClick = { position ->
                if (isSelectionMode) {
                    toggleSelection(position)
                } else {
                    handleNormalClick(position)
                }
            },
            onRecommendClick = { position ->
                // 다시추천 클릭
                showRecommendDialog(position)
            },
            onDeleteClick = { position ->
                // 삭제 클릭
                showDeleteDialog(position)
            }
        )
        binding.recyclerView.adapter = adapter
    }

    // 다시추천 다이얼로그
    private fun showRecommendDialog(position: Int) {
        if (currentDayIndex >= allDaysData.size) return
        val currentPlace = allDaysData[currentDayIndex].places.getOrNull(position) ?: return

        AlertDialog.Builder(this)
            .setTitle("다시 추천")
            .setMessage("'${currentPlace.name}'을(를) 다른 장소로 변경할까요?")
            .setPositiveButton("변경") { _, _ ->
                recommendNewPlace(position, currentPlace)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // Gemini AI로 새로운 장소 추천
    private fun recommendNewPlace(position: Int, currentPlace: PlaceData) {
        Toast.makeText(this, "새로운 장소를 찾는 중...", Toast.LENGTH_SHORT).show()

        val dayData = allDaysData[currentDayIndex]
        val existingPlaces = dayData.places.map { it.name }.joinToString(", ")

        val prompt = """
당신은 전문 여행 플래너입니다. 

[요청]
${country}의 ${currentDayIndex + 1}일차 일정에서 "${currentPlace.name}"을 대체할 새로운 장소 1개를 추천해주세요.

[조건]
- 기존 장소들: $existingPlaces (중복 제외)
- 사용자 선호: ${keywords.joinToString(", ")}
- 비슷한 시간대에 방문할 수 있는 장소
- 실제 존재하는 장소만 추천

[응답 형식]
다음 JSON 형식으로만 응답해주세요:
```json
{
    "name": "장소명",
    "description": "장소 설명 (20자 내외)",
    "time": "${currentPlace.time}",
    "duration": "예상 소요시간",
    "tip": "꿀팁"
}
```
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 1024)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$GEMINI_API_KEY")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@PlanActivity, "추천 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && responseBody != null) {
                        parseAndReplacePlace(position, responseBody)
                    } else {
                        Toast.makeText(this@PlanActivity, "추천 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // 응답 파싱 및 장소 교체
    private fun parseAndReplacePlace(position: Int, responseBody: String) {
        try {
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val placeObj = JSONObject(cleanJson)
            val newPlace = PlaceData(
                name = placeObj.getString("name"),
                description = placeObj.optString("description", ""),
                time = placeObj.optString("time", ""),
                duration = placeObj.optString("duration", ""),
                tip = placeObj.optString("tip", "")
            )

            // 데이터 업데이트
            allDaysData[currentDayIndex].places[position] = newPlace

            // UI 업데이트
            val newItem = PlanItem(
                title = newPlace.name,
                description = newPlace.description,
                time = newPlace.time,
                duration = newPlace.duration,
                tip = newPlace.tip
            )
            adapter.replaceItem(position, newItem)

            Toast.makeText(this, "'${newPlace.name}'으로 변경되었습니다!", Toast.LENGTH_SHORT).show()

            // Firebase 업데이트 (선택적)
            updateFirebaseHistory()

        } catch (e: Exception) {
            Log.e("PlanActivity", "파싱 실패: ${e.message}")
            Toast.makeText(this, "추천 결과 처리 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 삭제 다이얼로그
    private fun showDeleteDialog(position: Int) {
        if (currentDayIndex >= allDaysData.size) return
        val currentPlace = allDaysData[currentDayIndex].places.getOrNull(position) ?: return

        val dayPlaces = allDaysData[currentDayIndex].places
        val isLastPlaceInDay = dayPlaces.size == 1
        val totalDays = allDaysData.size

        val message = if (isLastPlaceInDay && totalDays > 1) {
            "'${currentPlace.name}'을(를) 삭제하면 ${currentDayIndex + 1}일차 전체가 삭제됩니다.\n\n${totalDays}박${totalDays}일 → ${totalDays - 1}박${totalDays - 1}일로 변경됩니다.\n\n삭제할까요?"
        } else if (isLastPlaceInDay && totalDays == 1) {
            "'${currentPlace.name}'을(를) 삭제하면 일정이 비어있게 됩니다.\n\n삭제할까요?"
        } else {
            "'${currentPlace.name}'을(를) 삭제할까요?"
        }

        AlertDialog.Builder(this)
            .setTitle("일정 삭제")
            .setMessage(message)
            .setPositiveButton("삭제") { _, _ ->
                deletePlace(position)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 장소 삭제
    private fun deletePlace(position: Int) {
        if (currentDayIndex >= allDaysData.size) return

        val dayPlaces = allDaysData[currentDayIndex].places

        if (dayPlaces.size == 1) {
            // 마지막 장소 삭제 → 일차 전체 삭제
            deleteDayAndReorder(currentDayIndex)
        } else {
            // 장소만 삭제
            dayPlaces.removeAt(position)
            adapter.removeItem(position)
            Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // Firebase 업데이트
        updateFirebaseHistory()
    }

    // 일차 삭제 및 번호 재정렬
    private fun deleteDayAndReorder(dayIndex: Int) {
        if (allDaysData.size <= 1) {
            // 마지막 일차면 일정 비우기
            allDaysData[dayIndex].places.clear()
            adapter.updateItems(emptyList())
            Toast.makeText(this, "모든 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 일차 삭제
        allDaysData.removeAt(dayIndex)

        // 일차 번호 재정렬
        allDaysData.forEachIndexed { index, dayData ->
            // day 필드 업데이트 (DayPlanData가 data class면 copy 사용)
            allDaysData[index] = dayData.copy(day = index + 1)
        }

        // nights 업데이트
        nights = allDaysData.size - 1

        // 탭 재설정
        setupDayTabs()

        // 현재 탭 인덱스 조정
        currentDayIndex = if (dayIndex >= allDaysData.size) allDaysData.size - 1 else dayIndex

        // 데이터 로드
        if (allDaysData.isNotEmpty()) {
            binding.tabDays.getTabAt(currentDayIndex)?.select()
            loadDayData(currentDayIndex)
        }

        // 날짜 표시 업데이트
        updateDateDisplay()

        Toast.makeText(this, "일차가 삭제되고 일정이 재정렬되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // Firebase history 업데이트
    private fun updateFirebaseHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val daysMap = allDaysData.map { d ->
            mapOf(
                "day" to d.day,
                "title" to d.title,
                "places" to d.places.map { p ->
                    mapOf(
                        "name" to p.name,
                        "description" to p.description,
                        "time" to p.time,
                        "duration" to p.duration,
                        "tip" to p.tip
                    )
                }
            )
        }

        // 가장 최근 히스토리 업데이트 (또는 특정 문서 ID로 업데이트)
        Firebase.firestore
            .collection("history")
            .document(uid)
            .collection("trips")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents[0].id
                    Firebase.firestore
                        .collection("history")
                        .document(uid)
                        .collection("trips")
                        .document(docId)
                        .update(
                            mapOf(
                                "days" to daysMap,
                                "daysCount" to allDaysData.size,
                                "nights" to nights
                            )
                        )
                        .addOnSuccessListener {
                            Log.d("PlanActivity", "Firebase 업데이트 완료")
                        }
                }
            }
    }

    private fun setupClickListeners() {
        binding.btnPrev.setOnClickListener {
            finish()
        }

        // 내 일정에 추가 버튼
        binding.btnAddToSchedule.setOnClickListener {
            saveToPlanHistory()
        }

        binding.btnComplete?.setOnClickListener {
            completeSelection()
        }

        binding.btnCancel?.setOnClickListener {
            cancelSelection()
        }
    }

    // planhistory 컬렉션에 저장
    private fun saveToPlanHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (allDaysData.isEmpty()) {
            Toast.makeText(this, "저장할 일정이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 버튼 비활성화 (중복 클릭 방지)
        binding.btnAddToSchedule.isEnabled = false
        binding.btnAddToSchedule.text = "저장 중..."

        val daysMap = allDaysData.map { d ->
            mapOf(
                "day" to d.day,
                "title" to d.title,
                "places" to d.places.map { p ->
                    mapOf(
                        "name" to p.name,
                        "description" to p.description,
                        "time" to p.time,
                        "duration" to p.duration,
                        "tip" to p.tip
                    )
                }
            )
        }

        val planData = hashMapOf(
            "uid" to uid,
            "nickname" to userName,
            "country" to country,
            "startDate" to startDate,
            "endDate" to endDate,
            "nights" to nights,
            "daysCount" to allDaysData.size,
            "days" to daysMap,
            "travelStyle" to travelStyle,
            "keywords" to keywords,
            "savedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        Firebase.firestore
            .collection("planhistory")
            .add(planData)
            .addOnSuccessListener { docRef ->
                Log.d("PlanActivity", "내 일정 저장 완료: ${docRef.id}")
                Toast.makeText(this, "내 일정에 저장되었습니다! ✅", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
                finish()


                // 버튼 상태 변경
                binding.btnAddToSchedule.text = "저장 완료 ✅"
                binding.btnAddToSchedule.isEnabled = false
            }
            .addOnFailureListener { e ->
                Log.e("PlanActivity", "내 일정 저장 실패: ${e.message}")
                Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()

                // 버튼 복원
                binding.btnAddToSchedule.isEnabled = true
                binding.btnAddToSchedule.text = "내 일정에 추가"
            }
    }

    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        selectedItems.clear()
        updateUI()
        adapter.updateSelectionMode(isSelectionMode, selectedItems)
    }

    private fun updateUI() {
        if (isSelectionMode) {
            binding.preButton.visibility = View.GONE
            binding.layoutSelectionButtons.visibility = View.VISIBLE
        } else {
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
        if (selectedItems.isNotEmpty()) {
            Toast.makeText(this, "${selectedItems.size}개 항목이 일정에 추가되었습니다", Toast.LENGTH_SHORT).show()
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
        if (currentDayIndex < allDaysData.size) {
            val place = allDaysData[currentDayIndex].places.getOrNull(position)
            place?.let {
                Toast.makeText(this, "${it.name}\n${it.tip}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNav() {
        val bottom = binding.bottomNav
        bottom.selectedItemId = R.id.tab_search
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_country -> true
                R.id.tab_search -> {
                    startActivity(Intent(this, QuestionnaireActivity::class.java)
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

// 데이터 클래스는 Models.kt에서 가져옴
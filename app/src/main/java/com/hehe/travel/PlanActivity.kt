package com.hehe.travel

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import java.util.concurrent.TimeUnit

class PlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanBinding
    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<Int>()
    private lateinit var adapter: PlanAdapter

    // 타임아웃 60초 설정
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

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

    // 비회원 여부
    private var isGuest = false

    // 탭 변경 중 플래그 (스크롤 리스너와 탭 클릭 충돌 방지)
    private var isTabChanging = false

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

        // 비회원 여부 확인
        isGuest = intent.getBooleanExtra("isGuest", false) || FirebaseAuth.getInstance().currentUser == null

        val itineraryJson = intent.getStringExtra("itineraryJson") ?: ""

        // JSON 파싱
        parseItineraryJson(itineraryJson)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        setupHeader()
        setupDayTabs()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNav()

        // 이미지 프리로딩 후 화면 표시
        preloadImagesAndShow()
    }

    // 이미지 미리 로드 후 화면 표시
    private fun preloadImagesAndShow() {
        if (allDaysData.isEmpty()) {
            // 데이터가 없으면 바로 표시
            binding.loadingOverlay.visibility = View.GONE
            return
        }

        // 로딩 오버레이 표시
        binding.loadingOverlay.visibility = View.VISIBLE
        val firstDayCount = allDaysData.firstOrNull()?.places?.size ?: 0
        binding.tvLoadingProgress.text = "0 / $firstDayCount"

        // 이미지 프리로드 (1일차 우선 + 병렬 로딩)
        PlacesPhotoHelper.preloadWithPriority(
            context = this,
            allDays = allDaysData,
            country = country,
            onFirstDayComplete = {
                // 1일차 로딩 완료 - 화면 표시
                binding.loadingOverlay.visibility = View.GONE

                // 모든 일차 데이터를 어댑터에 로드 (연속 스크롤)
                if (allDaysData.isNotEmpty()) {
                    adapter.updateAllDays(allDaysData)
                }
            },
            onAllComplete = {
                // 나머지 일차도 완료 (백그라운드에서 조용히 완료)
                Log.d("PlanActivity", "모든 이미지 로드 완료")
            },
            onProgress = { current, total ->
                // 1일차 로딩 중일 때만 프로그레스 표시
                if (current <= firstDayCount) {
                    binding.tvLoadingProgress.text = "$current / $firstDayCount"
                }
            }
        )
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
        binding.name.text = " ${userName}님의"
        updateDateDisplay()

        // 나라 배경 이미지 로드
        if (country.isNotEmpty()) {
            PlacesPhotoHelper.loadCountryPhoto(country, binding.ivHeaderBackground)
        }
    }

    // 날짜 표시 업데이트
    private fun updateDateDisplay() {
        val days = allDaysData.size
        val nightsText = if (days > 1) "${days - 1}박 ${days}일" else "당일치기"

        binding.night.text = "$nightsText"
        // 상단: "4박 5일 발리 여행은 이렇게 준비했어요!"
        binding.country.text = " $country"

        // 하단: "일정 2025.08.01 - 2025.08.05"
        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            binding.date.text = "$startDate - $endDate"
        } else {
            binding.date.text = ""
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
                if (isTabChanging) return  // 스크롤로 인한 탭 변경 시 무시

                currentDayIndex = tab.position
                // 해당 일차 위치로 스크롤
                val targetPosition = adapter.getDayStartPosition(currentDayIndex)
                isTabChanging = true
                (binding.recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(targetPosition, 0)
                // 스크롤 완료 후 플래그 해제
                binding.recyclerView.post {
                    isTabChanging = false
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.tabDays.visibility = if (allDaysData.size <= 1) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = PlanAdapter(
            isSelectionMode = isSelectionMode,
            selectedItems = selectedItems,
            onItemClick = { position ->
                if (isSelectionMode) {
                    toggleSelection(position)
                }
            },
            onRecommendClick = { dayIndex, placeIndex ->
                // 다시추천 클릭
                showRecommendDialog(dayIndex, placeIndex)
            },
            onDeleteClick = { dayIndex, placeIndex ->
                // 삭제 클릭
                showDeleteDialog(dayIndex, placeIndex)
            },
            country = country
        )
        binding.recyclerView.adapter = adapter

        // 스크롤 리스너 추가 - 현재 보이는 일차에 따라 탭 업데이트
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isTabChanging) return  // 탭 클릭으로 인한 스크롤 중에는 무시

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                    val dayIndex = adapter.getDayIndexForPosition(firstVisiblePosition)
                    if (dayIndex != currentDayIndex) {
                        currentDayIndex = dayIndex
                        // 탭 업데이트 (리스너 트리거 방지)
                        isTabChanging = true
                        binding.tabDays.getTabAt(dayIndex)?.select()
                        isTabChanging = false
                    }
                }
            }
        })
    }

    // 다시추천 다이얼로그
    private fun showRecommendDialog(dayIndex: Int, placeIndex: Int) {
        if (dayIndex >= allDaysData.size) return
        val currentPlace = allDaysData[dayIndex].places.getOrNull(placeIndex) ?: return

        AlertDialog.Builder(this)
            .setTitle("다시 추천")
            .setMessage("'${currentPlace.name}'을(를) 다른 장소로 변경할까요?")
            .setPositiveButton("변경") { _, _ ->
                recommendNewPlace(dayIndex, placeIndex, currentPlace)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // Gemini AI로 새로운 장소 추천 (재시도 로직 포함)
    private fun recommendNewPlace(dayIndex: Int, placeIndex: Int, currentPlace: PlaceData, retryCount: Int = 0) {
        val maxRetries = 3
        val retryDelays = listOf(10000L, 20000L, 40000L) // 10초, 20초, 40초

        if (retryCount == 0) {
            Toast.makeText(this, "새로운 장소를 찾는 중...", Toast.LENGTH_SHORT).show()
        }

        val dayData = allDaysData[dayIndex]
        val existingPlaces = dayData.places.map { it.name }.joinToString(", ")

        val prompt = """
${country} ${dayIndex + 1}일차에서 "${currentPlace.name}" 대체 장소 1개.
기존: $existingPlaces (중복X)

JSON만 응답:
{"name":"장소명","description":"15자 설명","time":"${currentPlace.time}","duration":"2시간","tip":"팁"}
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
                put("maxOutputTokens", 4096)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY")
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
                Log.d("PlanActivity", "다시추천 응답: ${response.code} - $responseBody")

                runOnUiThread {
                    when {
                        response.isSuccessful && responseBody != null -> {
                            parseAndReplacePlace(dayIndex, placeIndex, responseBody)
                        }
                        response.code == 429 && retryCount < maxRetries -> {
                            // 429 에러: 재시도
                            val delay = retryDelays[retryCount]
                            val seconds = delay / 1000
                            Toast.makeText(
                                this@PlanActivity,
                                "서버 요청 한도 초과. ${seconds}초 후 재시도합니다... (${retryCount + 1}/$maxRetries)",
                                Toast.LENGTH_LONG
                            ).show()

                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                recommendNewPlace(dayIndex, placeIndex, currentPlace, retryCount + 1)
                            }, delay)
                        }
                        response.code == 429 -> {
                            Toast.makeText(
                                this@PlanActivity,
                                "서버 요청 한도 초과. 잠시 후 다시 시도해주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(this@PlanActivity, "추천 실패 (${response.code})", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    // 응답 파싱 및 장소 교체
    private fun parseAndReplacePlace(dayIndex: Int, placeIndex: Int, responseBody: String) {
        try {
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            Log.d("PlanActivity", "AI 원본 응답: $content")

            // JSON 추출
            val cleanJson = extractJson(content)
            Log.d("PlanActivity", "추출된 JSON: $cleanJson")

            val placeObj = JSONObject(cleanJson)
            val newPlace = PlaceData(
                name = placeObj.getString("name"),
                description = placeObj.optString("description", ""),
                time = placeObj.optString("time", ""),
                duration = placeObj.optString("duration", ""),
                tip = placeObj.optString("tip", "")
            )

            // 데이터 업데이트
            allDaysData[dayIndex].places[placeIndex] = newPlace

            // UI 업데이트
            adapter.replacePlace(dayIndex, placeIndex, newPlace)

            Toast.makeText(this, "'${newPlace.name}'으로 변경되었습니다!", Toast.LENGTH_SHORT).show()

            // Firebase 업데이트 (선택적)
            updateFirebaseHistory()

        } catch (e: Exception) {
            Log.e("PlanActivity", "파싱 실패: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "추천 결과 처리 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON 추출 헬퍼 함수
    private fun extractJson(text: String): String {
        var cleaned = text.trim()

        // 1. ```json 또는 ``` 제거
        cleaned = cleaned
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // 2. { 로 시작하는 부분 찾기
        val startIndex = cleaned.indexOf("{")
        if (startIndex == -1) return cleaned

        cleaned = cleaned.substring(startIndex)

        // 3. 마지막 } 찾기 (불완전한 JSON 처리)
        val endIndex = cleaned.lastIndexOf("}")
        if (endIndex != -1) {
            return cleaned.substring(0, endIndex + 1)
        }

        // 4. } 가 없으면 불완전한 JSON - 강제로 닫기 시도
        // 필요한 필드만 추출
        return tryCompleteJson(cleaned)
    }

    // 불완전한 JSON 복구 시도
    private fun tryCompleteJson(incompleteJson: String): String {
        try {
            // name 필드 추출
            val nameRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val nameMatch = nameRegex.find(incompleteJson)
            val name = nameMatch?.groupValues?.get(1) ?: "추천 장소"

            // description 필드 추출
            val descRegex = "\"description\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val descMatch = descRegex.find(incompleteJson)
            val description = descMatch?.groupValues?.get(1) ?: ""

            // time 필드 추출
            val timeRegex = "\"time\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val timeMatch = timeRegex.find(incompleteJson)
            val time = timeMatch?.groupValues?.get(1) ?: ""

            // 완전한 JSON 생성
            return """{"name":"$name","description":"$description","time":"$time","duration":"2시간","tip":""}"""
        } catch (e: Exception) {
            return incompleteJson
        }
    }

    // 삭제 다이얼로그
    private fun showDeleteDialog(dayIndex: Int, placeIndex: Int) {
        if (dayIndex >= allDaysData.size) return
        val currentPlace = allDaysData[dayIndex].places.getOrNull(placeIndex) ?: return

        val dayPlaces = allDaysData[dayIndex].places
        val isLastPlaceInDay = dayPlaces.size == 1
        val totalDays = allDaysData.size

        val message = if (isLastPlaceInDay && totalDays > 1) {
            "'${currentPlace.name}'을(를) 삭제하면 ${dayIndex + 1}일차 전체가 삭제됩니다.\n\n${totalDays}박${totalDays}일 → ${totalDays - 1}박${totalDays - 1}일로 변경됩니다.\n\n삭제할까요?"
        } else if (isLastPlaceInDay && totalDays == 1) {
            "'${currentPlace.name}'을(를) 삭제하면 일정이 비어있게 됩니다.\n\n삭제할까요?"
        } else {
            "'${currentPlace.name}'을(를) 삭제할까요?"
        }

        AlertDialog.Builder(this)
            .setTitle("일정 삭제")
            .setMessage(message)
            .setPositiveButton("삭제") { _, _ ->
                deletePlace(dayIndex, placeIndex)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 장소 삭제
    private fun deletePlace(dayIndex: Int, placeIndex: Int) {
        if (dayIndex >= allDaysData.size) return

        val dayPlaces = allDaysData[dayIndex].places

        if (dayPlaces.size == 1) {
            // 마지막 장소 삭제 → 일차 전체 삭제
            deleteDayAndReorder(dayIndex)
        } else {
            // 장소만 삭제
            dayPlaces.removeAt(placeIndex)
            // 어댑터 전체 업데이트
            adapter.updateAllDays(allDaysData)
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
            adapter.updateAllDays(allDaysData)
            Toast.makeText(this, "모든 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 일차 삭제
        allDaysData.removeAt(dayIndex)

        // 일차 번호 재정렬
        allDaysData.forEachIndexed { index, dayData ->
            allDaysData[index] = dayData.copy(day = index + 1)
        }

        // nights 업데이트
        nights = allDaysData.size - 1

        // 탭 재설정
        setupDayTabs()

        // 현재 탭 인덱스 조정
        currentDayIndex = if (dayIndex >= allDaysData.size) allDaysData.size - 1 else dayIndex

        // 어댑터 전체 업데이트
        adapter.updateAllDays(allDaysData)

        // 탭 선택
        if (allDaysData.isNotEmpty()) {
            isTabChanging = true
            binding.tabDays.getTabAt(currentDayIndex)?.select()
            isTabChanging = false
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

        // 비회원인 경우 로그인 화면으로 이동
        if (uid == null || isGuest) {
            Toast.makeText(this, "일정을 저장하려면 로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("fromGuestFlow", true)
            startActivity(intent)
            return
        }

        if (allDaysData.isEmpty()) {
            Toast.makeText(this, "저장할 일정이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 버튼 비활성화 (중복 클릭 방지)
        binding.btnAddToSchedule.isEnabled = false
        binding.btnAddToSchedule.text = "저장 중..."

        // profiles에서 hasSemiPass 값 가져온 후 저장
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { profileDoc ->
                val hasSemiPass = profileDoc.getBoolean("hasSemiPass") ?: false

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
                    "hasSemiPass" to hasSemiPass,
                    "savedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                savePlanDataToFirestore(planData)
            }
            .addOnFailureListener { e ->
                Log.e("PlanActivity", "프로필 조회 실패: ${e.message}")
                // 프로필 조회 실패 시 hasSemiPass = false로 저장
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
                    "hasSemiPass" to false,
                    "savedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                savePlanDataToFirestore(planData)
            }
    }

    // Firestore에 planData 저장
    private fun savePlanDataToFirestore(planData: HashMap<String, Any>) {
        Firebase.firestore
            .collection("planhistory")
            .add(planData)
            .addOnSuccessListener { docRef ->
                Log.d("PlanActivity", "내 일정 저장 완료: ${docRef.id}")
                Toast.makeText(this, "내 일정에 저장되었습니다! ✅", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainContainerActivity::class.java)
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
                R.id.tab_country -> {
                    startActivity(Intent(this, MainContainerActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_search -> {
                    startActivity(Intent(this, MainContainerActivity::class.java)
                        .putExtra("initialTab", R.id.tab_search)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.tab_profile -> {
                    // 비회원인 경우 로그인 유도
                    if (isGuest) {
                        Toast.makeText(this, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("fromGuestFlow", true)
                        startActivity(intent)
                        return@setOnItemSelectedListener false
                    }
                    startActivity(Intent(this, MainContainerActivity::class.java)
                        .putExtra("initialTab", R.id.tab_profile)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                else -> false
            }
        }
    }
}

// 데이터 클래스는 Models.kt에서 가져옴
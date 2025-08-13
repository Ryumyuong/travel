package com.hehe.travel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ItineraryActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var titleText: TextView
    private lateinit var itineraryRecyclerView: RecyclerView
    private lateinit var itineraryAdapter: ItineraryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_itinerary)

        country = intent.getStringExtra("country") ?: "italy"

        initViews()
        loadItinerary()
    }

    private fun initViews() {
        titleText = findViewById(R.id.tv_itinerary_title)
        itineraryRecyclerView = findViewById(R.id.rv_itinerary)

        titleText.text = "2박 3일 럭셔리 시간대별 일정"

        itineraryAdapter = ItineraryAdapter()
        itineraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ItineraryActivity)
            adapter = itineraryAdapter
        }
    }

    private fun loadItinerary() {
        val itinerary = when (country) {
            "italy" -> getItalyItinerary()
            else -> emptyList()
        }
        itineraryAdapter.submitList(itinerary)
    }

    private fun getItalyItinerary(): List<ItineraryDay> {
        return listOf(
            ItineraryDay(
                day = 1,
                title = "첫째 날 - 도착과 첫 만남",
                activities = listOf(
                    Activity(
                        time = "09:00",
                        title = "전용 요트 탑승 & 아말피 해안 크루즈",
                        description = "포지타노 항구에서 시작하는 럭셔리 크루즈",
                        location = "포지타노 항구"
                    ),
                    Activity(
                        time = "12:00",
                        title = "고급 해산물 점심 식사",
                        description = "미슐랭 가이드 추천 레스토랑",
                        location = "Ristorante Max"
                    ),
                    Activity(
                        time = "15:00",
                        title = "포지타노 마을 산책 & 기념품 쇼핑",
                        description = "아름다운 해안 마을 탐방",
                        location = "포지타노 마을 중심가"
                    ),
                    Activity(
                        time = "18:00",
                        title = "전통 이탈리아 요리 저녁 식사",
                        description = "현지 특선 요리 체험",
                        location = "La Sponda"
                    ),
                    Activity(
                        time = "21:00",
                        title = "숙소 휴식 & 야경 감상",
                        description = "지중해 야경을 감상하며 휴식",
                        location = "Hotel Le Sirenuse"
                    )
                )
            ),
            ItineraryDay(
                day = 2,
                title = "둘째 날 - 문화와 역사",
                activities = listOf(
                    Activity(
                        time = "08:00",
                        title = "호텔 조식",
                        description = "전용 테라스에서 아침 식사",
                        location = "호텔"
                    ),
                    Activity(
                        time = "10:00",
                        title = "폼페이 유적지 가이드 투어",
                        description = "전문 가이드와 함께하는 역사 탐방",
                        location = "폼페이"
                    )
                )
            ),
            ItineraryDay(
                day = 3,
                title = "셋째 날 - 마지막 순간들",
                activities = listOf(
                    Activity(
                        time = "09:00",
                        title = "카프리섬 당일 여행",
                        description = "청의 동굴과 쇼핑",
                        location = "카프리섬"
                    )
                )
            )
        )
    }
}
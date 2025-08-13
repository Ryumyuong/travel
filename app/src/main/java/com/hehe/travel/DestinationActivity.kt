package com.hehe.travel

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DestinationActivity : AppCompatActivity() {

    private lateinit var country: String
    private lateinit var titleText: TextView
    private lateinit var destinationRecyclerView: RecyclerView
    private lateinit var destinationAdapter: DestinationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_destination)

        country = intent.getStringExtra("country") ?: "italy"

        initViews()
        loadDestinations()
    }

    private fun initViews() {
        titleText = findViewById(R.id.tv_destination_title)
        destinationRecyclerView = findViewById(R.id.rv_destinations)

        titleText.text = "추천 관광지"

        destinationAdapter = DestinationAdapter()
        destinationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DestinationActivity)
            adapter = destinationAdapter
        }
    }

    private fun loadDestinations() {
        val destinations = when (country) {
            "italy" -> getItalyDestinations()
            else -> emptyList()
        }
        destinationAdapter.submitList(destinations)
    }

    private fun getItalyDestinations(): List<Destination> {
        return listOf(
            Destination(
                name = "포지타노 (Positano)",
                description = "아말피 해안의 아름다운 마을",
                category = "관광지"
            ),
            Destination(
                name = "폼페이 (Pompeii)",
                description = "고대 로마 유적지",
                category = "관광지"
            ),
            Destination(
                name = "카프리섬 (Capri)",
                description = "아름다운 경치와 고급 부티크로 유명한 섬",
                category = "관광지"
            ),
            Destination(
                name = "나폴리 (Naples)",
                description = "활기찬 도시와 맛있는 음식으로 유명",
                category = "관광지"
            )
        )
    }
}
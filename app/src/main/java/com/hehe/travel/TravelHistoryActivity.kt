package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivityTravelHistoryBinding
import java.util.*

class TravelHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTravelHistoryBinding
    private lateinit var auth: FirebaseAuth
    private val tripList = mutableListOf<TripHistoryItem>()
    private lateinit var adapter: TripHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTravelHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadTripHistory()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = TripHistoryAdapter(tripList) { trip ->
            // 아이템 클릭 시 상세 화면으로 이동
            val intent = Intent(this, PlanActivity::class.java).apply {
                putExtra("country", trip.country)
                putExtra("login", trip.nickname)
                putExtra("startDate", trip.startDate)
                putExtra("endDate", trip.endDate)
                putExtra("nights", trip.nights)
                putExtra("travelStyle", trip.travelStyle)
                putExtra("itineraryJson", trip.daysJson)
            }
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadTripHistory() {
        val uid = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        // orderBy 없이 쿼리 (복합 인덱스 불필요)
        Firebase.firestore.collection("planhistory")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE

                tripList.clear()

                if (documents.isEmpty) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                // savedAt 기준 클라이언트에서 정렬 (최신순)
                val sortedDocs = documents.documents.sortedByDescending {
                    it.getTimestamp("savedAt")?.toDate()?.time ?: 0L
                }

                for (doc in sortedDocs) {
                    val country = doc.getString("country") ?: ""
                    val nickname = doc.getString("nickname") ?: ""
                    val nights = doc.getLong("nights")?.toInt() ?: 0
                    val daysCount = doc.getLong("daysCount")?.toInt() ?: (nights + 1)
                    val startDate = doc.getString("startDate") ?: ""
                    val endDate = doc.getString("endDate") ?: ""
                    val travelStyle = doc.getString("travelStyle") ?: ""
                    val savedAt = doc.getTimestamp("savedAt")
                    val keywords = doc.get("keywords") as? List<String> ?: emptyList()

                    // days 필드를 JSON 문자열로 변환
                    val days = doc.get("days") as? List<Map<String, Any>> ?: emptyList()
                    val daysJson = convertDaysToJson(days)

                    tripList.add(TripHistoryItem(
                        docId = doc.id,
                        country = country,
                        nickname = nickname,
                        nights = nights,
                        daysCount = daysCount,
                        startDate = startDate,
                        endDate = endDate,
                        travelStyle = travelStyle,
                        savedAt = savedAt?.toDate(),
                        keywords = keywords,
                        daysJson = daysJson
                    ))
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
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
}
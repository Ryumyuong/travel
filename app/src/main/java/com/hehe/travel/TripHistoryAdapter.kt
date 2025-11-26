package com.hehe.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hehe.travel.TripHistoryAdapter.ViewHolder
import java.util.Date

class TripHistoryAdapter(
    private val items: List<TripHistoryItem>,
    private val onItemClick: (TripHistoryItem) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTripTitle)
        val tvDate: TextView = view.findViewById(R.id.tvTripDate)
        val tvStyle: TextView = view.findViewById(R.id.tvTravelStyle)
        val tvTimeAgo: TextView = view.findViewById(R.id.tvTimeAgo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // 제목: "발리 4박5일 여행"
        holder.tvTitle.text = "${item.country} ${item.nights}박${item.daysCount}일 여행"

        // 날짜
        val startFormatted = item.startDate.replace("-", ".")
        val endFormatted = item.endDate.replace("-", ".")
        holder.tvDate.text = "$startFormatted - $endFormatted"

        // 여행 스타일
        holder.tvStyle.text = item.travelStyle

        // 시간 경과
        holder.tvTimeAgo.text = getTimeAgo(item.savedAt)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    private fun getTimeAgo(date: Date?): String {
        if (date == null) return ""

        val now = Date()
        val diffMillis = now.time - date.time
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)
        val diffMonths = diffDays / 30

        return when {
            diffMonths > 0 -> "${diffMonths}월 전"
            diffDays > 0 -> "${diffDays}일 전"
            else -> "오늘"
        }
    }
}
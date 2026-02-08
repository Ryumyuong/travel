package com.hehe.travel

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class TravelHistoryAdapter(
    private val onItemClick: (TravelHistoryItem) -> Unit,
    private val onDeleteClick: (TravelHistoryItem) -> Unit = {},
    private val onPinClick: (TravelHistoryItem) -> Unit = {}
) : RecyclerView.Adapter<TravelHistoryAdapter.ViewHolder>() {

    private var items: List<TravelHistoryItem> = emptyList()

    fun submitList(newItems: List<TravelHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardHistory: MaterialCardView = itemView.findViewById(R.id.cardHistory)
        private val tvCountryWithFlag: TextView = itemView.findViewById(R.id.tvCountryWithFlag)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)
        private val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
        private val btnPin: ImageView = itemView.findViewById(R.id.btnPin)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(item: TravelHistoryItem) {
            // 국기 + 여행지
            tvCountryWithFlag.text = item.getCountryWithFlag()

            // 제목 (국가명 제외)
            tvTitle.text = item.getDisplayTitle()
            tvDate.text = item.getFormattedDate()

            // 고정 상태에 따른 핀 아이콘
            updatePinIcon(item.isPinned)

            // 타입에 따른 스타일 적용
            when (item.type) {
                1 -> {
                    // 일반 (새미패스 X) - 회색 테두리, 파란 화살표
                    cardHistory.strokeColor = Color.parseColor("#676767")
                    cardHistory.strokeWidth = 5
                    ivArrow.imageTintList = ColorStateList.valueOf(Color.parseColor("#3653AE"))
                    tvTitle.setTextColor(Color.parseColor("#000000"))
                    tvDate.setTextColor(Color.parseColor("#969696"))
                    tvBadge.visibility = View.GONE
                }
                2 -> {
                    // 새미패스 - 파란색 테두리
                    cardHistory.strokeColor = Color.parseColor("#6378C4")
                    cardHistory.strokeWidth = 5
                    ivArrow.imageTintList = ColorStateList.valueOf(Color.parseColor("#3653AE"))
                    tvTitle.setTextColor(Color.parseColor("#000000"))
                    tvDate.setTextColor(Color.parseColor("#969696"))
                    tvBadge.visibility = View.VISIBLE
                    tvBadge.text = "새미패스"
                    tvBadge.setTextColor(Color.parseColor("#6378C4"))
                    tvBadge.setBackgroundResource(R.drawable.bg_sammy_pass_badge)
                }
                3 -> {
                    // 취향맞춤 (planhistory) - 핑크 테두리
                    cardHistory.strokeColor = Color.parseColor("#FF7096")
                    cardHistory.strokeWidth = 5
                    ivArrow.imageTintList = ColorStateList.valueOf(Color.parseColor("#FF7096"))
                    tvTitle.setTextColor(Color.parseColor("#000000"))
                    tvDate.setTextColor(Color.parseColor("#969696"))
                    tvBadge.visibility = View.VISIBLE
                    tvBadge.text = "취향맞춤"
                    tvBadge.setTextColor(Color.parseColor("#F3758A"))
                    tvBadge.setBackgroundResource(R.drawable.bg_taste_badge)
                }
            }

            // 카드 클릭
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // 삭제 버튼 클릭
            btnDelete.setOnClickListener {
                onDeleteClick(item)
            }

            // 핀 버튼 클릭
            btnPin.setOnClickListener {
                onPinClick(item)
            }
        }

        private fun updatePinIcon(isPinned: Boolean) {
            if (isPinned) {
                btnPin.setImageResource(R.drawable.ic_pin_filled)
            } else {
                btnPin.setImageResource(R.drawable.ic_pin)
            }
        }
    }
}

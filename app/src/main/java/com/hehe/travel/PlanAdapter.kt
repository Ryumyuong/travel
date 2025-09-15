package com.hehe.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

// PlanAdapter.kt
class PlanAdapter(
    private var isSelectionMode: Boolean,
    private val selectedItems: MutableSet<Int>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = listOf(
        PlanItem("The Seminyak Beach", "세미냐크 해변에서 즐기는 휴식"),
        PlanItem("Revolver Espresso", "발리의 유명한 커피숍 아침식사"),
        PlanItem("Bodyworks Spa", "전신 마사지와 스파로 힐링의 시간"),
        PlanItem("Bambu Restaurant", "전통 인도네시아 요리 맛보기")
    )

    companion object {
        const val VIEW_TYPE_NORMAL = 0
        const val VIEW_TYPE_SELECTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSelectionMode) VIEW_TYPE_SELECTION else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_plan_normal, parent, false)
                NormalViewHolder(view)
            }
            VIEW_TYPE_SELECTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_plan_selection, parent, false)
                SelectionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is NormalViewHolder -> holder.bind(item, position + 1, onItemClick)
            is SelectionViewHolder -> holder.bind(item, selectedItems.contains(position), onItemClick)
        }
    }

    override fun getItemCount() = items.size

    fun updateSelectionMode(newIsSelectionMode: Boolean, newSelectedItems: MutableSet<Int>) {
        isSelectionMode = newIsSelectionMode
        selectedItems.clear()
        selectedItems.addAll(newSelectedItems)
        notifyDataSetChanged()
    }

    // 일반 모드 ViewHolder
    class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val cardView: CardView = itemView.findViewById(R.id.cardView)

        fun bind(item: PlanItem, number: Int, onItemClick: (Int) -> Unit) {
            tvNumber.text = number.toString()
            tvTitle.text = item.title
            tvDescription.text = item.description

            cardView.setOnClickListener { onItemClick(adapterPosition) }
        }
    }

    // 선택 모드 ViewHolder
    class SelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val container: View = itemView.findViewById(R.id.container)

        fun bind(item: PlanItem, isSelected: Boolean, onItemClick: (Int) -> Unit) {
            tvTitle.text = item.title
            tvDescription.text = item.description
            checkbox.isChecked = isSelected

            // 선택된 항목 강조
            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_selected_item)
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.brand_primary))
            } else {
                container.setBackgroundResource(R.drawable.bg_normal_item)
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }

            container.setOnClickListener { onItemClick(adapterPosition) }
            checkbox.setOnClickListener { onItemClick(adapterPosition) }
        }
    }
}

// 데이터 클래스
data class PlanItem(
    val title: String,
    val description: String
)
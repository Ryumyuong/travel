package com.hehe.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
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
        PlanItem("The Seminyak Beach", "석양이 아름다운 스미냐 해변"),
        PlanItem("Revolver Espresso", "분위기 좋은 시크한 로컬 카페"),
        PlanItem("Bodyworks Spa", "전통 발리 마사지로 여행 피로 풀기 좋은 곳"),
        PlanItem("Bambu Restaurant", "모던한 분위기의 인도네시아 레스토랑")
    )

    companion object {
        const val VIEW_TYPE_NORMAL = 0
        const val VIEW_TYPE_SELECTION = 1
        const val VIEW_TYPE_ADD_BUTTON = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isSelectionMode && position == items.size -> VIEW_TYPE_ADD_BUTTON
            isSelectionMode -> VIEW_TYPE_SELECTION
            else -> VIEW_TYPE_NORMAL
        }
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
            VIEW_TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_plan_add_button, parent, false)
                AddButtonViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalViewHolder -> {
                val item = items[position]
                holder.bind(item, position + 1, position == items.size - 1, onItemClick)
            }
            is SelectionViewHolder -> {
                val item = items[position]
                holder.bind(item, selectedItems.contains(position), onItemClick)
            }
            is AddButtonViewHolder -> {
                holder.bind(onItemClick)
            }
        }
    }

    override fun getItemCount() = if (isSelectionMode) items.size + 1 else items.size

    fun updateSelectionMode(newIsSelectionMode: Boolean, newSelectedItems: MutableSet<Int>) {
        isSelectionMode = newIsSelectionMode
        selectedItems.clear()
        selectedItems.addAll(newSelectedItems)
        notifyDataSetChanged()
    }

    // *** 일반 모드 ViewHolder - 새로운 타임라인 레이아웃에 맞게 수정 ***
    class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val btnRecommend: LinearLayout = itemView.findViewById(R.id.btnRecommend)
        private val btnDelete: LinearLayout = itemView.findViewById(R.id.btnDelete)
        private val timelineLine: View = itemView.findViewById(R.id.timelineLine)

        fun bind(item: PlanItem, number: Int, isLastItem: Boolean, onItemClick: (Int) -> Unit) {
            tvNumber.text = number.toString()
            tvTitle.text = item.title
            tvDescription.text = item.description

            // *** 모든 아이템에서 타임라인 점선 표시 ***
            timelineLine.visibility = View.VISIBLE

            // *** 버튼 클릭 이벤트 설정 ***
            btnRecommend.setOnClickListener {
                // 다시추천 버튼 클릭 처리
            }

            btnDelete.setOnClickListener {
                onItemClick(adapterPosition) // 삭제 버튼은 기존 클릭 이벤트 사용
            }

            // 전체 아이템 클릭 이벤트
            itemView.setOnClickListener { onItemClick(adapterPosition) }
        }
    }

    // *** 선택 모드 ViewHolder - 새로운 둥근 디자인에 맞게 수정 ***
    class SelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val container: View = itemView.findViewById(R.id.container)

        fun bind(item: PlanItem, isSelected: Boolean, onItemClick: (Int) -> Unit) {
            tvTitle.text = item.title
            tvDescription.text = item.description
            checkbox.isChecked = isSelected

            // 전체 아이템 클릭 이벤트
            container.setOnClickListener { onItemClick(adapterPosition) }
        }
    }

    // *** 일정 추가 버튼 ViewHolder ***
    class AddButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addButton: LinearLayout = itemView.findViewById(R.id.addButton)

        fun bind(onItemClick: (Int) -> Unit) {
            addButton.setOnClickListener {
                onItemClick(-1) // 일정 추가 버튼은 특별한 position(-1)으로 구분
            }
        }
    }
}

// 데이터 클래스
data class PlanItem(
    val title: String,
    val description: String
)
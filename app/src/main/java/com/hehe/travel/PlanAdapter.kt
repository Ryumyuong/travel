package com.hehe.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView

class PlanAdapter(
    private var isSelectionMode: Boolean,
    private val selectedItems: MutableSet<Int>,
    private val onItemClick: (Int) -> Unit,
    private val onRecommendClick: (Int) -> Unit = {},  // 다시추천 콜백
    private val onDeleteClick: (Int) -> Unit = {},     // 삭제 콜백
    private var country: String = ""                    // 나라명 (사진 검색용)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 동적으로 업데이트되는 아이템 리스트
    private var items: MutableList<PlanItem> = mutableListOf()

    companion object {
        const val VIEW_TYPE_NORMAL = 0
        const val VIEW_TYPE_SELECTION = 1
        const val VIEW_TYPE_ADD_BUTTON = 2
    }

    // 아이템 업데이트
    fun updateItems(newItems: List<PlanItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    // 나라명 설정
    fun setCountry(countryName: String) {
        country = countryName
    }

    // 특정 위치 아이템 교체 (다시추천용)
    fun replaceItem(position: Int, newItem: PlanItem) {
        if (position >= 0 && position < items.size) {
            items[position] = newItem
            notifyItemChanged(position)
        }
    }

    // 아이템 삭제
    fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            // 번호 재정렬을 위해 나머지 아이템 갱신
            notifyItemRangeChanged(position, items.size - position)
        }
    }

    // 현재 아이템 리스트 반환
    fun getItems(): List<PlanItem> = items.toList()

    // 아이템 개수 반환
    fun getItemSize(): Int = items.size

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
                holder.bind(
                    item = item,
                    number = position + 1,
                    isLastItem = position == items.size - 1,
                    onItemClick = onItemClick,
                    onRecommendClick = onRecommendClick,
                    onDeleteClick = onDeleteClick,
                    country = country
                )
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

    // 일반 모드 ViewHolder
    class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val btnRecommend: LinearLayout = itemView.findViewById(R.id.btnRecommend)
        private val btnDelete: LinearLayout = itemView.findViewById(R.id.btnDelete)
        private val timelineLine: View = itemView.findViewById(R.id.timelineLine)

        fun bind(
            item: PlanItem,
            number: Int,
            isLastItem: Boolean,
            onItemClick: (Int) -> Unit,
            onRecommendClick: (Int) -> Unit,
            onDeleteClick: (Int) -> Unit,
            country: String
        ) {
            tvNumber.text = number.toString()
            tvTitle.text = item.title
            tvDescription.text = item.description

            // 장소 사진 로드
            if (country.isNotEmpty() && item.title.isNotEmpty()) {
                PlacesPhotoHelper.loadPlacePhoto(item.title, country, ivThumbnail)
            }

            // 마지막 아이템이면 타임라인 숨김
            timelineLine.visibility = if (isLastItem) View.GONE else View.VISIBLE

            // 다시추천 버튼 클릭
            btnRecommend.setOnClickListener {
                onRecommendClick(adapterPosition)
            }

            // 삭제 버튼 클릭
            btnDelete.setOnClickListener {
                onDeleteClick(adapterPosition)
            }

            itemView.setOnClickListener { onItemClick(adapterPosition) }
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

            container.setOnClickListener { onItemClick(adapterPosition) }
        }
    }

    // 일정 추가 버튼 ViewHolder
    class AddButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addButton: LinearLayout = itemView.findViewById(R.id.addButton)

        fun bind(onItemClick: (Int) -> Unit) {
            addButton.setOnClickListener {
                onItemClick(-1)
            }
        }
    }
}
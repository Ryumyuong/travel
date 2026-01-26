package com.hehe.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView

// 통합 아이템 (헤더 or 장소)
sealed class PlanListItem {
    data class DayHeader(val dayNumber: Int, val title: String) : PlanListItem()
    data class Place(val planItem: PlanItem, val dayIndex: Int, val placeIndex: Int) : PlanListItem()
}

class PlanAdapter(
    private var isSelectionMode: Boolean,
    private val selectedItems: MutableSet<Int>,
    private val onItemClick: (Int) -> Unit,
    private val onRecommendClick: (Int, Int) -> Unit = { _, _ -> },  // dayIndex, placeIndex
    private val onDeleteClick: (Int, Int) -> Unit = { _, _ -> },     // dayIndex, placeIndex
    private var country: String = ""
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<PlanListItem> = mutableListOf()

    // 각 일차의 시작 position 저장 (탭 연동용)
    private val dayStartPositions: MutableList<Int> = mutableListOf()

    companion object {
        const val VIEW_TYPE_DAY_HEADER = 0
        const val VIEW_TYPE_NORMAL = 1
        const val VIEW_TYPE_SELECTION = 2
        const val VIEW_TYPE_ADD_BUTTON = 3
    }

    // 모든 일차 데이터를 통합 리스트로 변환
    fun updateAllDays(allDaysData: List<DayPlanData>) {
        items.clear()
        dayStartPositions.clear()

        allDaysData.forEachIndexed { dayIndex, dayData ->
            // 일차 시작 위치 저장
            dayStartPositions.add(items.size)

            // 헤더 추가
            items.add(PlanListItem.DayHeader(dayData.day, dayData.title))

            // 장소들 추가
            dayData.places.forEachIndexed { placeIndex, place ->
                items.add(PlanListItem.Place(
                    planItem = PlanItem(
                        title = place.name,
                        description = place.description,
                        time = place.time,
                        duration = place.duration,
                        tip = place.tip
                    ),
                    dayIndex = dayIndex,
                    placeIndex = placeIndex
                ))
            }
        }
        notifyDataSetChanged()
    }

    // 특정 일차의 시작 position 반환
    fun getDayStartPosition(dayIndex: Int): Int {
        return if (dayIndex in dayStartPositions.indices) dayStartPositions[dayIndex] else 0
    }

    // position으로 현재 일차 인덱스 반환
    fun getDayIndexForPosition(position: Int): Int {
        for (i in dayStartPositions.indices.reversed()) {
            if (position >= dayStartPositions[i]) {
                return i
            }
        }
        return 0
    }

    fun setCountry(countryName: String) {
        country = countryName
    }

    // 특정 장소 아이템 교체 (다시추천용)
    fun replacePlace(dayIndex: Int, placeIndex: Int, newPlace: PlaceData) {
        val targetPosition = findPlacePosition(dayIndex, placeIndex)
        if (targetPosition != -1) {
            items[targetPosition] = PlanListItem.Place(
                planItem = PlanItem(
                    title = newPlace.name,
                    description = newPlace.description,
                    time = newPlace.time,
                    duration = newPlace.duration,
                    tip = newPlace.tip
                ),
                dayIndex = dayIndex,
                placeIndex = placeIndex
            )
            notifyItemChanged(targetPosition)
        }
    }

    // dayIndex, placeIndex로 position 찾기
    private fun findPlacePosition(dayIndex: Int, placeIndex: Int): Int {
        items.forEachIndexed { index, item ->
            if (item is PlanListItem.Place && item.dayIndex == dayIndex && item.placeIndex == placeIndex) {
                return index
            }
        }
        return -1
    }

    override fun getItemViewType(position: Int): Int {
        if (isSelectionMode && position == items.size) return VIEW_TYPE_ADD_BUTTON
        return when (items[position]) {
            is PlanListItem.DayHeader -> VIEW_TYPE_DAY_HEADER
            is PlanListItem.Place -> if (isSelectionMode) VIEW_TYPE_SELECTION else VIEW_TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DAY_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_plan_day_header, parent, false)
                DayHeaderViewHolder(view)
            }
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
            is DayHeaderViewHolder -> {
                val item = items[position] as PlanListItem.DayHeader
                holder.bind(item)
            }
            is NormalViewHolder -> {
                val item = items[position] as PlanListItem.Place
                // 해당 일차 내에서 마지막 장소인지 확인
                val isLastInDay = isLastPlaceInDay(position)
                holder.bind(
                    item = item,
                    number = item.placeIndex + 1,
                    isLastItem = isLastInDay,
                    onRecommendClick = { onRecommendClick(item.dayIndex, item.placeIndex) },
                    onDeleteClick = { onDeleteClick(item.dayIndex, item.placeIndex) },
                    country = country
                )
            }
            is SelectionViewHolder -> {
                val item = items[position] as PlanListItem.Place
                holder.bind(item.planItem, selectedItems.contains(position), onItemClick)
            }
            is AddButtonViewHolder -> {
                holder.bind(onItemClick)
            }
        }
    }

    // 해당 position이 일차 내 마지막 장소인지 확인
    private fun isLastPlaceInDay(position: Int): Boolean {
        if (position + 1 >= items.size) return true
        return items[position + 1] is PlanListItem.DayHeader
    }

    override fun getItemCount() = if (isSelectionMode) items.size + 1 else items.size

    fun updateSelectionMode(newIsSelectionMode: Boolean, newSelectedItems: MutableSet<Int>) {
        isSelectionMode = newIsSelectionMode
        selectedItems.clear()
        selectedItems.addAll(newSelectedItems)
        notifyDataSetChanged()
    }

    // 일차 헤더 ViewHolder
    class DayHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val tvDayTitle: TextView = itemView.findViewById(R.id.tvDayTitle)

        fun bind(item: PlanListItem.DayHeader) {
            tvDayNumber.text = "${item.dayNumber}일차"
            tvDayTitle.text = item.title
        }
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
            item: PlanListItem.Place,
            number: Int,
            isLastItem: Boolean,
            onRecommendClick: () -> Unit,
            onDeleteClick: () -> Unit,
            country: String
        ) {
            tvNumber.text = number.toString()
            tvTitle.text = item.planItem.title
            tvDescription.text = item.planItem.description

            // 장소 사진 로드
            if (country.isNotEmpty() && item.planItem.title.isNotEmpty()) {
                PlacesPhotoHelper.loadPlacePhoto(item.planItem.title, country, ivThumbnail)
            }

            // 마지막 아이템이면 타임라인 숨김
            timelineLine.visibility = if (isLastItem) View.GONE else View.VISIBLE

            btnRecommend.setOnClickListener { onRecommendClick() }
            btnDelete.setOnClickListener { onDeleteClick() }
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

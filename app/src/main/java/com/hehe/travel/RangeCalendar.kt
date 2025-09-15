package com.hehe.travel

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hehe.travel.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/** 셀 데이터 */
data class DayCell(
    val date: LocalDate?,         // null = 빈 칸
    val inMonth: Boolean,
)

/** 범위 선택 상태 */
data class RangeState(var start: LocalDate? = null, var end: LocalDate? = null) {
    fun clear() { start = null; end = null }
    fun isInRange(d: LocalDate): Boolean {
        val s = start ?: return false
        val e = end ?: s
        val a = if (s.isBefore(e)) s else e
        val b = if (s.isAfter(e)) s else e
        return d >= a && d <= b
    }
}

/** 7열 그리드 설정 헬퍼 */
fun RecyclerView.setupCalendarGrid() {
    layoutManager = GridLayoutManager(context, 7)
    setHasFixedSize(true)
}

/** 어댑터 */
class DayAdapter(
    private val range: RangeState,
    private val onClick: (LocalDate) -> Unit
) : ListAdapter<DayCell, DayVH>(Diff) {

    object Diff : DiffUtil.ItemCallback<DayCell>() {
        override fun areItemsTheSame(o: DayCell, n: DayCell) = o.date == n.date && o.inMonth == n.inMonth
        override fun areContentsTheSame(o: DayCell, n: DayCell) = o == n
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day_cell, parent, false)
        return DayVH(v)
    }

    override fun onBindViewHolder(h: DayVH, pos: Int) {
        val item = getItem(pos)
        h.bind(item, range, onClick)
    }
}

class DayVH(view: View) : RecyclerView.ViewHolder(view) {
    private val tv: TextView = view.findViewById(R.id.tvDay)

    fun bind(item: DayCell, range: RangeState, onClick: (LocalDate) -> Unit) {
        if (item.date == null) { tv.text = ""; tv.background = null; tv.isClickable = false; return }

        tv.text = item.date.dayOfMonth.toString()
        tv.isClickable = item.inMonth
        tv.alpha = if (item.inMonth) 1f else 0.35f
        tv.setTextColor(ContextCompat.getColor(tv.context, android.R.color.black))
        tv.background = null

        // ★ 범위의 '끝점'에만 파란 원을 그린다
        val s = range.start
        val e = range.end ?: s
        if (s != null && e != null) {
            val a = minOf(s, e); val b = maxOf(s, e)
            if (item.date == a || item.date == b) {
                val d = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(ContextCompat.getColor(tv.context, R.color.brand_primary)) // 원하는 색
                }
                tv.setTextColor(ContextCompat.getColor(tv.context, android.R.color.white))
                tv.background = d
            }
        }
        tv.setOnClickListener { item.date?.let(onClick) }
    }
}

/** 해당 월의 6주(42칸) 셀 생성 */
fun buildMonth(yearMonth: YearMonth, firstDayOfWeek: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY): List<DayCell> {
    val first = yearMonth.atDay(1)
    val shift = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
    val start = first.minusDays(shift.toLong())
    return (0 until 42).map { i ->
        val d = start.plusDays(i.toLong())
        DayCell(date = d, inMonth = d.month == yearMonth.month)
    }
}

/** 월 텍스트 포맷터 */
val MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")

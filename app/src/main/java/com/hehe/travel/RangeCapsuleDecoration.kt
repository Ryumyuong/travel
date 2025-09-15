package com.hehe.travel

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

/**
 * 날짜 범위를 하나의 '캡슐'로 그려주는 데코.
 * - 바 높이 = 셀 높이 (끝점 원과 정확히 동일)
 * - 라운드 반경 = 셀 높이 / 2
 * - 여러 줄에 걸친 경우: 첫 줄/마지막 줄은 절반에서 시작/종료, 중간 줄은 풀폭
 */
class RangeCapsuleDecoration(
    private val state: RangeState,
    barColor: Int
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = barColor
        style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, s: RecyclerView.State) {
        val start = state.start ?: return
        val end = state.end ?: return

        val lm = parent.layoutManager as? GridLayoutManager ?: return
        val adapter = parent.adapter as? DayAdapter ?: return
        val list = adapter.currentList

        val aDate = minOf(start, end)
        val bDate = maxOf(start, end)

        val posA = list.indexOfFirst { it.date == aDate }
        val posB = list.indexOfFirst { it.date == bDate }
        if (posA == -1 || posB == -1) return

        val rowA = posA / 7
        val rowB = posB / 7

        for (row in rowA..rowB) {
            val firstPos = row * 7
            val firstView = lm.findViewByPosition(firstPos) ?: continue

            val top = firstView.top.toFloat()
            val bottom = firstView.bottom.toFloat()
            val height = bottom - top
            val radius = height / 2f

            val left = if (row == rowA) {
                val v = lm.findViewByPosition(posA) ?: continue
                (v.left + v.right) / 2f
            } else {
                parent.paddingLeft.toFloat()
            }

            val right = if (row == rowB) {
                val v = lm.findViewByPosition(posB) ?: continue
                (v.left + v.right) / 2f
            } else {
                (parent.width - parent.paddingRight).toFloat()
            }

            // 셀 사이 미세 틈 보정
            val rect = RectF(left - 0.5f, top, right + 0.5f, bottom)
            c.drawRoundRect(rect, radius, radius, paint)
        }
    }
}

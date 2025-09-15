// RangeBarDecoration.kt
package com.hehe.travel

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.core.view.get
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hehe.travel.R
import java.time.LocalDate
import java.time.YearMonth

class RangeBarDecoration(
    private val state: RangeState,
    private val barHeightRatio: Float = 0.6f, // 셀 높이의 60% 높이
    color: Int,
    private val radiusPx: Float
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, s: RecyclerView.State) {
        val start = state.start ?: return
        val end = state.end ?: return
        val a = minOf(start, end)
        val b = maxOf(start, end)

        val lm = parent.layoutManager as? GridLayoutManager ?: return
        val adapter = parent.adapter as? DayAdapter ?: return
        val list = adapter.currentList

        val posA = list.indexOfFirst { it.date == a }
        val posB = list.indexOfFirst { it.date == b }
        if (posA == -1 || posB == -1) return

        val rowA = posA / 7
        val rowB = posB / 7

        for (row in rowA..rowB) {
            val firstPos = row * 7
            val firstView = lm.findViewByPosition(firstPos) ?: continue
            val cellTop = firstView.top.toFloat()
            val cellBottom = firstView.bottom.toFloat()
            val cellHeight = cellBottom - cellTop
            val barH = cellHeight * barHeightRatio
            val top = cellTop + (cellHeight - barH) / 2f
            val bottom = top + barH

            val left = when (row) {
                rowA -> {
                    val v = lm.findViewByPosition(posA) ?: return
                    // 시작 셀의 가운데부터
                    (v.left + v.right) / 2f
                }
                else -> parent.paddingLeft.toFloat()
            }
            val right = when (row) {
                rowB -> {
                    val v = lm.findViewByPosition(posB) ?: return
                    (v.left + v.right) / 2f
                }
                else -> (parent.width - parent.paddingRight).toFloat()
            }

            // 같은 줄에서 시작==끝이면 짧은 캡슐 그리기
            val rect = RectF(left, top, right, bottom)
            c.drawRoundRect(rect, radiusPx, radiusPx, paint)
        }
    }
}

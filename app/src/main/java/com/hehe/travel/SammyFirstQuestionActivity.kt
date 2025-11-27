package com.hehe.travel

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SammyFirstQuestionActivity : AppCompatActivity() {

    private lateinit var seekBarAge: SeekBar
    private val ageLabels = mutableListOf<TextView>()
    private var selectedAge = "30대" // 기본값: 30대

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sammy_first_question)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingLeft = v.paddingLeft
            val originalPaddingTop = v.paddingTop
            val originalPaddingRight = v.paddingRight
            val originalPaddingBottom = v.paddingBottom
            v.setPadding(
                originalPaddingLeft + systemBars.left,
                originalPaddingTop + systemBars.top,
                originalPaddingRight + systemBars.right,
                originalPaddingBottom + systemBars.bottom
            )
            insets
        }

        setupAgeSlider()
    }

    private fun setupAgeSlider() {
        seekBarAge = findViewById(R.id.seekBarAge)

        // 연령대 레이블 TextView들 가져오기
        val llAgeLabels = findViewById<android.widget.LinearLayout>(R.id.llAgeLabels)
        for (i in 0 until llAgeLabels.childCount) {
            val textView = llAgeLabels.getChildAt(i) as TextView
            ageLabels.add(textView)
        }

        // 초기 선택 상태 표시 (30대 = progress 2)
        updateAgeLabels(2)

        seekBarAge.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateAgeLabels(progress)

                // 선택된 연령대 저장
                selectedAge = when(progress) {
                    0 -> "10대"
                    1 -> "20대"
                    2 -> "30대"
                    3 -> "40대"
                    4 -> "50대"
                    else -> "30대"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateAgeLabels(selectedPosition: Int) {
        // 모든 레이블을 기본 색상으로
        ageLabels.forEachIndexed { index, textView ->
            if (index == selectedPosition) {
                textView.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
                textView.textSize = 14f
            } else {
                textView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                textView.textSize = 12f
            }
        }
    }

    fun getSelectedAge(): String {
        return selectedAge
    }
}
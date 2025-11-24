package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class SammyFirstQuestionActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var grade:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_sammy_first_question)

        auth = FirebaseAuth.getInstance()
        findViewById<TextView>(R.id.etNickname).text =
            auth.currentUser?.displayName?.let { "$it" }

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroup)
        val btnNext = findViewById<AppCompatButton>(R.id.btnNext)

        val btnRefresh = findViewById<ImageButton>(R.id.btnRefresh)
        val tvRefresh = findViewById<TextView>(R.id.tvRefresh)

        btnRefresh.setOnClickListener {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                chip?.isChecked = false
            }
            Toast.makeText(this, "태그 선택이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }



        tvRefresh.setOnClickListener {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                chip?.isChecked = false
            }
            Toast.makeText(this, "태그 선택이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }


        // ✅ ChipGroup 안의 모든 Chip에 클릭 리스너 달기
        btnNext.setOnClickListener {
            // 1) 선택된 칩 텍스트 모으기
            val selectedTags = (0 until chipGroup.childCount)
                .mapNotNull { chipGroup.getChildAt(it) as? Chip }
                .filter { it.isChecked }
                .map { it.text.toString() }

            // 2) 나머지 값들 수집
            val nickname = findViewById<EditText>(R.id.etNickname).text.toString()
            val gender = if (findViewById<RadioButton>(R.id.rbMale).isChecked) "남자" else "여자"
            val ageDecade = (findViewById<SeekBar>(R.id.seekBarAge).progress + 1) * 10 // 0~4 → 10~50

            // 3) 다음 화면으로 전달 (예시)
            val intent = Intent(this, SammySecondQuestionActivity::class.java).apply {
                putExtra("nickname", nickname)
                putExtra("gender", gender)
                putExtra("ageDecade", ageDecade)
                putStringArrayListExtra("tags", ArrayList(selectedTags))
            }
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // XML에서 설정한 패딩 값 유지하면서 시스템 바 insets 추가
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

        val ageSeekBar = findViewById<SeekBar>(R.id.seekBarAge)
        ageSeekBar.max = 4

        ageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                showAgeToast(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 드래그 끝났을 때만 보이게 하려면 여기서만 호출
                // showAgeToast(seekBar?.progress ?: 0)
            }
        })


    }

    private fun showAgeToast(progress: Int) {
        val decade = (progress + 1) * 10
        grade = "${decade}대"
    }
}
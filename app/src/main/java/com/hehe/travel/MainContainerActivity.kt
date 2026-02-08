package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.hehe.travel.databinding.ActivityMainContainerBinding
import com.hehe.travel.fragment.MyInfoFragment
import com.hehe.travel.fragment.SearchFragment
import com.hehe.travel.fragment.TasteFragment

class MainContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainContainerBinding
    private lateinit var auth: FirebaseAuth
    private var backPressedTime: Long = 0

    // 현재 선택된 탭
    private var currentTabId: Int = R.id.tab_country

    // Fragment 캐시
    private var searchFragment: SearchFragment? = null
    private var tasteFragment: TasteFragment? = null
    private var myInfoFragment: MyInfoFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupBottomNavigation()

        // 초기 탭 설정 (Intent에서 받거나 기본값)
        val initialTab = intent.getIntExtra("initialTab", R.id.tab_country)
        binding.bottomNav.selectedItemId = initialTab
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_country -> {
                    showFragment(getSearchFragment())
                    currentTabId = R.id.tab_country
                    true
                }
                R.id.tab_search -> {
                    showFragment(getTasteFragment())
                    currentTabId = R.id.tab_search
                    true
                }
                R.id.tab_profile -> {
                    // 비로그인 시 로그인 화면으로
                    if (auth.currentUser == null) {
                        Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        false
                    } else {
                        showFragment(getMyInfoFragment())
                        currentTabId = R.id.tab_profile
                        true
                    }
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun getSearchFragment(): SearchFragment {
        if (searchFragment == null) {
            searchFragment = SearchFragment()
        }
        return searchFragment!!
    }

    private fun getTasteFragment(): TasteFragment {
        if (tasteFragment == null) {
            tasteFragment = TasteFragment()
        }
        return tasteFragment!!
    }

    private fun getMyInfoFragment(): MyInfoFragment {
        if (myInfoFragment == null) {
            myInfoFragment = MyInfoFragment()
        }
        return myInfoFragment!!
    }

    // 외부에서 탭 선택
    fun selectTab(tabId: Int) {
        binding.bottomNav.selectedItemId = tabId
    }

    // Fragment 새로고침
    fun refreshCurrentFragment() {
        when (currentTabId) {
            R.id.tab_profile -> {
                myInfoFragment = null
                showFragment(getMyInfoFragment())
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 다른 탭에서 뒤로가기 → 나라검색 탭으로
        if (currentTabId != R.id.tab_country) {
            binding.bottomNav.selectedItemId = R.id.tab_country
            return
        }

        // 나라검색 탭에서 뒤로가기 → 2번 눌러야 종료
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            super.onBackPressed()
            finishAffinity()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "한 번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
        }
    }
}

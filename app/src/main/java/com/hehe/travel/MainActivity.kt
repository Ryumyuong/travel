package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import com.hehe.travel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityMainBinding

    // Views
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var vipButton: Button
    private lateinit var welcomeCard: CardView
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeDesc: TextView
    private lateinit var googleSignInButton: Button

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Firebase 초기화 먼저
        initFirebase()

        // 이미 로그인된 상태면 UI 표시 없이 바로 이동
        if (auth.currentUser != null) {
            routeAfterLogin()
            return
        }

        // 로그인 안 된 경우에만 UI 세팅
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupClickListeners()

        binding.tvSignUp.setOnClickListener {
            Toast.makeText(this, "회원가입 창", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initViews() {
        titleText = findViewById(R.id.tv_title)
//        subtitleText = findViewById(R.id.tv_subtitle)
        welcomeCard = findViewById(R.id.card_welcome)
//        welcomeTitle = findViewById(R.id.tv_welcome_title)
//        welcomeDesc = findViewById(R.id.tv_welcome_desc)
        googleSignInButton = findViewById(R.id.btn_google_signin)

        // Set initial text
        titleText.text = "MBTI로 찾는 나다운 여행, AI 추천 여행 플랫폼"
//        subtitleText.text = "당신만을 위한 럭셔리 여행 큐레이션"
//        welcomeTitle.text = "환영합니다"
//        welcomeDesc.text = "Google 계정으로 로그인하여\n개인화된 프리미엄 여행 추천을 받아보세요"
        googleSignInButton.text = "구글 아이디로 시작하기"
    }

    private fun setupClickListeners() {

        googleSignInButton.setOnClickListener {
            Log.d("lmj","버튼 클릭")
            startGoogleSignIn()

        }


    }

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        Log.d("lmj","$signInIntent")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrEmpty()) {
                    Log.e("lmj", "idToken이 null/empty 입니다. 웹 클라이언트 ID 또는 SHA 지문 설정을 확인하세요.")
                    return
                }
                firebaseAuthWithGoogle(idToken)
            } catch (e: ApiException) {
                logSignInError(e)
            }
        }
    }

    private fun logSignInError(e: ApiException) {
        val code = e.statusCode
        val codeString = GoogleSignInStatusCodes.getStatusCodeString(code)
        Log.e("lmj", "Google Sign-In 실패: code=$code ($codeString), message=${e.message}", e)
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    routeAfterLogin()   // ✅ 로그인 성공 후 분기
                } else {
                    Log.e("lmj", "로그인 실패", task.exception)
                }
            }
    }

    private fun routeAfterLogin() {
        val uid = auth.currentUser?.uid ?: return
        Firebase.firestore.collection("profiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // 이미 한 번 등록 완료 → 바로 검색 화면
                    startActivity(Intent(this, SearchActivity::class.java))
                } else {
                    // 첫 로그인(아직 프로필 없음) → 패스/첫 질문 화면
                    startActivity(Intent(this, SammyPassMainActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("lmj", "프로필 조회 실패", e)
                // 실패 시 보수적으로 프로필 화면으로 보냄
                startActivity(Intent(this, SammyPassMainActivity::class.java))
                finish()
            }
    }

}
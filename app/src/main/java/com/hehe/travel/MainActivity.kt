package com.hehe.travel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        initViews()
        setupClickListeners()
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
        subtitleText = findViewById(R.id.tv_subtitle)
        welcomeCard = findViewById(R.id.card_welcome)
        welcomeTitle = findViewById(R.id.tv_welcome_title)
        welcomeDesc = findViewById(R.id.tv_welcome_desc)
        googleSignInButton = findViewById(R.id.btn_google_signin)

        // Set initial text
        titleText.text = "희희호호 여행 추천"
        subtitleText.text = "당신만을 위한 럭셔리 여행 큐레이션"
        welcomeTitle.text = "환영합니다"
        welcomeDesc.text = "Google 계정으로 로그인하여\n개인화된 프리미엄 여행 추천을 받아보세요"
        googleSignInButton.text = "Google로 시작하기"
    }

    private fun setupClickListeners() {

        googleSignInButton.setOnClickListener {
            Log.d("lmj","버튼 클릭")
            startGoogleSignIn()
//            val intent = Intent(this, SearchActivity::class.java)
//            startActivity(intent)
//            finish()

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
        Log.d("lmj","권한도착")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    Log.d("lmj","유저 : $user")
                    navigateToSearch()
                } else {
                    // Sign in failed
                    Log.d("lmj","로그인오류 : ${task.exception?.printStackTrace()}")
                    task.exception?.printStackTrace()
                }
            }
    }

    private fun navigateToSearch() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in
        val currentUser = auth.currentUser

        if (currentUser != null) {
            navigateToSearch()
        }
    }
}
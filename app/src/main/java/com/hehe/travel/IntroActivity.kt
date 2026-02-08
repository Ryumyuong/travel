package com.hehe.travel

import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class IntroActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var auth: FirebaseAuth
    private var textureView: TextureView? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var navigateRunnable: Runnable? = null
    private var isVideoCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        auth = FirebaseAuth.getInstance()

        // 이미 로그인된 경우 인트로 스킵하고 바로 메인으로 이동
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainContainerActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_intro)

        // 비회원 데이터 초기화
        GuestProfileData.clear()

        // TextureView 설정
        textureView = findViewById(R.id.videoBackground)
        textureView?.surfaceTextureListener = this

        // 5초 후 자동으로 화면 전환
        navigateRunnable = Runnable {
            navigateToNext()
        }
        handler.postDelayed(navigateRunnable!!, 5000)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        // 화면 비율에 따라 다른 영상 선택
        val aspectRatio = height.toFloat() / width.toFloat()
        val videoResId = if (aspectRatio >= 1.6f) {
            // 좁은 화면 (세로로 긴 화면) - 기본 영상
            R.raw.intro
        } else {
            // 넓은 화면 (가로로 넓은 화면) - intro2 영상
            R.raw.intro2
        }
        setupVideo(Surface(surface), width, height, videoResId)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateTextureViewSize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releaseMediaPlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // 업데이트 시 특별한 처리 필요 없음
    }

    private fun setupVideo(surface: Surface, viewWidth: Int, viewHeight: Int, videoResId: Int) {
        try {
            mediaPlayer = MediaPlayer().apply {
                val videoUri = Uri.parse("android.resource://${packageName}/${videoResId}")
                setDataSource(this@IntroActivity, videoUri)
                setSurface(surface)
                isLooping = false  // 반복 재생 안함

                setOnPreparedListener { mp ->
                    // 비디오 크기를 가져와서 TextureView 스케일 조정
                    val videoWidth = mp.videoWidth
                    val videoHeight = mp.videoHeight
                    adjustAspectRatio(viewWidth, viewHeight, videoWidth, videoHeight)
                    mp.seekTo(1000)
                    mp.start()
                }

                setOnCompletionListener {
                    // 영상 완료 - 마지막 프레임에서 정지 상태 유지
                    isVideoCompleted = true
                }

                setOnErrorListener { _, _, _ ->
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTextureViewSize(viewWidth: Int, viewHeight: Int) {
        mediaPlayer?.let { mp ->
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            if (videoWidth > 0 && videoHeight > 0) {
                adjustAspectRatio(viewWidth, viewHeight, videoWidth, videoHeight)
            }
        }
    }

    private fun adjustAspectRatio(viewWidth: Int, viewHeight: Int, videoWidth: Int, videoHeight: Int) {
        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
        val viewAspectRatio = viewHeight.toFloat() / viewWidth.toFloat()

        val scaleX: Float
        val scaleY: Float

        if (aspectRatio > viewAspectRatio) {
            // 비디오가 더 세로로 길다 -> 가로 기준으로 맞추고 위아래 잘림
            scaleX = 1f
            scaleY = aspectRatio / viewAspectRatio
        } else {
            // 비디오가 더 가로로 길다 -> 세로 기준으로 맞추고 좌우 잘림
            scaleX = viewAspectRatio / aspectRatio
            scaleY = 1f
        }

        val pivotX = viewWidth / 2f
        val pivotY = viewHeight / 2f

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, pivotX, pivotY)

        textureView?.setTransform(matrix)
    }

    private fun navigateToNext() {
        if (auth.currentUser != null) {
            // 로그인된 경우 MainContainerActivity로 이동
            startActivity(Intent(this, MainContainerActivity::class.java))
        } else {
            startActivity(Intent(this, SammyPassMainActivity::class.java))
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        try {
            // 영상이 완료된 상태면 다시 시작하지 않음
            if (isVideoCompleted) return

            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        navigateRunnable?.let { handler.removeCallbacks(it) }
        releaseMediaPlayer()
    }
}

package com.nexus.app.ui.splash

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.nexus.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var splashView: CgSplashView
    private val handler = Handler(Looper.getMainLooper())
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()

        splashView = CgSplashView(this)
        setContentView(splashView)

        handler.postDelayed({
            waitForAnimationAndNavigate()
        }, 5500)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun waitForAnimationAndNavigate() {
        if (navigated) return
        navigated = true

        val rootView = splashView
        rootView.animate()
            .alpha(0f)
            .setDuration(600)
            .withEndAction {
                navigateToMain()
            }
            .start()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        // prevent exit during splash
    }
}

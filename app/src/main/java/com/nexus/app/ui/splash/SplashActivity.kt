package com.nexus.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.nexus.app.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var splashView: CgSplashView
    private val handler = Handler(Looper.getMainLooper())
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        hideSystemUI()

        splashView = CgSplashView(this)
        setContentView(splashView)

        handler.postDelayed({
            waitForAnimationAndNavigate()
        }, 5500)
    }

    @Suppress("DEPRECATION")
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

        splashView.animate()
            .alpha(0f)
            .setDuration(600)
            .withEndAction {
                navigateToMain()
            }
            .start()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, LoginActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}
}

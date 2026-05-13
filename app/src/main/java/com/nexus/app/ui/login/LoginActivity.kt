package com.nexus.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.TextView
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.nexus.app.R
import com.nexus.app.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPwd: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvError: TextView
    private lateinit var ivLogo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPwd = findViewById(R.id.etPwd)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        ivLogo = findViewById(R.id.ivLogo)

        ivLogo.post {
            val d: Drawable? = ivLogo.drawable
            if (d is Animatable) d.start()
        }

        btnLogin.setOnClickListener { doLogin() }

        animateEntrance()
    }

    private fun animateEntrance() {
        val items = listOf(
            findViewById<View>(R.id.ivLogo),
            findViewById<View>(R.id.tvTitle),
            findViewById<View>(R.id.tvSubtitle),
            findViewById<View>(R.id.tilEmail),
            findViewById<View>(R.id.tilPwd),
            btnLogin
        )

        items.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(80L + index * 70L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun doLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val pwd = etPwd.text?.toString() ?: ""

        btnLogin.isEnabled = false
        btnLogin.text = "验证中..."
        tvError.animate().alpha(0f).setDuration(200).start()

        etEmail.postDelayed({
            if (email == "admin" && pwd == "1") {
                onLoginSuccess()
            } else {
                onLoginFail()
            }
        }, 800)
    }

    private fun onLoginSuccess() {
        val rootView = findViewById<View>(android.R.id.content)

        val scale = ScaleAnimation(1f, 1.05f, 1f, 1.05f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
        val fade = AlphaAnimation(1f, 0f)
        val set = AnimationSet(true)
        set.addAnimation(scale)
        set.addAnimation(fade)
        set.duration = 500
        set.interpolator = AccelerateDecelerateInterpolator()
        set.fillAfter = true

        rootView.startAnimation(set)

        set.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        })
    }

    private fun onLoginFail() {
        btnLogin.isEnabled = true
        btnLogin.text = "登 录"
        tvError.animate().alpha(1f).setDuration(300).start()

        val rootView = findViewById<View>(android.R.id.content)
        rootView.animate().translationX(10f).setDuration(50).withEndAction {
            rootView.animate().translationX(-10f).setDuration(50).withEndAction {
                rootView.animate().translationX(6f).setDuration(50).withEndAction {
                    rootView.animate().translationX(-6f).setDuration(50).withEndAction {
                        rootView.animate().translationX(0f).setDuration(50).start()
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // no-op
    }
}

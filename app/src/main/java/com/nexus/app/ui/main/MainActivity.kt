package com.nexus.app.ui.main

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.nexus.app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        animateEntrance()
    }

    private fun animateEntrance() {
        val topViews = listOf(
            findViewById<View>(R.id.tvWelcome),
            findViewById<View>(R.id.tvMainTitle),
            findViewById<View>(R.id.tvSub)
        )

        topViews.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = -15f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(100L + index * 120L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        val cards = listOf(
            findViewById<View>(R.id.cardData),
            findViewById<View>(R.id.cardTask),
            findViewById<View>(R.id.cardMonitor),
            findViewById<View>(R.id.cardConfig)
        )

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 30f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(400L + index * 80L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
}

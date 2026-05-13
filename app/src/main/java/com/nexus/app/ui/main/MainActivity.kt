package com.nexus.app.ui.main

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.nexus.app.R
import com.nexus.app.ui.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var contentArea: FrameLayout
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("nexus_auth", MODE_PRIVATE)

        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        bottomNav = findViewById(R.id.bottomNav)
        contentArea = findViewById(R.id.navHostFragment)

        toolbar.setNavigationOnClickListener { drawerLayout.open() }

        setupBottomNav()
        setupDrawerNav()

        showMessagesPage()
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navMessages -> { toolbar.title = "消息"; showMessagesPage(); true }
                R.id.navContacts -> { toolbar.title = "通讯录"; showContent("通讯录页面"); true }
                R.id.navDiscover -> { toolbar.title = "发现"; showContent("发现页面"); true }
                R.id.navProfile -> { toolbar.title = "我的"; showContent("个人中心"); true }
                else -> false
            }
        }
    }

    private fun setupDrawerNav() {
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.drawerLogout -> {
                    prefs.edit().remove("logged_in").apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                else -> {
                    toolbar.title = item.title
                    showContent(item.title.toString())
                }
            }
            drawerLayout.close()
            true
        }
    }

    private fun showMessagesPage() {
        contentArea.removeAllViews()
        val tv = TextView(this).apply {
            text = "消息列表将在此显示"
            setPadding(32, 32, 32, 32)
            textSize = 14f
            setTextColor(0xFF999999.toInt())
        }
        contentArea.addView(tv)
    }

    private fun showContent(title: String) {
        contentArea.removeAllViews()
        val tv = TextView(this).apply {
            text = title
            setPadding(32, 32, 32, 32)
            textSize = 16f
            setTextColor(0xFF1A1A2E.toInt())
        }
        contentArea.addView(tv)
    }
}

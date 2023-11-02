package com.example.randommemories

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.randommemories.helpers.LocaleHelper
import com.example.randommemories.mainFlow.HomeFragment


class MainActivity : AppCompatActivity() {
    var genderFemale = true

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        actionBar?.hide()
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false

        LocaleHelper.onCreate(this, "he")
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment.newInstance(activeDiary = false))
                .commitNow()
        }

        val restartButton = findViewById<Button>(R.id.restart_button)
        restartButton.setOnClickListener {
            restartButton.background.clearColorFilter()
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
        }

        val menuButton = findViewById<Button>(R.id.menu_button)
        menuButton.setOnClickListener {
            menuButton.background.clearColorFilter()
            this.finish()
        }
    }
}
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
import android.Manifest
import com.example.randommemories.mainFlow.WriteFragment
import com.example.randommemories.notifications.NotificationScheduler

class MainActivity : AppCompatActivity() {
    var genderFemale = true
    var isDemo = false

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


        // In case of enter the app from notification
        if (intent.getStringExtra("navigate_to") == "WriteFragment") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, WriteFragment())
                .commit()
        }

        if (savedInstanceState == null) {
            // if not demo mode, this is my personal use mode means there is an active dairy
            val appInUse = !isDemo
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment.newInstance(activeDiary = appInUse))
                .commitNow()
        }

        val restartButton = findViewById<Button>(R.id.restart_button)
        restartButton.setOnClickListener {
            restartButton.background.clearColorFilter()
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
        }
        restartButton.visibility = if (isDemo) View.VISIBLE else View.INVISIBLE

        val menuButton = findViewById<Button>(R.id.menu_button)
        menuButton.setOnClickListener {
            menuButton.background.clearColorFilter()
            this.finish()
        }

        if (!isDemo) {
            // Request notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.CAMERA
                    ), 100
                )
            }

            // Schedule the first random notification
            NotificationScheduler.scheduleNext(this)
        }
    }
}
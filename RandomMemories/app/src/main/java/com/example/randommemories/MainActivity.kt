package com.example.randommemories

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.randommemories.helpers.LocaleHelper
import com.example.randommemories.ui.main.WriteFragment

class MainActivity : AppCompatActivity() {
    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.bg)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        LocaleHelper.onCreate(this, "he")
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, WriteFragment.newInstance())
                .commitNow()
        }
    }
}
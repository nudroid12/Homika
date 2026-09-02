package com.homiq.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.homiq.app.data.preferences.AppearancePreferences
import com.homiq.app.ui.HomiqApp
import com.homiq.app.ui.theme.HomiqTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppearancePreferences(applicationContext).applySavedMode()
        enableEdgeToEdge()

        setContent {
            HomiqTheme {
                HomiqApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val homiqApplication =
            application as HomiqApplication
        homiqApplication.container
            .appLockService
            .onAppForeground()
        homiqApplication.container
            .updateManager
            .onAppForeground()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            val homiqApplication =
                application as HomiqApplication
            homiqApplication.container
                .appLockService
                .onAppBackground()
        }
        super.onStop()
    }
}

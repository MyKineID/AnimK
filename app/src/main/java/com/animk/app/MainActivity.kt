package com.animk.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.animk.app.ui.screen.MainScreen
import com.animk.app.ui.theme.AnimKTheme
import com.animk.app.ui.theme.AppThemeAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableMaxRefreshRate()

        setContent {
            var activeThemeAccent by remember { mutableStateOf(AppThemeAccent.NEON_LIME) }

            AnimKTheme(accent = activeThemeAccent) {
                MainScreen(
                    activeAccent = activeThemeAccent,
                    onAccentChange = { activeThemeAccent = it }
                )
            }
        }
    }

    private fun enableMaxRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val display = window.context.display
                val maxRefreshRateMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxRefreshRateMode != null) {
                    val layoutParams = window.attributes
                    layoutParams.preferredDisplayModeId = maxRefreshRateMode.modeId
                    window.attributes = layoutParams
                }
            } catch (e: Exception) {
                // Fallback gracefully if display mode cannot be queried
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
        }
    }
}

package com.animk.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.animk.app.data.remoteconfig.DirectorConfigProvider
import com.animk.app.ui.screen.MainScreen
import com.animk.app.ui.theme.AppThemeAccent
import com.animk.app.ui.theme.AnimKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableMaxRefreshRate()
        DirectorConfigProvider.init(this)

        setContent {
            var activeThemeAccent by remember { mutableStateOf(AppThemeAccent.NEON_GECKO) }

            AnimKTheme(accent = activeThemeAccent) {
                MainScreen(
                    activeAccent = activeThemeAccent,
                    onAccentChange = { activeThemeAccent = it }
                )
            }
        }
    }

    private fun enableMaxRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = maxMode.modeId
                    window.attributes = lp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

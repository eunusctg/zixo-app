package com.zexo.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.zexo.app.data.local.PreferencesManager
import com.zexo.app.ui.navigation.ZixoNavHost
import com.zexo.app.ui.theme.ZixoBg
import com.zexo.app.ui.theme.ZixoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by preferencesManager.settingsFlow.collectAsState(
                initial = com.zexo.app.data.model.AppSettings()
            )

            // Apply privacy blur (FLAG_SECURE) based on settings
            if (settings.privacyBlur) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            ZixoTheme(themeMode = settings.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ZixoBg
                ) {
                    ZixoNavHost()
                }
            }
        }
    }
}

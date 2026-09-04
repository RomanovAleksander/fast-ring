package com.oleksandr.fastflow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oleksandr.fastflow.ui.AppViewModel
import com.oleksandr.fastflow.ui.FastFlowApp
import com.oleksandr.fastflow.ui.theme.FastFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark-only, so both system bars keep light icons whatever
        // the system theme says (SPEC 5.1).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsStateWithLifecycle()

            // Switching palettes crossfades inside the theme (SPEC 3.5).
            FastFlowTheme(palette = settings.palette) {
                FastFlowApp(
                    onboardingDone = settings.onboardingDone,
                    onOnboardingComplete = appViewModel::completeOnboarding,
                )
            }
        }
    }
}

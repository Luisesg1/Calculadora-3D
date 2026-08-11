package com.print3d.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.print3d.calculator.core.util.LocaleHelper
import com.print3d.calculator.feature.settings.SettingsViewModel
import com.print3d.calculator.navigation.AppNavGraph
import com.print3d.calculator.ui.theme.Print3DTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        var loaded = false
        splash.setKeepOnScreenCondition { !loaded }

        setContent {
            val vm: SettingsViewModel = hiltViewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()

            // Hold splash + render nothing until DataStore delivers the real settings.
            val current = settings ?: return@setContent
            loaded = true

            val base = LocalContext.current
            val localized = remember(current.language) { LocaleHelper.wrap(base, current.language) }

            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration
            ) {
                Print3DTheme(themeMode = current.theme) {
                    Surface(Modifier.fillMaxSize()) {
                        if (current.onboardingDone) {
                            AppNavGraph()
                        } else {
                            com.print3d.calculator.feature.onboarding.OnboardingScreen()
                        }
                    }
                }
            }
        }
    }
}

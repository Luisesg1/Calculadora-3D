package com.print3d.calculator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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

    // Apply the saved in-app language to the Activity's base context. This is the authoritative
    // override — unlike a Compose-only LocalContext swap, it survives OEM skins (MIUI/HyperOS).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase, LocaleHelper.persistedTag(newBase)))
    }

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

            // The base context was localized at attachBaseContext time using the tag saved *then*.
            // If the desired language differs (user switched it, or an install predates the mirror),
            // sync the mirror FIRST, then re-create so the base context re-applies the locale — the
            // only place MIUI/HyperOS reliably honor. Persisting before recreate is what stops an
            // infinite recreate loop: the next attachBaseContext reads the now-matching tag.
            val attachedTag = remember { LocaleHelper.persistedTag(this@MainActivity) }
            LaunchedEffect(current.language) {
                if (current.language.tag != attachedTag) {
                    LocaleHelper.persist(this@MainActivity, current.language.tag)
                    recreate()
                }
            }

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

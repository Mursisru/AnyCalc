package com.mursis.anycalc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mursis.anycalc.ui.camera.CameraRoute
import com.mursis.anycalc.ui.history.HistoryRoute
import com.mursis.anycalc.ui.onboarding.Onboarding
import com.mursis.anycalc.ui.settings.SettingsRoute
import com.mursis.anycalc.ui.settings.SettingsViewModel
import com.mursis.anycalc.ui.theme.AnyCalcTheme
import java.util.Locale

@Composable
fun AppRoot() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(settings.language) {
        runCatching { Locale.setDefault(Locale(settings.language)) }
    }

    AnyCalcTheme(settings = settings) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = settings.fontScale)) {
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> CameraRoute(onHistory = { tab = 1 }, onSettings = { tab = 2 })
                    1 -> HistoryRoute(onBack = { tab = 0 })
                    2 -> SettingsRoute(onBack = { tab = 0 })
                }
                if (!settings.onboardingDone) {
                    Onboarding(onDone = settingsViewModel::completeOnboarding)
                }
            }
        }
    }
}

package com.quranmedia.player.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.quranmedia.player.BuildConfig
import com.quranmedia.player.data.repository.DarkModePreference
import com.quranmedia.player.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import com.quranmedia.player.presentation.theme.QuranMediaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule athkar notifications from saved settings
        com.quranmedia.player.data.notification.AthkarAlarmReceiver.scheduleFromSettings(
            this, settingsRepository.getCurrentSettings()
        )

        setContent {
            val settings by settingsRepository.settings.collectAsState(
                initial = com.quranmedia.player.data.repository.UserSettings()
            )
            val systemDark = isSystemInDarkTheme()
            val isDark = when (settings.darkModePreference) {
                DarkModePreference.ON -> true
                DarkModePreference.OFF -> false
                DarkModePreference.AUTO -> systemDark
            }
            QuranMediaTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuranMediaApp(settingsRepository)
                }
            }
        }
    }
}

@Composable
fun QuranMediaApp(settingsRepository: SettingsRepository) {
    val navController = androidx.navigation.compose.rememberNavController()

    // Check if Onboarding screen should be shown (first install or not completed)
    val shouldShowOnboarding = remember {
        settingsRepository.shouldShowWhatsNew(BuildConfig.VERSION_CODE)
    }

    val currentSettings = settingsRepository.getCurrentSettings()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    com.quranmedia.player.presentation.navigation.QuranNavGraph(
        navController = navController,
        shouldShowOnboarding = shouldShowOnboarding,
        onToggleDarkMode = {
            coroutineScope.launch {
                val current = settingsRepository.getCurrentSettings().darkModePreference
                val next = when (current) {
                    com.quranmedia.player.data.repository.DarkModePreference.OFF ->
                        com.quranmedia.player.data.repository.DarkModePreference.ON
                    com.quranmedia.player.data.repository.DarkModePreference.ON ->
                        com.quranmedia.player.data.repository.DarkModePreference.OFF
                    com.quranmedia.player.data.repository.DarkModePreference.AUTO ->
                        com.quranmedia.player.data.repository.DarkModePreference.ON
                }
                settingsRepository.setDarkModePreference(next)
            }
        }
    )
}

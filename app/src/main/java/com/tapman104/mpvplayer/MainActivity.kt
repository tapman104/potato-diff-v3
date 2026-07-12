package com.tapman104.mpvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tapman104.mpvplayer.core.preferences.UserPreferencesRepository
import com.tapman104.mpvplayer.home.ui.HomeScreen
import com.tapman104.mpvplayer.settings.SettingsScreen
import com.tapman104.mpvplayer.settings.SettingsViewModel
import com.tapman104.mpvplayer.settings.SettingsViewModelFactory
import com.tapman104.mpvplayer.ui.theme.MpvPlayerTheme

class MainActivity : ComponentActivity() {
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val intent = Intent(this, PlayerActivity::class.java).apply {
                data = it
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MpvPlayerTheme {
                var showSettings by remember { mutableStateOf(false) }

                val transition = updateTransition(targetState = showSettings, label = "nav")

                transition.AnimatedContent(
                    transitionSpec = {
                        if (targetState) {
                            // Navigating to Settings: slide in from right
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            )
                        } else {
                            // Back from Settings: slide in from left
                            slideInHorizontally(
                                initialOffsetX = { -it / 4 },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            )
                        }
                    }
                ) { isSettings ->
                    if (isSettings) {
                        val context = LocalContext.current
                        val settingsViewModel: SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(
                                UserPreferencesRepository(context.applicationContext)
                            )
                        )
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { showSettings = false }
                        )
                    } else {
                        HomeScreen(
                            onOpenFile = { filePickerLauncher.launch(arrayOf("video/*")) },
                            onSettingsClick = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

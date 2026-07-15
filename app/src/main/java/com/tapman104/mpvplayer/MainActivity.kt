package com.tapman104.mpvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.tapman104.mpvplayer.history.HistoryScreen
import com.tapman104.mpvplayer.history.HistoryViewModel
import com.tapman104.mpvplayer.home.ui.HomeScreen
import com.tapman104.mpvplayer.settings.SettingsScreen
import com.tapman104.mpvplayer.settings.SettingsViewModel
import com.tapman104.mpvplayer.settings.SettingsViewModelFactory
import com.tapman104.mpvplayer.ui.theme.MpvPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val historyViewModel: HistoryViewModel by viewModels()
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
                // nav state: null = Home, "settings" = Settings, "history" = History
                var navTarget by remember { mutableStateOf<String?>(null) }

                BackHandler(enabled = navTarget != null) {
                    navTarget = null
                }

                val transition = updateTransition(targetState = navTarget, label = "nav")

                transition.AnimatedContent(
                    transitionSpec = {
                        if (targetState != null) {
                            // Navigating forward: slide in from right
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            )
                        } else {
                            // Back to Home: slide in from left
                            slideInHorizontally(
                                initialOffsetX = { -it / 4 },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = EaseOutCubic)
                            )
                        }
                    }
                ) { target ->
                    when (target) {
                        "settings" -> {
                            val context = LocalContext.current
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModelFactory(
                                    UserPreferencesRepository(context.applicationContext)
                                )
                            )
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navTarget = null }
                            )
                        }
                        "history" -> {
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onItemClick = { path, posMs ->
                                    val intent = Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                        data = Uri.parse(path)
                                        putExtra(PlayerActivity.EXTRA_RESUME_MS, posMs)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(intent)
                                },
                                onBack = { navTarget = null }
                            )
                        }
                        else -> {
                            HomeScreen(
                                onOpenFile = { filePickerLauncher.launch(arrayOf("video/*")) },
                                onSettingsClick = { navTarget = "settings" },
                                onHistoryClick = { navTarget = "history" }
                            )
                        }
                    }
                }
            }
        }
    }
}

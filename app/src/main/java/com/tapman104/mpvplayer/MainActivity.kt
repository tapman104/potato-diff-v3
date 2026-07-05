package com.tapman104.mpvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

                HomeScreen(
                    onOpenFile = { filePickerLauncher.launch(arrayOf("video/*")) },
                    onSettingsClick = { showSettings = true }
                )

                if (showSettings) {
                    val context = LocalContext.current
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(UserPreferencesRepository(context.applicationContext))
                    )
                    
                    val preferredSubtitleLang by settingsViewModel.subtitleLanguage.collectAsState()
                    val subtitleSize by settingsViewModel.subtitleSize.collectAsState()
                    val subtitlePosition by settingsViewModel.subtitlePosition.collectAsState()
                    val resumePlayback by settingsViewModel.resumePlayback.collectAsState()
                    val decodeMode by settingsViewModel.decodeMode.collectAsState()

                    SettingsScreen(
                        preferredSubtitleLang = preferredSubtitleLang,
                        onSubtitleLangChange = { settingsViewModel.setSubtitleLanguage(it) },
                        subtitleSize = subtitleSize,
                        subtitlePosition = subtitlePosition,
                        onSubtitleSizeChange = { settingsViewModel.setSubtitleSize(it) },
                        onSubtitlePositionChange = { settingsViewModel.setSubtitlePosition(it) },
                        resumePlayback = resumePlayback,
                        onResumePlaybackChange = { settingsViewModel.setResumePlayback(it) },
                        decodeMode = decodeMode,
                        onDecodeModeChange = { settingsViewModel.setDecodeMode(it) },
                        onBack = { showSettings = false }
                    )
                }
            }
        }
    }
}

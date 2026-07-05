package com.tapman104.mpvplayer.player.playback

import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tapman104.mpvplayer.player.viewmodel.PlayerViewModel

@Composable
fun PlayerVideo(viewModel: PlayerViewModel) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(viewModel.controller.surface)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

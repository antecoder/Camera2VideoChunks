package com.sample.android.screens.playback

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import app.learning.mediachunkupload.ui.main.Screen
import app.learning.mediachunkupload.ui.main.navigateTo
import app.learning.mediachunkupload.ui.main.playback.LocalPlaybackManager
import app.learning.mediachunkupload.ui.main.playback.PlaybackManager
import app.learning.mediachunkupload.ui.main.playback.PlaybackViewModel
import app.learning.mediachunkupload.ui.theme.MediaChunkUploadTheme

/**
 * Composable screen which displays media playback.
 */
@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun PlaybackScreen (
    sessionFilePath: String,
    navHostController: NavHostController,
    playbackViewModel: PlaybackViewModel = viewModel()
) {
    val state by playbackViewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    playbackViewModel.onEvent(PlaybackViewModel.Event.ON_COMPLETED)
                }
                else -> {}
            }
        }
    }

    val playbackManager = remember {
        PlaybackManager.Builder(context)
            .apply {
                this.filePath = sessionFilePath
                this.listener = listener
                this.lifecycleOwner = lifecycleOwner
            }
            .build()
    }

    // Handle lifecycle events so that we can relay to the media-player when the screen is paused or closed.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    playbackManager.pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    playbackManager.release()
                    playbackViewModel.onEvent(PlaybackViewModel.Event.ON_CLICK_RELEASE)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalPlaybackManager provides playbackManager) {
        PlaybackScreenContent(state, playbackViewModel::onEvent)
    }

    LaunchedEffect(playbackViewModel) {
        playbackViewModel.effect.collect {
            when (it) {
                PlaybackViewModel.Effect.PAUSE -> playbackManager.pause()
                PlaybackViewModel.Effect.PLAY -> playbackManager.play(true)
                PlaybackViewModel.Effect.RESUME -> playbackManager.play(false)
                PlaybackViewModel.Effect.RELEASE -> playbackManager.release()
                PlaybackViewModel.Effect.CLOSE_SCREEN -> navHostController.navigateTo(Screen.Sessions.route)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlaybackScreenContent(
    state: PlaybackViewModel.PlaybackState,
    onEvent: (PlaybackViewModel.Event) -> Unit
) {
    val playbackManager = LocalPlaybackManager.current
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
            PlayerView(context).apply {
                player = playbackManager.player
            }
        })
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.Black)) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = {
            playbackManager.playerView
        })
        when (state) {
            PlaybackViewModel.PlaybackState.IDLE -> {
                CameraPlayButton(Modifier.align(Alignment.Center)) {
                    onEvent(PlaybackViewModel.Event.ON_CLICK_PLAY)
                }
            }
            PlaybackViewModel.PlaybackState.PAUSED -> {
                CameraPlayButton(Modifier.align(Alignment.Center)) {
                    onEvent(PlaybackViewModel.Event.ON_CLICK_RESUME)
                }
            }
            PlaybackViewModel.PlaybackState.PLAYING -> {
                CameraPauseButton(Modifier.align(Alignment.Center)) {
                    onEvent(PlaybackViewModel.Event.ON_CLICK_PAUSE)
                }
            }
        }
    }
}

@Composable
fun CameraPauseButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledIconButton (
        modifier = Modifier.then(modifier),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Gray, contentColor = Color.White),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = ""
        )
    }
}


@Composable
fun CameraPlayButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledIconButton (
        modifier = Modifier.then(modifier),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Gray, contentColor = Color.White),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = ""
        )
    }


}

@Preview
@Composable
fun CameraPauseButtonPreview() {
    MediaChunkUploadTheme {
        CameraPauseButton {  }
    }
}
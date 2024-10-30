package app.learning.mediachunkupload.ui.main.playback

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * Helper class for coordinating Exoplayer playback.
 */
@UnstableApi
class PlaybackManager private constructor(private val builder: Builder) {

    companion object {
        const val LOG_TAG = "PlaybackManager"
    }

    private val context = builder.context
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            playerView.player = this
            playerView.setShowNextButton(false)
            playerView.setShowFastForwardButton(false)
            playerView.setShowRewindButton(false)
            playerView.setShowPreviousButton(false)
            builder.listener?.let { addListener(it) }

            builder.filePath?.let { filePath ->

                val file = File(filePath)
                if (!file.exists() || !file.canRead()) {
                    throw IllegalArgumentException("Invalid file path: $filePath")
                }
                val playlist = file.listFiles()?.map { recordFile ->
                    MediaItem.fromUri(Uri.fromFile(recordFile))
                } ?: run {
                    throw IllegalArgumentException("Invalid file path: $filePath")
                }
                addMediaItems(playlist)
                prepare()
            }

        }
    }

    val playerView: PlayerView by lazy {
        PlayerView(context).apply {
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
            requestFocus()
        }
    }

    /**
     * Begins/resumes media playback
     *
     * @param resetPlayback If true, will reset the playback to its starting position.
     */
    fun play(resetPlayback: Boolean) {
        if (resetPlayback) {
            player.seekTo(0, 0)
        }
        player.play()
    }

    /**
     * Pauses media playback.
     */
    fun pause() {
        player.pause()
    }

    /**
     * Releases the media player to release resources. This also stops playback.
     */
    fun release() {
        player.stop()
        player.release()
    }

    /**
     * [PlaybackManager] builder class.
     */
    class Builder(val context: Context) {
        var listener: Player.Listener? = null
        var lifecycleOwner: LifecycleOwner? = null
        var filePath: String? = null

        fun build(): PlaybackManager {
            requireNotNull(filePath)
            requireNotNull(lifecycleOwner)
            requireNotNull(listener)
            return PlaybackManager(this)
        }
    }

}

val LocalPlaybackManager =
    compositionLocalOf<PlaybackManager> { error("No playback manager found!") }

package app.learning.mediachunkupload.ui.main.playback

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Playback View. This acts as the media-play state-holder.
 */
class PlaybackViewModel : ViewModel() {

    private val _state: MutableStateFlow<PlaybackState> = MutableStateFlow(PlaybackState.IDLE)
    val state: StateFlow<PlaybackState> = _state

    private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
    val effect:SharedFlow<Effect> = _effect

    /**
     * Relays [Event]'s such as UI-actions to the view model./
     */
    fun onEvent(event: Event) {
        when (event) {
            Event.ON_CLICK_PLAY -> onClickPlayButton(false)
            Event.ON_CLICK_RESUME -> onClickPlayButton(true)
            Event.ON_CLICK_PAUSE -> onClickPauseButton()
            Event.ON_CLICK_RELEASE -> onRelease()
            Event.ON_COMPLETED -> onCompleted()
        }
    }

    private fun onClickPlayButton(resume: Boolean) {
        viewModelScope.launch {
            _effect.emit(if (resume) Effect.RESUME else Effect.PLAY)
        }
        _state.update { PlaybackState.PLAYING }
    }

    private fun onClickPauseButton() {
        viewModelScope.launch {
            _effect.emit(Effect.PAUSE)
        }
        _state.update { PlaybackState.PAUSED }
    }

    private fun onRelease() {
        viewModelScope.launch {
            _effect.emit(Effect.RELEASE)
        }
        _state.update { PlaybackState.IDLE }
    }

    private fun onPrepared() {
        _state.update { PlaybackState.IDLE }
    }

    private fun onCompleted() {
        _state.update { PlaybackState.IDLE }
    }

    /**
     * [PlaybackViewModel] effects on the main player screen, used to share effects with the
     * [PlaybackScreen] and [PlaybackManager]
     */
    enum class Effect {
        CLOSE_SCREEN,
        PAUSE,
        PLAY,
        RESUME,
        RELEASE
    }

    /**
     * Represents the current state of the media player.
     */
    enum class PlaybackState {
        IDLE,
        PLAYING,
        PAUSED
    }

    /**
     * Represents events either from a UI-action on the [PlaybackScreen] or from the media player.
     * Used to trigger state changes.
     */
    enum class Event {
        ON_COMPLETED,
        ON_CLICK_PLAY,
        ON_CLICK_RESUME,
        ON_CLICK_PAUSE,
        ON_CLICK_RELEASE
    }

}
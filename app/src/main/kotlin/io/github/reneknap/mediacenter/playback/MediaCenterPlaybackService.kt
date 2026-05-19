package io.github.reneknap.mediacenter.playback

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Playback service that owns the single ExoPlayer and MediaSession.
 *
 * Lifecycle policy (made explicit; matches Media3 1.5.1 defaults):
 * - Auto-promoted to foreground when the player starts playing.
 * - On pause, Media3 calls stopForeground(STOP_FOREGROUND_DETACH); the
 *   notification stays but becomes dismissable. Resume re-promotes.
 * - On task removal: if nothing is playing, the service stops itself.
 *   If playback is active, the service stays alive in the foreground.
 *
 * Notification surface uses Media3's DefaultMediaNotificationProvider plus
 * a custom-layout shuffle action wired through [SHUFFLE_TOGGLE_ACTION].
 */
@AndroidEntryPoint
class MediaCenterPlaybackService : MediaSessionService() {
    @Inject
    lateinit var playbackController: PlaybackController

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val attributes =
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
        val exo =
            ExoPlayer.Builder(this)
                .setAudioAttributes(attributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
        player = exo
        val session =
            MediaSession.Builder(this, exo)
                .setCallback(MediaCenterCallback())
                .build()
        mediaSession = session
        session.setCustomLayout(ImmutableList.of(shuffleButton(playbackController.status.value.shuffleEnabled)))
        observeShuffleState(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val active = mediaSession?.player
        val isPlaying = active?.playWhenReady == true && active.mediaItemCount > 0
        if (!isPlaying) {
            pauseAllPlayersAndStopSelf()
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun observeShuffleState(session: MediaSession) {
        serviceScope.launch {
            playbackController.status
                .map { it.shuffleEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    session.setCustomLayout(ImmutableList.of(shuffleButton(enabled)))
                }
        }
    }

    private fun shuffleButton(shuffleEnabled: Boolean): CommandButton {
        val icon = if (shuffleEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF
        return CommandButton.Builder(icon)
            .setSessionCommand(SessionCommand(SHUFFLE_TOGGLE_ACTION, Bundle.EMPTY))
            .setDisplayName(SHUFFLE_DISPLAY_NAME)
            .build()
    }

    // Extension point for upcoming Sprint-2 items (Headset/BT, Audio focus).
    // Defaults still forward Play/Pause/Next/Prev/Stop to the underlying Player;
    // we only override the bits we need for the custom shuffle action.
    private inner class MediaCenterCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(SessionCommand(SHUFFLE_TOGGLE_ACTION, Bundle.EMPTY))
                    .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == SHUFFLE_TOGGLE_ACTION) {
                val current = playbackController.status.value.shuffleEnabled
                playbackController.setShuffleEnabled(!current)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    companion object {
        const val SHUFFLE_TOGGLE_ACTION = "io.github.reneknap.mediacenter.SHUFFLE_TOGGLE"
        private const val SHUFFLE_DISPLAY_NAME = "Shuffle"
    }
}

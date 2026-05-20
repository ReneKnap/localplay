package io.github.reneknap.mediacenter.playback

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.core.content.IntentCompat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
 *
 * Media button policy:
 * - Discrete remote keys (PLAY/PAUSE, PLAY_PAUSE, NEXT, PREVIOUS, STOP, from BT remotes,
 *   wired multi-button headsets, and the lockscreen) fall through to Media3's default
 *   player-command handling — see [MediaCenterCallback.onMediaButtonEvent].
 * - The single hook button on one-button headsets ([KeyEvent.KEYCODE_HEADSETHOOK]) is
 *   intercepted for multi-tap: 1 = play/pause, 2 = next, 3 = previous, debounced via
 *   [HeadsetHookMultiTapDetector].
 */
@AndroidEntryPoint
class MediaCenterPlaybackService : MediaSessionService() {
    @Inject
    lateinit var playbackController: PlaybackController

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val multiTapDetector = HeadsetHookMultiTapDetector()
    private var multiTapDispatchJob: Job? = null

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

    private fun scheduleHeadsetHookTap(uptimeMs: Long) {
        multiTapDetector.registerTap(uptimeMs)
        multiTapDispatchJob?.cancel()
        multiTapDispatchJob =
            serviceScope.launch {
                delay(HeadsetHookMultiTapDetector.DEFAULT_WINDOW_MS)
                multiTapDetector.resolveBurst()?.let { dispatchMediaButtonAction(it) }
            }
    }

    private fun dispatchMediaButtonAction(action: MediaButtonAction) {
        when (action) {
            MediaButtonAction.PLAY_PAUSE -> playbackController.togglePlayPause()
            MediaButtonAction.NEXT -> playbackController.next()
            MediaButtonAction.PREVIOUS -> playbackController.previous()
        }
    }

    // Forwards discrete media buttons to the Player defaults; only the custom shuffle action
    // and single-button headset-hook multi-tap are intercepted (see class KDoc).
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

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent,
        ): Boolean {
            val keyEvent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            if (keyEvent?.keyCode != KeyEvent.KEYCODE_HEADSETHOOK) {
                return false
            }
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.repeatCount == 0) {
                scheduleHeadsetHookTap(keyEvent.eventTime)
            }
            return true
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

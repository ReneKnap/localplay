package io.github.reneknap.mediacenter.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

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
 * Item 3 (custom notification) will replace DefaultMediaNotificationProvider.
 * Items 5/6 (headset/audio-focus) hook into MediaCenterCallback.
 */
@AndroidEntryPoint
class MediaCenterPlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

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
        mediaSession =
            MediaSession.Builder(this, exo)
                .setCallback(MediaCenterCallback())
                .build()
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
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    // Extension point for upcoming Sprint-2 items: hook custom commands (e.g., shuffle toggle from
    // the notification) here. Defaults forward Play/Pause/Next/Prev/Stop to the underlying Player.
    private inner class MediaCenterCallback : MediaSession.Callback
}

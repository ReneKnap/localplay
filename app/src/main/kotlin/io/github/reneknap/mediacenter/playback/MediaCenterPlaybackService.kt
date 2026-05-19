package io.github.reneknap.mediacenter.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

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

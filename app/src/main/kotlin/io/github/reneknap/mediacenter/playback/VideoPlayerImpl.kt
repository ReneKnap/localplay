package io.github.reneknap.mediacenter.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.reneknap.mediacenter.data.video.VideoItem
import javax.inject.Inject

/**
 * Screen-scoped video player (ADR-009). Owns its own [ExoPlayer], separate from the audio
 * [MediaCenterPlaybackService]. Built with MOVIE/MEDIA attributes and audio-focus handling, so
 * starting a video pauses background music; [release] is called from the owning ViewModel's
 * `onCleared`. Bound to a `PlayerView` via [player].
 */
class VideoPlayerImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : VideoPlayer {
        private val exoPlayer: ExoPlayer =
            ExoPlayer.Builder(context)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */ true,
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

        override val player: Player
            get() = exoPlayer

        override fun setPlaylist(
            items: List<VideoItem>,
            startIndex: Int,
        ) {
            exoPlayer.setMediaItems(items.map { it.toMediaItem() }, startIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }

        override fun pause() {
            exoPlayer.playWhenReady = false
        }

        override fun release() {
            exoPlayer.release()
        }
    }

private fun VideoItem.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(uri)
        .setUri(uri.toUri())
        .build()

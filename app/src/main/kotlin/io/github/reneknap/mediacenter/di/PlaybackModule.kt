package io.github.reneknap.mediacenter.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.reneknap.mediacenter.playback.MediaEngine
import io.github.reneknap.mediacenter.playback.MediaEngineImpl
import io.github.reneknap.mediacenter.playback.PlaybackController
import io.github.reneknap.mediacenter.playback.PlaybackControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {
    @Binds
    @Singleton
    abstract fun bindMediaEngine(impl: MediaEngineImpl): MediaEngine

    @Binds
    @Singleton
    abstract fun bindPlaybackController(impl: PlaybackControllerImpl): PlaybackController

    companion object {
        // Phase 2 will move ExoPlayer ownership into a MediaSessionService; for v1 the player
        // lives with the process and is never released explicitly.
        @Provides
        @Singleton
        fun provideExoPlayer(
            @ApplicationContext context: Context,
        ): ExoPlayer {
            val attributes =
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build()
            return ExoPlayer.Builder(context)
                .setAudioAttributes(attributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
        }
    }
}

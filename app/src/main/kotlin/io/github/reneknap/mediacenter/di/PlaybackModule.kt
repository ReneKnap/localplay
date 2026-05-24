package io.github.reneknap.mediacenter.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.reneknap.mediacenter.playback.MediaEngine
import io.github.reneknap.mediacenter.playback.MediaEngineImpl
import io.github.reneknap.mediacenter.playback.PlaybackController
import io.github.reneknap.mediacenter.playback.PlaybackControllerImpl
import io.github.reneknap.mediacenter.playback.VideoPlayer
import io.github.reneknap.mediacenter.playback.VideoPlayerImpl
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

    // Intentionally unscoped: a fresh video player per VideoPlayerViewModel, released per screen (ADR-009).
    @Binds
    abstract fun bindVideoPlayer(impl: VideoPlayerImpl): VideoPlayer
}

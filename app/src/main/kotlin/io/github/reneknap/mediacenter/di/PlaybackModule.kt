package io.github.reneknap.mediacenter.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
}

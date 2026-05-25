package io.github.reneknap.mediacenter.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.reneknap.mediacenter.data.audio.ArtworkReader
import io.github.reneknap.mediacenter.data.audio.ArtworkReaderImpl
import io.github.reneknap.mediacenter.data.audio.PlaybackQueue
import io.github.reneknap.mediacenter.data.audio.PlaybackQueueImpl
import io.github.reneknap.mediacenter.data.audio.TagReader
import io.github.reneknap.mediacenter.data.audio.TagReaderImpl
import io.github.reneknap.mediacenter.data.folder.FolderAccess
import io.github.reneknap.mediacenter.data.folder.FolderAccessImpl
import io.github.reneknap.mediacenter.data.folder.FolderPreferencesDataSource
import io.github.reneknap.mediacenter.data.folder.FolderPreferencesDataSourceImpl
import io.github.reneknap.mediacenter.data.folder.FolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderRepositoryImpl
import io.github.reneknap.mediacenter.data.media.MediaFileScanner
import io.github.reneknap.mediacenter.data.media.MediaFileScannerImpl
import io.github.reneknap.mediacenter.data.media.MediaRepository
import io.github.reneknap.mediacenter.data.media.MediaRepositoryImpl
import io.github.reneknap.mediacenter.data.media.MediaScanIndex
import io.github.reneknap.mediacenter.data.media.MediaScanIndexImpl
import io.github.reneknap.mediacenter.data.playback.PlaybackPreferencesDataSource
import io.github.reneknap.mediacenter.data.playback.PlaybackPreferencesDataSourceImpl
import io.github.reneknap.mediacenter.data.review.ReviewPreferencesDataSource
import io.github.reneknap.mediacenter.data.review.ReviewPreferencesDataSourceImpl
import io.github.reneknap.mediacenter.data.theme.ThemePreferencesDataSource
import io.github.reneknap.mediacenter.data.theme.ThemePreferencesDataSourceImpl
import io.github.reneknap.mediacenter.data.video.VideoMetadataReader
import io.github.reneknap.mediacenter.data.video.VideoMetadataReaderImpl
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import kotlin.random.Random

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindFolderPreferencesDataSource(impl: FolderPreferencesDataSourceImpl): FolderPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindFolderAccess(impl: FolderAccessImpl): FolderAccess

    @Binds
    @Singleton
    abstract fun bindMediaFileScanner(impl: MediaFileScannerImpl): MediaFileScanner

    @Binds
    @Singleton
    abstract fun bindVideoMetadataReader(impl: VideoMetadataReaderImpl): VideoMetadataReader

    @Binds
    @Singleton
    abstract fun bindTagReader(impl: TagReaderImpl): TagReader

    @Binds
    @Singleton
    abstract fun bindArtworkReader(impl: ArtworkReaderImpl): ArtworkReader

    @Binds
    @Singleton
    abstract fun bindMediaScanIndex(impl: MediaScanIndexImpl): MediaScanIndex

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackQueue(impl: PlaybackQueueImpl): PlaybackQueue

    @Binds
    @Singleton
    abstract fun bindPlaybackPreferencesDataSource(
        impl: PlaybackPreferencesDataSourceImpl,
    ): PlaybackPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindThemePreferencesDataSource(impl: ThemePreferencesDataSourceImpl): ThemePreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindReviewPreferencesDataSource(impl: ReviewPreferencesDataSourceImpl): ReviewPreferencesDataSource

    companion object {
        @Provides
        @Singleton
        fun provideRandom(): Random = Random.Default

        @Provides
        @Singleton
        fun provideDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("folders") },
            )

        @Provides
        @Singleton
        fun provideJson(): Json =
            Json {
                ignoreUnknownKeys = true
            }
    }
}

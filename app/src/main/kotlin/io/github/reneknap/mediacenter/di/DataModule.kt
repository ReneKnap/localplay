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
import io.github.reneknap.mediacenter.data.folder.FolderAccess
import io.github.reneknap.mediacenter.data.folder.FolderAccessImpl
import io.github.reneknap.mediacenter.data.folder.FolderPreferencesDataSource
import io.github.reneknap.mediacenter.data.folder.FolderPreferencesDataSourceImpl
import io.github.reneknap.mediacenter.data.folder.FolderRepository
import io.github.reneknap.mediacenter.data.folder.FolderRepositoryImpl
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindFolderPreferencesDataSource(
        impl: FolderPreferencesDataSourceImpl,
    ): FolderPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindFolderAccess(impl: FolderAccessImpl): FolderAccess

    companion object {

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
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
        }
    }
}

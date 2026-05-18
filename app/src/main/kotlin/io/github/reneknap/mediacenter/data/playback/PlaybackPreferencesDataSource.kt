package io.github.reneknap.mediacenter.data.playback

import kotlinx.coroutines.flow.Flow

interface PlaybackPreferencesDataSource {
    val shuffleEnabled: Flow<Boolean>

    suspend fun setShuffleEnabled(enabled: Boolean)
}

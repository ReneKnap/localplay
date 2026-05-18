package io.github.reneknap.mediacenter.data.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlaybackPreferencesDataSource(
    initial: Boolean = false,
) : PlaybackPreferencesDataSource {
    private val state = MutableStateFlow(initial)

    override val shuffleEnabled: Flow<Boolean> = state.asStateFlow()

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        state.value = enabled
    }
}

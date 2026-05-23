package io.github.reneknap.mediacenter.data.review

import kotlinx.coroutines.flow.Flow

interface ReviewPreferencesDataSource {
    val appStartCount: Flow<Int>
    val supportHintDismissed: Flow<Boolean>

    suspend fun incrementAppStartCount()

    suspend fun markSupportHintDismissed()
}

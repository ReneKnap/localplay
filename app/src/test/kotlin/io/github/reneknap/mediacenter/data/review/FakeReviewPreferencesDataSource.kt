package io.github.reneknap.mediacenter.data.review

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeReviewPreferencesDataSource(
    initialCount: Int = 0,
    initialDismissed: Boolean = false,
) : ReviewPreferencesDataSource {
    private val count = MutableStateFlow(initialCount)
    private val dismissed = MutableStateFlow(initialDismissed)

    override val appStartCount: Flow<Int> = count.asStateFlow()
    override val supportHintDismissed: Flow<Boolean> = dismissed.asStateFlow()

    override suspend fun incrementAppStartCount() {
        count.value += 1
    }

    override suspend fun markSupportHintDismissed() {
        dismissed.value = true
    }
}

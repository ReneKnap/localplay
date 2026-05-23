package io.github.reneknap.mediacenter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.reneknap.mediacenter.data.review.ReviewPreferencesDataSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportHintViewModel
    @Inject
    constructor(
        private val reviewPreferences: ReviewPreferencesDataSource,
    ) : ViewModel() {
        val showSupportHint: StateFlow<Boolean> =
            combine(
                reviewPreferences.appStartCount,
                reviewPreferences.supportHintDismissed,
            ) { count, dismissed ->
                count >= HINT_THRESHOLD && !dismissed
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = false,
            )

        init {
            viewModelScope.launch {
                val dismissed = reviewPreferences.supportHintDismissed.first()
                val count = reviewPreferences.appStartCount.first()
                if (!dismissed && count < HINT_THRESHOLD) {
                    reviewPreferences.incrementAppStartCount()
                }
            }
        }

        fun onSupportMenuOpened() {
            viewModelScope.launch {
                reviewPreferences.markSupportHintDismissed()
            }
        }

        private companion object {
            const val HINT_THRESHOLD = 8
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

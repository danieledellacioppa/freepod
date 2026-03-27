package com.forteur.freepod.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.forteur.freepod.data.SubscriptionRepository
import com.forteur.freepod.model.SubscribedPodcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MyPodcastsUiState(
    val podcasts: List<SubscribedPodcast> = emptyList()
)

class MyPodcastsViewModel(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPodcastsUiState())
    val uiState: StateFlow<MyPodcastsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = MyPodcastsUiState(
            podcasts = subscriptionRepository.getSubscribedPodcasts()
        )
    }

    fun unsubscribe(feedUrl: String) {
        subscriptionRepository.unsubscribe(feedUrl)
        refresh()
    }

    companion object {
        fun factory(subscriptionRepository: SubscriptionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MyPodcastsViewModel(subscriptionRepository) as T
                }
            }
        }
    }
}

package com.forteur.freepod.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forteur.freepod.data.PodcastIndexService
import com.forteur.freepod.data.SubscriptionRepository
import com.forteur.freepod.model.PodcastSummary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val podcasts: List<PodcastSummary> = emptyList(),
    val errorMessage: String? = null,
    val selectedPodcast: PodcastSummary? = null
)

@OptIn(FlowPreview::class)
class DiscoverViewModel(
    private val podcastIndexService: PodcastIndexService,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(DiscoverUiState(isLoading = true))
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadTrending()
        viewModelScope.launch {
            queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        loadTrending()
                    } else {
                        search(query)
                    }
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        queryFlow.value = newQuery
    }

    fun selectPodcast(podcast: PodcastSummary) {
        _uiState.value = _uiState.value.copy(selectedPodcast = podcast)
    }

    fun clearSelectedPodcast() {
        _uiState.value = _uiState.value.copy(selectedPodcast = null)
    }

    fun subscribeSelectedPodcast() {
        val selected = _uiState.value.selectedPodcast ?: return
        subscriptionRepository.subscribe(selected)
    }

    fun isSelectedPodcastSubscribed(): Boolean {
        val selected = _uiState.value.selectedPodcast ?: return false
        return subscriptionRepository.isSubscribed(selected.feedUrl)
    }

    fun retry() {
        val query = _uiState.value.query
        if (query.isBlank()) loadTrending() else search(query)
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { podcastIndexService.trendingPodcasts() }
                .onSuccess { podcasts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        podcasts = podcasts,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        podcasts = emptyList(),
                        errorMessage = error.message ?: "Errore durante il caricamento discover"
                    )
                }
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { podcastIndexService.searchPodcasts(query) }
                .onSuccess { podcasts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        podcasts = podcasts,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        podcasts = emptyList(),
                        errorMessage = error.message ?: "Errore durante la ricerca"
                    )
                }
        }
    }

    companion object {
        fun factory(
            podcastIndexService: PodcastIndexService,
            subscriptionRepository: SubscriptionRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DiscoverViewModel(podcastIndexService, subscriptionRepository) as T
                }
            }
        }
    }
}

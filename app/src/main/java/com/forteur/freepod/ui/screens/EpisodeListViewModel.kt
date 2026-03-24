package com.forteur.freepod.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forteur.freepod.data.PodcastRepository
import com.forteur.freepod.model.PodcastEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EpisodeListUiState(
    val podcastTitle: String = "",
    val feedUrl: String = "",
    val isLoading: Boolean = false,
    val episodes: List<PodcastEpisode> = emptyList(),
    val errorMessage: String? = null
)

class EpisodeListViewModel(
    private val repository: PodcastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeListUiState())
    val uiState: StateFlow<EpisodeListUiState> = _uiState.asStateFlow()

    fun loadEpisodes(feedUrl: String, podcastTitle: String) {
        if (feedUrl.isBlank()) return
        viewModelScope.launch {
            _uiState.value = EpisodeListUiState(
                podcastTitle = podcastTitle,
                feedUrl = feedUrl,
                isLoading = true
            )
            runCatching { repository.fetchEpisodes(feedUrl) }
                .onSuccess { episodes ->
                    _uiState.value = EpisodeListUiState(
                        podcastTitle = podcastTitle,
                        feedUrl = feedUrl,
                        isLoading = false,
                        episodes = episodes
                    )
                }
                .onFailure { error ->
                    _uiState.value = EpisodeListUiState(
                        podcastTitle = podcastTitle,
                        feedUrl = feedUrl,
                        isLoading = false,
                        errorMessage = error.message ?: "Errore sconosciuto durante il caricamento"
                    )
                }
        }
    }

    fun findEpisodeByAudioUrl(audioUrl: String): PodcastEpisode? {
        return _uiState.value.episodes.firstOrNull { it.audioUrl == audioUrl }
    }

    companion object {
        fun factory(repository: PodcastRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EpisodeListViewModel(repository) as T
                }
            }
        }
    }
}

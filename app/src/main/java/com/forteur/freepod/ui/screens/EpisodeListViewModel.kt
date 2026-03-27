package com.forteur.freepod.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forteur.freepod.data.PodcastRepository
import com.forteur.freepod.model.PodcastEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.forteur.freepod.util.LOG_TAG_FEED
import com.forteur.freepod.util.LOG_TAG_UI

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
        if (feedUrl.isBlank()) {
            Log.e(LOG_TAG_FEED, "loadEpisodes aborted: feedUrl blank | podcastTitle=$podcastTitle")
            return
        }
        Log.d(LOG_TAG_FEED, "loadEpisodes start | feedUrl=$feedUrl, podcastTitle=$podcastTitle")
        viewModelScope.launch {
            _uiState.value = EpisodeListUiState(
                podcastTitle = podcastTitle,
                feedUrl = feedUrl,
                isLoading = true
            )
            runCatching { repository.fetchEpisodes(feedUrl) }
                .onSuccess { episodes ->
                    Log.d(
                        LOG_TAG_FEED,
                        "loadEpisodes success | feedUrl=$feedUrl, podcastTitle=$podcastTitle, episodeCount=${episodes.size}"
                    )
                    _uiState.value = EpisodeListUiState(
                        podcastTitle = podcastTitle,
                        feedUrl = feedUrl,
                        isLoading = false,
                        episodes = episodes
                    )
                }
                .onFailure { error ->
                    Log.e(
                        LOG_TAG_FEED,
                        "loadEpisodes failure | feedUrl=$feedUrl, podcastTitle=$podcastTitle, error=${error.message}",
                        error
                    )
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
        val found = _uiState.value.episodes.firstOrNull { it.audioUrl == audioUrl }
        if (found == null) {
            Log.e(
                LOG_TAG_UI,
                "findEpisodeByAudioUrl failed | audioUrl=$audioUrl, episodesInMemory=${_uiState.value.episodes.size}, feedUrl=${_uiState.value.feedUrl}"
            )
        } else {
            Log.d(
                LOG_TAG_UI,
                "findEpisodeByAudioUrl success | audioUrl=$audioUrl, title=${found.title}, feedUrl=${_uiState.value.feedUrl}"
            )
        }
        return found
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

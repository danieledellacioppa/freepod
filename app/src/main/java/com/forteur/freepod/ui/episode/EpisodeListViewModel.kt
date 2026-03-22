package com.forteur.freepod.ui.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forteur.freepod.data.PodcastRepository
import com.forteur.freepod.model.PodcastEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EpisodeListUiState(
    val isLoading: Boolean = true,
    val episodes: List<PodcastEpisode> = emptyList(),
    val errorMessage: String? = null,
    val podcastName: String = "Easy Catalan"
)

class EpisodeListViewModel(
    private val repository: PodcastRepository = PodcastRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeListUiState())
    val uiState: StateFlow<EpisodeListUiState> = _uiState.asStateFlow()

    init {
        loadEpisodes()
    }

    fun loadEpisodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                repository.fetchEpisodes()
            }.onSuccess { episodes ->
                _uiState.value = EpisodeListUiState(
                    isLoading = false,
                    episodes = episodes,
                    podcastName = "Easy Catalan"
                )
            }.onFailure { error ->
                _uiState.value = EpisodeListUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Errore sconosciuto"
                )
            }
        }
    }
}

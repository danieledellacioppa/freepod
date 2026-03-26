package com.forteur.freepod.data

import com.forteur.freepod.model.PodcastSummary

interface PodcastDiscoveryService {
    suspend fun trendingPodcasts(maxResults: Int = 30): List<PodcastSummary>
    suspend fun searchPodcasts(query: String, maxResults: Int = 30): List<PodcastSummary>
}

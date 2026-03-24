package com.forteur.freepod.model

data class PodcastSummary(
    val podcastIndexId: Long?,
    val title: String,
    val author: String?,
    val imageUrl: String?,
    val description: String?,
    val feedUrl: String
)

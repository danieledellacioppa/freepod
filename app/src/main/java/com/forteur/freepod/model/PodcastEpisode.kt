package com.forteur.freepod.model

data class PodcastEpisode(
    val title: String,
    val description: String,
    val pubDate: String?,
    val audioUrl: String,
    val duration: String?
)

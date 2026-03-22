package com.forteur.freepod.model

data class PodcastEpisode(
    val title: String,
    val description: String?,
    val pubDateRaw: String?,
    val audioUrl: String,
    val duration: String?
)

package com.forteur.freepod.model

data class SubscribedPodcast(
    val title: String,
    val feedUrl: String,
    val imageUrl: String?,
    val author: String?
)

package com.forteur.freepod.data

import com.forteur.freepod.BuildConfig

object PodcastIndexConfig {
    const val BASE_URL = "https://api.podcastindex.org/api/1.0"

    // Imposta queste credenziali dal portale Podcast Index.
    // In produzione è consigliato non lasciare chiavi in chiaro dentro l'app.
    const val API_KEY: String = BuildConfig.PODCAST_INDEX_KEY
    const val API_SECRET: String = BuildConfig.PODCAST_INDEX_SECRET

}

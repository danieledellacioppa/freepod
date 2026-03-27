package com.forteur.freepod.data

enum class DiscoveryProvider {
    ITUNES,
    PODCAST_INDEX
}

object DiscoveryProviderConfig {
    // Provider attivo al momento: iTunes.
    // Per tornare a Podcast Index, impostare PODCAST_INDEX.
    val ACTIVE_PROVIDER = DiscoveryProvider.ITUNES
}

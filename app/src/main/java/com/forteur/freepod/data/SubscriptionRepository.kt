package com.forteur.freepod.data

import com.forteur.freepod.model.PodcastSummary
import com.forteur.freepod.model.SubscribedPodcast

class SubscriptionRepository(
    private val localDataSource: SubscriptionLocalDataSource
) {
    fun getSubscribedPodcasts(): List<SubscribedPodcast> = localDataSource.getAllSubscriptions()

    fun subscribe(podcast: PodcastSummary) {
        localDataSource.saveSubscription(
            SubscribedPodcast(
                title = podcast.title,
                feedUrl = podcast.feedUrl,
                imageUrl = podcast.imageUrl,
                author = podcast.author
            )
        )
    }

    fun isSubscribed(feedUrl: String): Boolean = localDataSource.isSubscribed(feedUrl)

    fun unsubscribe(feedUrls: Set<String>) {
        localDataSource.removeSubscriptions(feedUrls)
    }
}

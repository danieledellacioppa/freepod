package com.forteur.freepod.data

import android.content.Context
import com.forteur.freepod.model.SubscribedPodcast
import org.json.JSONArray
import org.json.JSONObject

class SubscriptionLocalDataSource(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllSubscriptions(): List<SubscribedPodcast> {
        val raw = prefs.getString(KEY_SUBSCRIPTIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue
                add(
                    SubscribedPodcast(
                        title = json.optString(KEY_TITLE).ifBlank { "Untitled podcast" },
                        feedUrl = json.optString(KEY_FEED_URL),
                        imageUrl = json.optString(KEY_IMAGE).ifBlank { null },
                        author = json.optString(KEY_AUTHOR).ifBlank { null }
                    )
                )
            }
        }
    }

    fun saveSubscription(subscription: SubscribedPodcast) {
        val updated = getAllSubscriptions().toMutableList()
        val existingIndex = updated.indexOfFirst { it.feedUrl == subscription.feedUrl }
        if (existingIndex >= 0) {
            updated[existingIndex] = subscription
        } else {
            updated.add(subscription)
        }
        persistSubscriptions(updated)
    }

    fun isSubscribed(feedUrl: String): Boolean {
        return getAllSubscriptions().any { it.feedUrl == feedUrl }
    }

    fun removeSubscriptions(feedUrls: Set<String>) {
        if (feedUrls.isEmpty()) return
        val updated = getAllSubscriptions().filterNot { it.feedUrl in feedUrls }
        persistSubscriptions(updated)
    }

    private fun persistSubscriptions(subscriptions: List<SubscribedPodcast>) {
        val jsonArray = JSONArray()
        subscriptions.forEach { item ->
            jsonArray.put(
                JSONObject().apply {
                    put(KEY_TITLE, item.title)
                    put(KEY_FEED_URL, item.feedUrl)
                    put(KEY_IMAGE, item.imageUrl)
                    put(KEY_AUTHOR, item.author)
                }
            )
        }
        prefs.edit().putString(KEY_SUBSCRIPTIONS, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "freepod_subscriptions"
        private const val KEY_SUBSCRIPTIONS = "subscriptions"
        private const val KEY_TITLE = "title"
        private const val KEY_FEED_URL = "feedUrl"
        private const val KEY_IMAGE = "image"
        private const val KEY_AUTHOR = "author"
    }
}

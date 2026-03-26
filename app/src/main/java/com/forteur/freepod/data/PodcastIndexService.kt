package com.forteur.freepod.data

import com.forteur.freepod.model.PodcastSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PodcastIndexService(
    private val client: OkHttpClient
) : PodcastDiscoveryService {

    override suspend fun trendingPodcasts(maxResults: Int): List<PodcastSummary> = withContext(Dispatchers.IO) {
        val url = "${PodcastIndexConfig.BASE_URL}/podcasts/trending"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("max", maxResults.toString())
            .build()

        executePodcastListRequest(url.toString())
    }

    override suspend fun searchPodcasts(query: String, maxResults: Int): List<PodcastSummary> = withContext(Dispatchers.IO) {
        val url = "${PodcastIndexConfig.BASE_URL}/search/byterm"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("max", maxResults.toString())
            .build()

        executePodcastListRequest(url.toString())
    }

    private fun executePodcastListRequest(url: String): List<PodcastSummary> {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Podcast Index error HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val feeds = root.optJSONArray("feeds") ?: JSONArray()
            return parsePodcasts(feeds)
        }
    }

    private fun parsePodcasts(feeds: JSONArray): List<PodcastSummary> {
        val podcasts = mutableListOf<PodcastSummary>()
        for (i in 0 until feeds.length()) {
            val item = feeds.optJSONObject(i) ?: continue

            val feedUrl = item.optString("url")
            if (feedUrl.isBlank()) continue

            podcasts.add(
                PodcastSummary(
                    podcastIndexId = item.optLong("id").takeIf { it != 0L },
                    title = item.optString("title").ifBlank { "Untitled podcast" },
                    author = item.optString("author").ifBlank { null },
                    imageUrl = item.optString("image").ifBlank { null },
                    description = item.optString("description").ifBlank { null },
                    feedUrl = feedUrl
                )
            )
        }
        return podcasts
    }
}

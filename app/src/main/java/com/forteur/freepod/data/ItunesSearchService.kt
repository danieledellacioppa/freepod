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

class ItunesSearchService(
    private val client: OkHttpClient
) : PodcastDiscoveryService {

    override suspend fun trendingPodcasts(maxResults: Int): List<PodcastSummary> {
        // iTunes Search API non espone un endpoint "trending" dedicato.
        // Usiamo una query generica per popolare la discover iniziale.
        return searchPodcasts(query = DEFAULT_DISCOVER_QUERY, maxResults = maxResults)
    }

    override suspend fun searchPodcasts(query: String, maxResults: Int): List<PodcastSummary> = withContext(Dispatchers.IO) {
        val url = BASE_URL
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("media", "podcast")
            .addQueryParameter("entity", "podcast")
            .addQueryParameter("term", query)
            .addQueryParameter("limit", maxResults.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("iTunes Search API error HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: JSONArray()
            parsePodcasts(results)
        }
    }

    private fun parsePodcasts(results: JSONArray): List<PodcastSummary> {
        val podcasts = mutableListOf<PodcastSummary>()
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val feedUrl = item.optString("feedUrl")
            if (feedUrl.isBlank()) continue

            podcasts.add(
                PodcastSummary(
                    podcastIndexId = item.optLong("collectionId").takeIf { it != 0L },
                    title = item.optString("collectionName").ifBlank { "Untitled podcast" },
                    author = item.optString("artistName").ifBlank { null },
                    imageUrl = item.optString("artworkUrl600")
                        .ifBlank { item.optString("artworkUrl100") }
                        .ifBlank { null },
                    description = item.optString("primaryGenreName").ifBlank { null },
                    feedUrl = feedUrl
                )
            )
        }
        return podcasts
    }

    companion object {
        private const val BASE_URL = "https://itunes.apple.com/search"
        private const val DEFAULT_DISCOVER_QUERY = "podcast"
    }
}

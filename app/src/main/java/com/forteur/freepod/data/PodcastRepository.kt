package com.forteur.freepod.data

import android.util.Log
import com.forteur.freepod.model.PodcastEpisode
import com.forteur.freepod.util.LOG_TAG_FEED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class PodcastRepository(
    private val httpClient: OkHttpClient,
    private val parser: RssFeedParser = RssFeedParser()
) {

    suspend fun fetchEpisodes(feedUrl: String): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        Log.d(LOG_TAG_FEED, "fetchEpisodes request start | feedUrl=$feedUrl")
        val request = Request.Builder()
            .url(feedUrl)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(LOG_TAG_FEED, "fetchEpisodes HTTP error | feedUrl=$feedUrl, code=${response.code}")
                throw IOException("Errore HTTP ${response.code}")
            }

            val body = response.body ?: throw IOException("Risposta RSS vuota")
            val episodes = body.byteStream().use { stream -> parser.parse(stream) }
            Log.d(LOG_TAG_FEED, "fetchEpisodes parsed | feedUrl=$feedUrl, parsedCount=${episodes.size}")
            episodes.sortedByDescending { parseRssDate(it.pubDateRaw) ?: 0L }
        }
    }

    private fun parseRssDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                .parse(value)
                ?.time
        } catch (_: ParseException) {
            null
        }
    }
}

package com.forteur.freepod.data

import com.forteur.freepod.model.PodcastEpisode
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

    companion object {
        // Cambia questo valore con il feed RSS che vuoi usare.
        const val PODCAST_FEED_URL = "INSERISCI_QUI_L_URL_RSS_DI_EASY_CATALAN"
    }

    suspend fun fetchEpisodes(): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(PODCAST_FEED_URL)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Errore HTTP ${response.code}")
            }

            val body = response.body ?: throw IOException("Risposta RSS vuota")
            val episodes = body.byteStream().use { stream -> parser.parse(stream) }
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

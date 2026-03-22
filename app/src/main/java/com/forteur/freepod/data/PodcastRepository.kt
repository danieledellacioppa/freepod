package com.forteur.freepod.data

import android.text.Html
import com.forteur.freepod.model.PodcastEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


private const val PODCAST_FEED_URL = "INSERISCI_QUI_L_URL_RSS_DI_EASY_CATALAN"

class PodcastRepository(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun fetchEpisodes(): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(PODCAST_FEED_URL).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Errore HTTP ${response.code}")
            }

            val body = response.body?.string() ?: throw IllegalStateException("Feed RSS vuoto")
            parseRss(body).sortedByDescending { parsePubDate(it.pubDate) ?: Instant.EPOCH }
        }
    }

    private fun parseRss(xml: String): List<PodcastEpisode> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }

        val episodes = mutableListOf<PodcastEpisode>()
        var eventType = parser.eventType
        var currentTitle = ""
        var currentDescription = ""
        var currentPubDate: String? = null
        var currentAudioUrl = ""
        var currentDuration: String? = null
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            insideItem = true
                            currentTitle = ""
                            currentDescription = ""
                            currentPubDate = null
                            currentAudioUrl = ""
                            currentDuration = null
                        }

                        "title" -> if (insideItem) {
                            currentTitle = parser.nextText().trim()
                        }

                        "description" -> if (insideItem) {
                            currentDescription = cleanDescription(parser.nextText())
                        }

                        "pubDate" -> if (insideItem) {
                            currentPubDate = parser.nextText().trim()
                        }

                        "enclosure" -> if (insideItem) {
                            currentAudioUrl = parser.getAttributeValue(null, "url")?.trim().orEmpty()
                        }

                        "itunes:duration", "duration" -> if (insideItem) {
                            currentDuration = parser.nextText().trim().ifBlank { null }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        insideItem = false
                        if (currentAudioUrl.isNotBlank()) {
                            episodes += PodcastEpisode(
                                title = currentTitle.ifBlank { "Episodio senza titolo" },
                                description = currentDescription,
                                pubDate = currentPubDate,
                                audioUrl = currentAudioUrl,
                                duration = currentDuration
                            )
                        }
                    }
                }
            }

            eventType = parser.next()
        }

        return episodes
    }

    private fun parsePubDate(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        }.getOrNull()
    }

    private fun cleanDescription(raw: String): String {
        return Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}

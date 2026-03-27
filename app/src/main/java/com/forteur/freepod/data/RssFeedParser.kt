package com.forteur.freepod.data

import android.util.Log
import android.util.Xml
import com.forteur.freepod.model.PodcastEpisode
import com.forteur.freepod.util.LOG_TAG_FEED
import com.forteur.freepod.util.stripHtml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class RssFeedParser {

    fun parse(inputStream: InputStream): List<PodcastEpisode> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val episodes = mutableListOf<PodcastEpisode>()
        var eventType = parser.eventType

        var currentTitle: String? = null
        var currentDescription: String? = null
        var currentPubDate: String? = null
        var currentAudioUrl: String? = null
        var currentDuration: String? = null
        var itemIndex = 0
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            insideItem = true
                            itemIndex += 1
                            currentTitle = null
                            currentDescription = null
                            currentPubDate = null
                            currentAudioUrl = null
                            currentDuration = null
                        }

                        "title" -> if (insideItem) currentTitle = readMixedContentText(parser)
                        "description" -> if (insideItem) {
                            currentDescription = stripHtml(readMixedContentText(parser))
                        }
                        "pubDate" -> if (insideItem) currentPubDate = readMixedContentText(parser)
                        "enclosure" -> if (insideItem) {
                            currentAudioUrl = parser.getAttributeValue(null, "url")?.trim()
                            if (currentAudioUrl.isNullOrBlank()) {
                                Log.w(
                                    LOG_TAG_FEED,
                                    "RSS item[$itemIndex] has missing/blank enclosure url"
                                )
                            }
                        }
                        "itunes:duration", "duration" -> if (insideItem) {
                            currentDuration = readMixedContentText(parser)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        insideItem = false
                        val audioUrl = currentAudioUrl
                        if (currentTitle.isNullOrBlank()) {
                            Log.w(LOG_TAG_FEED, "RSS item[$itemIndex] missing title")
                        }
                        if (!audioUrl.isNullOrBlank()) {
                            if (!audioUrl.startsWith("http", ignoreCase = true)) {
                                Log.w(
                                    LOG_TAG_FEED,
                                    "RSS item[$itemIndex] enclosure looks invalid | audioUrl=$audioUrl"
                                )
                            }
                            episodes.add(
                                PodcastEpisode(
                                    title = currentTitle?.ifBlank { "Untitled episode" }
                                        ?: "Untitled episode",
                                    description = currentDescription,
                                    pubDateRaw = currentPubDate,
                                    audioUrl = audioUrl,
                                    duration = currentDuration
                                )
                            )
                        } else {
                            Log.e(
                                LOG_TAG_FEED,
                                "RSS item[$itemIndex] skipped due to missing audioUrl | title=${currentTitle.orEmpty()}"
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return episodes
    }

    private fun readMixedContentText(parser: XmlPullParser): String {
        val startDepth = parser.depth
        val builder = StringBuilder()

        while (true) {
            when (parser.next()) {
                XmlPullParser.TEXT,
                XmlPullParser.CDSECT,
                XmlPullParser.ENTITY_REF -> builder.append(parser.text)

                XmlPullParser.END_TAG -> {
                    if (parser.depth == startDepth) {
                        return builder.toString().trim()
                    }
                }

                XmlPullParser.END_DOCUMENT -> return builder.toString().trim()
            }
        }
    }
}

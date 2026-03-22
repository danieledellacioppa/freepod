package com.forteur.freepod.data

import android.util.Xml
import com.forteur.freepod.model.PodcastEpisode
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
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            insideItem = true
                            currentTitle = null
                            currentDescription = null
                            currentPubDate = null
                            currentAudioUrl = null
                            currentDuration = null
                        }

                        "title" -> if (insideItem) currentTitle = parser.nextText().trim()
                        "description" -> if (insideItem) currentDescription = stripHtml(parser.nextText())
                        "pubDate" -> if (insideItem) currentPubDate = parser.nextText().trim()
                        "enclosure" -> if (insideItem) {
                            currentAudioUrl = parser.getAttributeValue(null, "url")?.trim()
                        }
                        "itunes:duration", "duration" -> if (insideItem) {
                            currentDuration = parser.nextText().trim()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        insideItem = false
                        val audioUrl = currentAudioUrl
                        if (!audioUrl.isNullOrBlank()) {
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
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return episodes
    }
}

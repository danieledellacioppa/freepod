package com.forteur.freepod.util

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import java.util.UUID

const val LOG_TAG_NAV = "FREEPOD_NAV"
const val LOG_TAG_PLAYER = "FREEPOD_PLAYER"
const val LOG_TAG_SERVICE = "FREEPOD_SERVICE"
const val LOG_TAG_CONTROLLER = "FREEPOD_CONTROLLER"
const val LOG_TAG_FEED = "FREEPOD_FEED"
const val LOG_TAG_UI = "FREEPOD_UI"

const val EXTRA_PLAY_REQUEST_ID = "playRequestId"
const val EXTRA_FEED_URL = "feedUrl"
const val EXTRA_PODCAST_TITLE = "podcastTitle"
const val EXTRA_IMAGE_URL = "imageUrl"

fun newPlayRequestId(): String = UUID.randomUUID().toString().take(8)

fun playerStateToString(state: Int): String = when (state) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "UNKNOWN($state)"
}

fun safeMediaMetadataSummary(metadata: MediaMetadata?): String {
    if (metadata == null) return "metadata=null"
    return "title=${metadata.title}, artist=${metadata.artist}, artworkUri=${metadata.artworkUri}, albumTitle=${metadata.albumTitle}, description=${metadata.description}"
}

fun safeMediaItemSummary(mediaItem: MediaItem?): String {
    if (mediaItem == null) return "mediaItem=null"
    return "mediaId=${mediaItem.mediaId}, uri=${mediaItem.localConfiguration?.uri}, ${safeMediaMetadataSummary(mediaItem.mediaMetadata)}"
}

fun bundleSummary(bundle: Bundle?): String {
    if (bundle == null) return "extras=null"
    val keys = bundle.keySet().joinToString(",")
    return "extrasKeys=[$keys], playRequestId=${bundle.getString(EXTRA_PLAY_REQUEST_ID)}"
}

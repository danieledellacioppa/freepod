package com.forteur.freepod.util

import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.forteur.freepod.BuildConfig
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
private const val DEBUG_LOG_LEVEL = 1

fun newPlayRequestId(): String = UUID.randomUUID().toString().take(8)

fun playerStateToString(state: Int): String = when (state) {
    Player.STATE_IDLE -> "IDLE"
    Player.STATE_BUFFERING -> "BUFFERING"
    Player.STATE_READY -> "READY"
    Player.STATE_ENDED -> "ENDED"
    else -> "UNKNOWN($state)"
}

fun playbackSuppressionReasonToString(reason: Int): String = when (reason) {
    Player.PLAYBACK_SUPPRESSION_REASON_NONE -> "NONE"
    Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS -> "TRANSIENT_AUDIO_FOCUS_LOSS"
    else -> "UNKNOWN($reason)"
}

fun playWhenReadyChangeReasonToString(reason: Int): String = when (reason) {
    Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> "USER_REQUEST"
    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> "AUDIO_FOCUS_LOSS"
    Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> "AUDIO_BECOMING_NOISY"
    Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> "REMOTE"
    Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> "END_OF_MEDIA_ITEM"
    Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG -> "SUPPRESSED_TOO_LONG"
    else -> "UNKNOWN($reason)"
}

fun safeMediaMetadataSummary(metadata: MediaMetadata?): String {
    if (metadata == null) return "metadata=null"
    val description = metadata.description?.toString().orEmpty()
    val descriptionSummary = if (description.isBlank()) {
        "description=<empty>"
    } else if (isDebugLogLevelEnabled(2)) {
        "description=${description}"
    } else {
        "description=<omitted,len=${description.length}>"
    }
    return "title=${metadata.title}, artist=${metadata.artist}, artworkUri=${metadata.artworkUri}, albumTitle=${metadata.albumTitle}, $descriptionSummary"
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

fun isDebugLogLevelEnabled(level: Int): Boolean = BuildConfig.DEBUG && level <= DEBUG_LOG_LEVEL

fun debugLog(tag: String, message: String, level: Int = 1) {
    if (isDebugLogLevelEnabled(level)) {
        Log.d(tag, message)
    }
}

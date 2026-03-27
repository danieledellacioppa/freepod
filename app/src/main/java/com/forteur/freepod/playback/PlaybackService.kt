package com.forteur.freepod.playback

import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.forteur.freepod.util.LOG_TAG_SERVICE
import com.forteur.freepod.util.bundleSummary
import com.forteur.freepod.util.debugLog
import com.forteur.freepod.util.playbackSuppressionReasonToString
import com.forteur.freepod.util.playWhenReadyChangeReasonToString
import com.forteur.freepod.util.playerStateToString
import com.forteur.freepod.util.safeMediaItemSummary

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            debugLog(
                LOG_TAG_SERVICE,
                "Service player onPlaybackStateChanged | state=${playerStateToString(playbackState)}($playbackState), current=${safeMediaItemSummary(player?.currentMediaItem)}"
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val activePlayer = player
            debugLog(
                LOG_TAG_SERVICE,
                "Service player onIsPlayingChanged | isPlaying=$isPlaying, playbackState=${activePlayer?.playbackState?.let(::playerStateToString)}, playWhenReady=${activePlayer?.playWhenReady}, suppressionReason=${activePlayer?.playbackSuppressionReason?.let(::playbackSuppressionReasonToString)}, currentPosition=${activePlayer?.currentPosition}, bufferedPosition=${activePlayer?.bufferedPosition}"
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            debugLog(
                LOG_TAG_SERVICE,
                "Service player onMediaItemTransition | reason=$reason, item=${safeMediaItemSummary(mediaItem)}"
            )
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val activePlayer = player
            debugLog(
                LOG_TAG_SERVICE,
                "Service player onPlayWhenReadyChanged | playWhenReady=$playWhenReady, reason=${playWhenReadyChangeReasonToString(reason)}($reason), isPlaying=${activePlayer?.isPlaying}, suppressionReason=${activePlayer?.playbackSuppressionReason?.let(::playbackSuppressionReasonToString)}, playbackState=${activePlayer?.playbackState?.let(::playerStateToString)}"
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                LOG_TAG_SERVICE,
                "Service player onPlayerError | code=${error.errorCodeName}, message=${error.message}",
                error
            )
        }

        override fun onEvents(player: Player, events: Player.Events) {
            debugLog(
                LOG_TAG_SERVICE,
                "Service player onEvents | events=$events, playbackState=${playerStateToString(player.playbackState)}, isPlaying=${player.isPlaying}, playWhenReady=${player.playWhenReady}, suppressionReason=${playbackSuppressionReasonToString(player.playbackSuppressionReason)}, currentPosition=${player.currentPosition}, bufferedPosition=${player.bufferedPosition}, current=${safeMediaItemSummary(player.currentMediaItem)}, ${bundleSummary(player.currentMediaItem?.mediaMetadata?.extras)}"
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        debugLog(LOG_TAG_SERVICE, "onCreate")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .also {
                it.addListener(playerListener)
                debugLog(LOG_TAG_SERVICE, "ExoPlayer initialized")
            }

        mediaSession = MediaSession.Builder(this, requireNotNull(player))
            .build()
            .also {
                debugLog(LOG_TAG_SERVICE, "MediaSession initialized")
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        debugLog(
            LOG_TAG_SERVICE,
            "onGetSession | packageName=${controllerInfo.packageName}, controllerVersion=${controllerInfo.controllerVersion}"
        )
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activePlayer = player ?: return
        debugLog(
            LOG_TAG_SERVICE,
            "onTaskRemoved | isPlaying=${activePlayer.isPlaying}, playWhenReady=${activePlayer.playWhenReady}, suppressionReason=${playbackSuppressionReasonToString(activePlayer.playbackSuppressionReason)}, playbackState=${playerStateToString(activePlayer.playbackState)}"
        )
        if (!activePlayer.isPlaying) {
            debugLog(
                LOG_TAG_SERVICE,
                "FREEPOD_RESET_SOURCE service.onTaskRemoved.stopSelf | current=${activePlayer.currentPosition}, playWhenReady=${activePlayer.playWhenReady}, playbackState=${playerStateToString(activePlayer.playbackState)}"
            )
            stopSelf()
        }
    }

    override fun onDestroy() {
        val activePlayer = player
        if (activePlayer != null) {
            debugLog(
                LOG_TAG_SERVICE,
                "FREEPOD_PAUSE_SOURCE service.onDestroy.releasePlayer | current=${activePlayer.currentPosition}, isPlaying=${activePlayer.isPlaying}, playWhenReady=${activePlayer.playWhenReady}, suppressionReason=${playbackSuppressionReasonToString(activePlayer.playbackSuppressionReason)}, playbackState=${playerStateToString(activePlayer.playbackState)}"
            )
        } else {
            debugLog(LOG_TAG_SERVICE, "onDestroy | player already null")
        }
        mediaSession?.run {
            player.removeListener(playerListener)
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}

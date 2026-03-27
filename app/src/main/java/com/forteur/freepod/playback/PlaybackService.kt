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
import com.forteur.freepod.util.playerStateToString
import com.forteur.freepod.util.safeMediaItemSummary

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(
                LOG_TAG_SERVICE,
                "Service player onPlaybackStateChanged | state=${playerStateToString(playbackState)}($playbackState), current=${safeMediaItemSummary(player?.currentMediaItem)}"
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(
                LOG_TAG_SERVICE,
                "Service player onIsPlayingChanged | isPlaying=$isPlaying, playbackState=${player?.playbackState?.let(::playerStateToString)}"
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(
                LOG_TAG_SERVICE,
                "Service player onMediaItemTransition | reason=$reason, item=${safeMediaItemSummary(mediaItem)}"
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
            Log.d(
                LOG_TAG_SERVICE,
                "Service player onEvents | events=$events, playbackState=${playerStateToString(player.playbackState)}, isPlaying=${player.isPlaying}, current=${safeMediaItemSummary(player.currentMediaItem)}, ${bundleSummary(player.currentMediaItem?.mediaMetadata?.extras)}"
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG_SERVICE, "onCreate")

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
                Log.d(LOG_TAG_SERVICE, "ExoPlayer initialized")
            }

        mediaSession = MediaSession.Builder(this, requireNotNull(player))
            .build()
            .also {
                Log.d(LOG_TAG_SERVICE, "MediaSession initialized")
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(
            LOG_TAG_SERVICE,
            "onGetSession | packageName=${controllerInfo.packageName}, controllerVersion=${controllerInfo.controllerVersion}"
        )
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activePlayer = player ?: return
        if (!activePlayer.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(LOG_TAG_SERVICE, "onDestroy")
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

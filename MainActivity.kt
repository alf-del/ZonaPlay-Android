package com.zonaplay.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var webView: WebView
    private lateinit var playerView: PlayerView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.nativePlayerView)
        webView = findViewById(R.id.zonaWebView)

        trackSelector = DefaultTrackSelector(this)

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()

        playerView.player = player

        /*
         * Native Media3 remains the media engine.
         * The WebView remains the Zona▶Play HTML interface.
         *
         * The WebView is intentionally transparent so the
         * existing HTML controls can sit over the native video.
         */
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(Bridge(), "ZonaPlayNativePlayer")
        webView.loadUrl("file:///android_asset/zona-play.html")

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                callJs("window.dispatchEvent(new CustomEvent('zonaNativePlaying',{detail:{playing:$isPlaying}}));")
            }

            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_BUFFERING -> "buffering"
                    Player.STATE_READY -> "ready"
                    Player.STATE_ENDED -> "ended"
                    else -> "idle"
                }
                callJs("window.dispatchEvent(new CustomEvent('zonaNativeState',{detail:{state:'$name'}}));")
                if (state == Player.STATE_READY) {
                    callJs("window.dispatchEvent(new CustomEvent('zonaNativeReady')); window.dispatchEvent(new Event('playing'));")
                }
            }
        })

        val initialUrl = intent?.getStringExtra("url")
        if (!initialUrl.isNullOrBlank()) {
            loadNative(initialUrl, "{}")
        }
    }

    private fun loadNative(url: String, json: String) {
        val lower = url.lowercase()
        val builder = MediaItem.Builder().setUri(Uri.parse(url))

        if (lower.contains(".m3u8")) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        } else if (lower.contains(".ts")) {
            builder.setMimeType(MimeTypes.VIDEO_MP2T)
        } else if (lower.contains(".mp4")) {
            builder.setMimeType(MimeTypes.VIDEO_MP4)
        }

        player.setMediaItem(builder.build())

        val params = trackSelector.parameters.buildUpon()
            .setPreferredAudioLanguage("es")
            .build()
        trackSelector.setParameters(params)

        player.prepare()
        player.playWhenReady = true

        callJs(
            "window.dispatchEvent(new CustomEvent('zonaNativeLoaded',{detail:{url:${JSONObject.quote(url)}}}));"
        )
    }

    private fun callJs(script: String) {
        runOnUiThread {
            if (::webView.isInitialized) {
                webView.evaluateJavascript(script, null)
            }
        }
    }

    private inner class Bridge {

        @JavascriptInterface
        fun load(url: String, data: String?) {
            if (url.isNotBlank()) loadNative(url, data ?: "{}")
        }

        @JavascriptInterface
        fun play() {
            player.play()
        }

        @JavascriptInterface
        fun pause() {
            player.pause()
        }

        @JavascriptInterface
        fun seek(seconds: Double) {
            player.seekTo((seconds * 1000.0).toLong())
        }

        @JavascriptInterface
        fun audio() {
            callJs("""
                window.dispatchEvent(new CustomEvent('zonaNativeAudioTracks',{
                    detail:{tracks:${audioTracksJson()}}
                }));
            """.trimIndent())
        }

        @JavascriptInterface
        fun subtitles() {
            callJs("""
                window.dispatchEvent(new CustomEvent('zonaNativeTextTracks',{
                    detail:{tracks:${textTracksJson()}}
                }));
            """.trimIndent())
        }

        @JavascriptInterface
        fun quality() {
            callJs("""
                window.dispatchEvent(new CustomEvent('zonaNativeVideoTracks',{
                    detail:{tracks:${videoTracksJson()}}
                }));
            """.trimIndent())
        }

        @JavascriptInterface
        fun selectAudio(groupIndex: Int, trackIndex: Int) {
            selectTrack(C.TRACK_TYPE_AUDIO, groupIndex, trackIndex)
        }

        @JavascriptInterface
        fun selectText(groupIndex: Int, trackIndex: Int) {
            selectTrack(C.TRACK_TYPE_TEXT, groupIndex, trackIndex)
        }

        @JavascriptInterface
        fun selectVideo(groupIndex: Int, trackIndex: Int) {
            selectTrack(C.TRACK_TYPE_VIDEO, groupIndex, trackIndex)
        }

        private fun selectTrack(type: Int, groupIndex: Int, trackIndex: Int) {
            val groups = player.currentTracks.groups.filter { it.type == type }
            if (groupIndex !in groups.indices) return

            val group = groups[groupIndex]
            if (trackIndex !in 0 until group.length) return

            val override = androidx.media3.common.TrackSelectionOverride(
                group.mediaTrackGroup,
                listOf(trackIndex)
            )

            val builder = trackSelector.parameters.buildUpon()
                .setOverrideForType(override)

            if (type == C.TRACK_TYPE_TEXT) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            }

            trackSelector.setParameters(builder.build())
        }

        private fun audioTracksJson(): String {
            return trackGroupsJson(C.TRACK_TYPE_AUDIO)
        }

        private fun textTracksJson(): String {
            return trackGroupsJson(C.TRACK_TYPE_TEXT)
        }

        private fun videoTracksJson(): String {
            return trackGroupsJson(C.TRACK_TYPE_VIDEO)
        }

        private fun trackGroupsJson(type: Int): String {
            val groups = player.currentTracks.groups.filter { it.type == type }
            return groups.mapIndexed { gi, group ->
                val tracks = (0 until group.length).map { ti ->
                    val f = group.getTrackFormat(ti)
                    val lang = f.language ?: ""
                    val label = f.label ?: ""
                    val role = f.roleFlags
                    """{"group":$gi,"track":$ti,"language":${JSONObject.quote(lang)},"label":${JSONObject.quote(label)},"width":${f.width},"height":${f.height},"bitrate":${f.bitrate},"roleFlags":$role}"""
                }
                """{"group":$gi,"tracks":[${tracks.joinToString(",")}]}"""
            }.let { "[${it.joinToString(",")}]" }
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

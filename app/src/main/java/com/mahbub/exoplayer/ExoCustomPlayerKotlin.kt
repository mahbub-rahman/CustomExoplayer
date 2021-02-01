package com.mahbub.exoplayer

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory

class ExoCustomPlayerKotlin(private val context: Activity, private val playerView: PlayerView) {
    private var player: SimpleExoPlayer? = null
    private var fullScreenBtn: ImageView? = null
    private var audioIcon: ImageView? = null
    private var playerProgressBar: ProgressBar? = null
    private var defaultVideoViewParams: ViewGroup.LayoutParams? = null
    private var defaultHttpDataSourceFactory: DefaultHttpDataSourceFactory? = null
    private var dataSourceFactory: DataSource.Factory? = null
    var isFullscreenView = false
        private set
    private val playWhenReady = false
    private val currentPos = 0
    private val speed = listOf(1f, 1.5f, 2f)
    private var isMp3 = false
    private var currentSpeed = 0


    companion object {
        const val TAG = "tag"
    }

    init {
        initializePlayer()
    }

    val currentDuration: Long
        get() = if (player != null && player!!.duration > 0) {
            player!!.currentPosition
        } else {
            0
        }

    val totalDuration: Long
        get() {
            return if (player != null && player!!.duration > 0) {
                player!!.duration
            } else {
                0
            }
        }

    private fun initializePlayer() {
        defaultVideoViewParams = playerView.layoutParams
        dataSourceFactory = DefaultDataSourceFactory(context, BuildConfig.APPLICATION_ID)
        fullScreenBtn = playerView.findViewById(R.id.bt_fullscreen)
        audioIcon = playerView.findViewById(R.id.audioIcon)
        playerProgressBar = playerView.findViewById(R.id.progressBar)
        defaultHttpDataSourceFactory = DefaultHttpDataSourceFactory(
            BuildConfig.APPLICATION_ID,
            null,
            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
            true
        )
        player = SimpleExoPlayer.Builder(context).build()
        playerView.player = player
        setPlayerSpeed(0)
        initListener()
    }

    private fun initListener() {
        playerView.findViewById<View>(R.id.exo_speed)
            .setOnClickListener {
                currentSpeed++
                setPlayerSpeed(currentSpeed)
            }
        playerView.findViewById<View>(R.id.bt_fullscreen)
            .setOnClickListener(object : View.OnClickListener {
                override fun onClick(view: View) {
                    Log.d(TAG, "onClick: " + isFullscreenView)
                    setOrientation()
                }
            })
        player!!.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                playerView.keepScreenOn = !(state == Player.STATE_IDLE || state == Player.STATE_ENDED ||
                        !playWhenReady)
                if (state == Player.STATE_BUFFERING) {
                    playerProgressBar!!.visibility = View.VISIBLE
                } else {
                    playerProgressBar!!.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                Log.d(TAG, "onPlayerError: " + error.message)
                pausePlayer()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerProgressBar!!.visibility = View.GONE
                }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                val manifest = player!!.currentManifest
                if (manifest != null) {
                }
            }
        })
    }

    fun setOrientation() {
        if (isFullscreenView) {
            fullScreenBtn!!.setImageDrawable(
                context.resources.getDrawable(R.drawable.ic_fullsecreen)
            )
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isFullscreenView = false
        } else {
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            fullScreenBtn!!.setImageDrawable(
                context.resources.getDrawable(R.drawable.ic_fullscreen_exit)
            )
            isFullscreenView = true
        }
    }

    private fun setPlayerSpeed(currentSpeed: Int) {
        val speedPos = speed[currentSpeed % speed.size]
        val param = PlaybackParameters(speedPos)
        player!!.setPlaybackParameters(param)
        val view = playerView.findViewById<View>(R.id.exo_speed) as TextView
        Log.d(TAG, "onClick:Speed $speedPos")
        view.text = String.format("%.1f", speedPos) + "x"
    }

    fun play(
        url: String,
        cloudFrontPolicy: String?=null, cloudFrontSignature: String?=null, cloudFrontKeyPairId: String?=null,
        lastDuration: Long=0
    ) {
        Log.d(TAG, "playVideoInExoPlayer: $url")
        if (player != null && player!!.isPlaying) {
            player!!.stop()
        }
        if (!TextUtils.isEmpty(url) && url.contains(".mp3")) {
            isMp3 = true
            playerView.clearDisappearingChildren()
            playerView.controllerShowTimeoutMs = 0
            playerView.controllerHideOnTouch = false
            audioIcon!!.visibility = View.VISIBLE
        } else {
            playerView.clearDisappearingChildren()
            playerView.controllerShowTimeoutMs = 5000
            playerView.controllerHideOnTouch = true
            isMp3 = false
            audioIcon!!.visibility = View.GONE
        }
        context.runOnUiThread {


            // Uri uri = Uri.parse(url);
            val mediaSource: MediaSource =
                buildMediaSource(url, cloudFrontPolicy, cloudFrontSignature, cloudFrontKeyPairId)

            player!!.setMediaSource(mediaSource)
            player!!.prepare()
            player!!.playWhenReady = true
            player!!.seekTo(currentPos, lastDuration)
        }
    }

    fun pausePlayer() {
        if (player == null) return
        player!!.playWhenReady = false
        player!!.playbackState
    }

    fun releasePlayer() {
        pausePlayer()
        player!!.release()
    }

    fun makeVideoFullScreen() {
        Log.d(TAG, "makeVideoFullScreen: called")
        if (playerView.visibility == View.GONE) return
        isFullscreenView = true
        fullScreenBtn!!.setImageDrawable(
            context.resources.getDrawable(R.drawable.ic_fullscreen_exit)
        )
        playerView.postDelayed({
            playerView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            playerView.layout(10, 10, 10, 10)
        }, 500)
    }

    // close fullscreen mode
    fun exitVideoFullScreen() {
        Log.d(TAG, "exitVideoFullScreen: called: ")
        if (playerView.visibility == View.GONE) return
        // binding.bottomNav.setVisibility(View.VISIBLE);
        isFullscreenView = false
        fullScreenBtn!!.setImageDrawable(
            context.resources.getDrawable(R.drawable.ic_fullsecreen)
        )
        playerView.postDelayed({
            playerView.layoutParams = defaultVideoViewParams
            playerView.layout(10, 10, 10, 10)
        }, 500)
    }

    private fun buildDefaultMediaSource(uri: Uri): MediaSource {
        return ProgressiveMediaSource.Factory(dataSourceFactory!!)
            .createMediaSource(MediaItem.fromUri(uri))
    }

    private fun buildMediaSource(url: String, CloudFrontPolicy: String?=null, CloudFrontSignature: String?=null, CloudFrontKeyPairId: String?=null): MediaSource {
        var url = url



        if (!TextUtils.isEmpty(CloudFrontPolicy) &&
            !TextUtils.isEmpty(CloudFrontSignature)
            && !TextUtils.isEmpty(CloudFrontKeyPairId)) {

            var cookieValue: String = ""
            cookieValue += "CloudFront-Policy=$CloudFrontPolicy;"
            cookieValue += "CloudFront-Signature=$CloudFrontSignature;"
            cookieValue += "CloudFront-Key-Pair-Id=$CloudFrontKeyPairId;"
            defaultHttpDataSourceFactory!!.defaultRequestProperties["Cookie"] = (cookieValue)
        }
        val dataSourceFactory =
            DefaultDataSourceFactory(context, null, defaultHttpDataSourceFactory!!)
        defaultHttpDataSourceFactory!!.createDataSource()

//         FOR HLS
        return if (url.endsWith(".m3u8")) {
            Log.d(TAG, "buildMediaSource:HLS $url")
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(url))
        } else if (url.endsWith(".mpd")) {
            //        FOR DASH LINK
            Log.d(TAG, "buildMediaSource:DASH $url")
            DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        } else {
            //        FOR AUDIO and VIDEO .mp4, mp3 etc.

            Log.d(TAG, "buildMediaSource:MP4 $url")
            buildDefaultMediaSource(Uri.parse(url))
        }
    }


}

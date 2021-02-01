package com.mahbub.exoplayer

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {

    lateinit var playerView: PlayerView
    lateinit var simplePlayer: SimpleExoPlayer
    lateinit var progressBar: ProgressBar
    lateinit var btFulScreen: ImageView
    var flag: Boolean = false
    private lateinit var player: ExoCustomPlayerKotlin
    private lateinit var exoPlayerView: PlayerView

    private val url = ""; //URL HERE

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.exoPlayer)
        player = ExoCustomPlayerKotlin(this, playerView)

        player.play(url =url)


    }

    override fun onPause() {
        super.onPause()
        player.pausePlayer()

    }

    override fun onStart() {
        super.onStart()


    }
}
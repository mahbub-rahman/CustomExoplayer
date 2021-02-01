package com.mahbub.exoplayer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;

import java.util.Arrays;
import java.util.List;

public class ExoCustomPlayer {

    public static final String TAG = "tag";

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private ImageView fullScreenBtn;
    private ImageView audioIcon;
    private ProgressBar playerProgressBar;

    private ViewGroup.LayoutParams defaultVideoViewParams;
    private DefaultHttpDataSourceFactory defaultHttpDataSourceFactory;
    private DataSource.Factory dataSourceFactory;

    private boolean isFullscreen;
    private Activity context;
    private boolean playWhenReady;
    private int currentPos = 0;
    private List<Float> speed = Arrays.asList(1f,1.5f, 2f);
    private boolean isMp3;
    private int currentSpeed = 0;


    public ExoCustomPlayer(Activity activity, PlayerView playerView) {
        this.playerView = playerView;
        this.context = activity;
        initializePlayer();

    }

    public long getCurrentDuration() {
        if (player != null && player.getDuration() > 0) {
           return player.getCurrentPosition();
        }else {
            return 0;
        }
    }
    public long getTotalDuration() {
        if (player != null && player.getDuration() > 0) {
            return player.getDuration();
        }else {
            return 0;
        }
    }


    public void initializePlayer() {

        defaultVideoViewParams = playerView.getLayoutParams();
        dataSourceFactory = new DefaultDataSourceFactory(context, BuildConfig.APPLICATION_ID);


        fullScreenBtn = playerView.findViewById(R.id.bt_fullscreen);
        audioIcon = playerView.findViewById(R.id.audioIcon);
        playerProgressBar = playerView.findViewById(R.id.progressBar);

        defaultHttpDataSourceFactory =
                new DefaultHttpDataSourceFactory(
                        BuildConfig.APPLICATION_ID,
                        null,
                        DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                        DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                        true);


        player = new SimpleExoPlayer.Builder(context).build();
        playerView.setPlayer(player);


        setPlayerSpeed(0);
        initListener();


    }

    private void initListener() {

        playerView.findViewById(R.id.exo_speed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSpeed++;
                setPlayerSpeed(currentSpeed);
            }
        });
        playerView.findViewById(R.id.bt_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: " + isFullscreen);
                setOrientation();
            }
        });
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_IDLE || state == Player.STATE_ENDED ||
                        !playWhenReady) {
                    playerView.setKeepScreenOn(false);
                } else { // STATE_IDLE, STATE_ENDED
                    // This prevents the screen from getting dim/lock
                    playerView.setKeepScreenOn(true);
                }

                if(state == Player.STATE_BUFFERING) {
                    playerProgressBar.setVisibility(View.VISIBLE);
                }else {
                    playerProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.d(TAG, "onPlayerError: " + error.getMessage());
                pausePlayer();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    playerProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {
                Object manifest = player.getCurrentManifest();
                if(manifest != null) {

                }
            }
        });

    }

    public boolean isFullscreenView() {
        return this.isFullscreen;
    }
    public void setOrientation() {
        if (isFullscreen) {
            fullScreenBtn.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_fullsecreen));
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFullscreen = false;
        }else {
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            fullScreenBtn.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_fullscreen_exit));
            isFullscreen   = true;
        }
    }

    private void setPlayerSpeed(int currentSpeed) {
        float speedPos = speed.get(currentSpeed % speed.size());
        PlaybackParameters param = new PlaybackParameters(speedPos);
        player.setPlaybackParameters(param);
        TextView view = (TextView) playerView.findViewById(R.id.exo_speed);
        Log.d(TAG, "onClick:Speed "+speedPos);
        view.setText(String.format("%.1f", speedPos)+"x");
    }

    public void play(final String url, String cookiePolicy, String cookieSignature, String cookiePairId, long lastDuration) {

        Log.d(TAG, "playVideoInExoPlayer: " + url);
        if (player != null && player.isPlaying()) {
            player.stop();
        }

        if (!TextUtils.isEmpty(url) && url.contains(".mp3")) {

            isMp3 = true;
            playerView.clearDisappearingChildren();
            playerView.setControllerShowTimeoutMs(0);
            playerView.setControllerHideOnTouch(false);
            audioIcon.setVisibility(View.VISIBLE);

        } else {
            playerView.clearDisappearingChildren();
            playerView.setControllerShowTimeoutMs(5000);
            playerView.setControllerHideOnTouch(true);
            isMp3 = false;
            audioIcon.setVisibility(View.GONE);
        }



        ((Activity) context).runOnUiThread(() -> {

            // Uri uri = Uri.parse(url);
            MediaSource mediaSource = buildMediaSource(url, cookiePolicy, cookieSignature, cookiePairId);

            player.setMediaSource(mediaSource);
            player.prepare();
            player.setPlayWhenReady(true);
            player.seekTo(currentPos, lastDuration);

        });

    }
    public void pausePlayer() {
        if (player == null) return;
        player.setPlayWhenReady(false);
        player.getPlaybackState();
    }

    public void releasePlayer() {
        pausePlayer();
        player.release();
    }


    public void makeVideoFullScreen() {
        Log.d(TAG, "makeVideoFullScreen: called");
        if (playerView.getVisibility() == View.GONE) return;
        isFullscreen = true;
        fullScreenBtn.setImageDrawable(context.getResources().getDrawable( R.drawable.ic_fullscreen_exit));
        playerView.postDelayed(new Runnable() {

            @Override
            public void run() {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                playerView.setLayoutParams(params);
                playerView.layout(10, 10, 10, 10);
            }
        }, 500);
    }
    // close fullscreen mode
    public void exitVideoFullScreen() {
        Log.d(TAG, "exitVideoFullScreen: called: ");
        if (playerView.getVisibility() == View.GONE) return;
        isFullscreen = false;
        fullScreenBtn.setImageDrawable(context.getResources().getDrawable( R.drawable.ic_fullsecreen));
        playerView.postDelayed(new Runnable() {

            @Override
            public void run() {
                playerView.setLayoutParams(defaultVideoViewParams);
                playerView.layout(10, 10, 10, 10);
            }
        }, 500);
    }

    private MediaSource buildMediaSource(Uri uri) {
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
    }

    private MediaSource buildMediaSource(String url, String CloudFrontPolicy,
                                         String CloudFrontSignature, String CloudFrontKeyPairId) {

        if (!TextUtils.isEmpty(CloudFrontPolicy) && !TextUtils.isEmpty(CloudFrontSignature) && !TextUtils.isEmpty(CloudFrontKeyPairId)) {
            String cookieValue = "";

            cookieValue += "CloudFront-Policy=" + CloudFrontPolicy + ";";
            cookieValue += "CloudFront-Signature=" + CloudFrontSignature + ";";
            cookieValue += "CloudFront-Key-Pair-Id=" + CloudFrontKeyPairId + ";";
            defaultHttpDataSourceFactory.getDefaultRequestProperties().set("Cookie", cookieValue);

        }
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, null, defaultHttpDataSourceFactory);
        defaultHttpDataSourceFactory.createDataSource();

//         FOR HLS
        if (url.endsWith(".m3u8")) {
            Log.d(TAG, "buildMediaSource:HLS " + url);
            return
                    new HlsMediaSource.Factory(dataSourceFactory)
                            .setAllowChunklessPreparation(true)
                            .createMediaSource(MediaItem.fromUri(url));

        } else if (url.endsWith(".mpd")) {
            //        FOR DASH LINK
            Log.d(TAG, "buildMediaSource:DASH " + url);
            return
                    new DashMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(url));
        } else {
            Log.d(TAG, "buildMediaSource:MP4 " + url);

            return buildMediaSource(Uri.parse(url));
        }

    }


}

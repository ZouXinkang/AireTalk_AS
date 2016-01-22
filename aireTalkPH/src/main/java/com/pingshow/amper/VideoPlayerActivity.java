package com.pingshow.amper;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.pingshow.amper.view.ProgressView;

public class VideoPlayerActivity extends Activity implements
		OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
		OnVideoSizeChangedListener, SurfaceHolder.Callback {

	private static final String TAG = "MediaPlayerDemo";
	private int mVideoWidth;
	private int mVideoHeight;
	private MediaPlayer mMediaPlayer;
	private SurfaceView mPreview;
	private ProgressView mProgress;
	private SurfaceHolder holder;
	private Handler mHandler=new Handler();
	private String path;
	private Bundle extras;
	private boolean mIsVideoSizeKnown = false;
	private boolean mIsVideoReadyToBePlayed = false;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		setContentView(R.layout.video_player);
		mPreview = (SurfaceView) findViewById(R.id.video);
		mProgress = (ProgressView) findViewById(R.id.progress);
		holder = mPreview.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		extras = getIntent().getExtras();
	}

	private void playVideo() {
		doCleanUp();
		try {
			path=extras.getString("URL");

			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.setDisplay(holder);
			mMediaPlayer.prepare();
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnVideoSizeChangedListener(this);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		} catch (Exception e) {
		}
	}
	
	Runnable playThread=new Runnable()
	{
		public void run()
		{
			playVideo();
		}
	};

	public void onBufferingUpdate(MediaPlayer arg0, int percent) {
		Log.d("onBufferingUpdate percent:" + percent);
	}

	public void onCompletion(MediaPlayer arg0) {
		Log.d("onCompletion called");
	}

	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		if (width == 0 || height == 0) {
			Log.e("invalid video width(" + width + ") or height(" + height + ")");
			return;
		}
		mIsVideoSizeKnown = true;
		mVideoWidth = width;
		mVideoHeight = height;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	public void onPrepared(MediaPlayer mediaplayer) {
		Log.d("onPrepared called");
		mIsVideoReadyToBePlayed = true;
		if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown) {
			startVideoPlayback();
		}
	}

	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
		Log.d("surfaceChanged called");
	}

	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
		Log.d("surfaceDestroyed called");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("surfaceCreated called");
		new Thread(playThread).start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaPlayer();
		doCleanUp();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMediaPlayer();
		doCleanUp();
	}

	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
	}

	private void doCleanUp() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsVideoReadyToBePlayed = false;
		mIsVideoSizeKnown = false;
	}

	private void startVideoPlayback() {
		holder.setFixedSize(mVideoWidth, mVideoHeight);
		setVideoSize();
		mMediaPlayer.start();
		
		mHandler.post(new Runnable(){
			public void run()
			{
				mProgress.setVisibility(View.INVISIBLE);
				mProgress.setImageResource(R.drawable.null_btn);
			}
		});
	}
	
	private void setVideoSize() {
        if (mVideoHeight==0) return;
        float videoProportion = (float) mVideoWidth / (float) mVideoHeight;

        // Get the width of the screen
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        // Get the SurfaceView layout parameters
        RelativeLayout.LayoutParams lp = (LayoutParams) mPreview.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        // Commit the layout parameters
        mPreview.setLayoutParams(lp);
    }
}
package com.pingshow.util;

import android.app.Activity;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.MediaController;
import android.widget.VideoView;

import com.pingshow.amper.Global;
import com.pingshow.amper.R;

/**
 * Created:         Hsia on 16/1/29.
 * Email:           xiaweifeng@pingshow.net
 * Description:     {TODO}(用于播放视频)
 */
public class OpenShareVideo extends Activity{
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        setContentView(R.layout.sharevideo);
//    }



    private VideoView videoView;
    private int play_progress;
    private String video_url;
    private String progress_Title;
    private Dialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sharevideo);

        Intent intent = getIntent();
//        video_url = intent.getStringExtra("video_url");
//        progress_Title = intent.getStringExtra("title");

        // for test
        video_url = Global.SdcardPath_downloads+"哈哈.mp4";
//                Environment.getExternalStorageDirectory().toString()
//                + "/dcim/100MEDIA/VIDEO0002.mp4";

        if (this.progress_Title == null)
            this.progress_Title = "Loading";

        play_progress = intent.getIntExtra("play_progress", 0);
        videoView = (VideoView) findViewById(R.id.videoView);
        progress = ProgressDialog.show(this, "loading", progress_Title);
        progress.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    OpenShareVideo.this.finish();
                }
                return false;
            }
        });

        videoView.setVideoURI(Uri.parse(video_url));
        MediaController controller = new MediaController(this);
        videoView.setMediaController(controller);
        videoView.requestFocus();

        videoView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        videoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (progress != null)
                    progress.dismiss();
            }
        });

        videoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (progress != null)
                    progress.dismiss();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent();
            intent.putExtra("play_progress", videoView.getCurrentPosition());
            setResult(RESULT_OK, intent);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.seekTo(play_progress);
        videoView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoView != null) {
            videoView.pause();
            play_progress = videoView.getCurrentPosition();
        }
        if (progress != null) {
            progress.dismiss();
        }
    }
}

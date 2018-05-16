package com.lichens.licht.videosyncdemo.manage;

import android.content.Context;
import android.util.Log;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYTextureView;

/**
 * Created by licht on 2018/5/15.
 */

public class VideoListenerManage {

    private static final String TAG = "VideoListenerManage";
    private KSYTextureView mVideoView;
    private Context mContext;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoOutputFormat;

    public VideoListenerManage(KSYTextureView videoView , Context context){
        mVideoView = videoView;
        mContext = context;
        //设置屏幕常亮
        videoView.setKeepScreenOn(true);
        //播放器的状态监听
        videoView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        videoView.setOnCompletionListener(mOnCompletionListener);
        videoView.setOnPreparedListener(mOnPreparedListener);
        videoView.setOnInfoListener(mOnInfoListener);
        videoView.setOnVideoSizeChangedListener(mOnVideoSizeChangeListener);
        videoView.setOnErrorListener(mOnErrorListener);
        videoView.setOnSeekCompleteListener(mOnSeekCompletedListener);
        //设置屏幕常亮
        videoView.setScreenOnWhilePlaying(true);
        //缓冲时间
        videoView.setBufferTimeMax(300.0f);
        //超时时间
        videoView.setTimeout(5, 30);
        //翻转角度
        videoView.setRotateDegree(90);
        //循环播放
        videoView.setLooping(true);
        //硬解264&265
        videoView.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_AUTO);
        //
        mVideoOutputFormat = KSYMediaPlayer.SDL_FCC_RV32;// 设置输出的视频原始数据格式为ABGR8888

        videoView.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", mVideoOutputFormat);
    }


    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            mVideoWidth = mVideoView.getVideoWidth();
            mVideoHeight = mVideoView.getVideoHeight();
            // Set Video Scaling Mode
            mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            //start player
            mVideoView.start();
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            long duration = mVideoView.getDuration();
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangeListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (width != mVideoWidth || height != mVideoHeight) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();

                    if (mVideoView != null)
                        mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }
            }
        }
    };

    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompletedListener = new IMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            Log.e(TAG, "onSeekComplete...............");
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            videoPlayEnd();
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            switch (what) {
//                case KSYVideoView.MEDIA_ERROR_UNKNOWN:
//                 Log.e(TAG, "OnErrorListener, Error Unknown:" + what + ",extra:" + extra);
//                  break;
                default:
                    Log.e(TAG, "OnErrorListener, Error:" + what + ",extra:" + extra);
            }
            videoPlayEnd();
            return false;
        }
    };

    public IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            switch (i) {
                case KSYMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    Log.e(TAG, "Buffering Start.");
                    break;
                case KSYMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    Log.e(TAG, "Buffering End.");
                    break;
                case KSYMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
//                    Toast.makeText(mContext, "Audio Rendering Start", Toast.LENGTH_SHORT).show();
                    break;
                case KSYMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
//                    Toast.makeText(mContext, "Video Rendering Start", Toast.LENGTH_SHORT).show();
                    break;
                case KSYMediaPlayer.MEDIA_INFO_SUGGEST_RELOAD:
//                     Player find a new stream(video or audio), and we could reload the video.
                    break;
                case KSYMediaPlayer.MEDIA_INFO_RELOADED:
                    Log.e(TAG, "Succeed to reload video.");
                    return false;
            }
            return false;
        }
    };
    public void videoPlayEnd() {
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
    }
}

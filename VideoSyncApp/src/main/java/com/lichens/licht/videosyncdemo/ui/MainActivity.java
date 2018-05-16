package com.lichens.licht.videosyncdemo.ui;

import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ksyun.media.player.KSYTextureView;
import com.lichens.licht.videosyncdemo.R;
import com.lichens.licht.videosyncdemo.manage.VideoListenerManage;
import com.lichens.licht.videosyncdemo.mqtt.MqttManager;
import com.lichens.licht.videosyncdemo.utils.SharePrefUtil;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;


public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private KSYTextureView mVideoView;
    private ImageView mSignalState;
    private Button mPublishMessage;
    private TextView mVideoInfo;
    private TextView mOffsetInfo;
    private VideoListenerManage mVideoListenerManage;
    private EditText mClientID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加载视图
        setContentView(R.layout.activity_main);

        //获取view的实例
        mVideoView = (KSYTextureView) findViewById(R.id.activity_ksy_player);
        mPublishMessage = (Button) findViewById(R.id.publish_message);
        mSignalState = (ImageView) findViewById(R.id.signal_state);
        mVideoInfo = (TextView) findViewById(R.id.info);
        mOffsetInfo = (TextView) findViewById(R.id.offset);
        mClientID = (EditText) findViewById(R.id.clientID);

        //视频texture的管理
        mVideoListenerManage = new VideoListenerManage(mVideoView, this);
        //设置屏幕常亮
        mVideoView.setKeepScreenOn(true);
        //多媒体音量控制
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        try {
            //获取播放路径
            String url = "file://sdcard/Download/12.mp4";
            mVideoView.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //准备播放
        mVideoView.prepareAsync();

        //按钮监听
        mPublishMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String numble = mClientID.getText().toString().trim();

                if (!TextUtils.isEmpty(numble))
                    SharePrefUtil.putString("CLIENTID", "client_" + numble);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MqttManager.getInstance().publish("test", 2, "0".getBytes());
                    }
                }).start();
            }
        });
    }


    /**
     * 订阅接收到的消息
     * 处理server端的命令
     */
    @Subscribe
    public void onEvent(final MqttMessage message) {
        Log.e(TAG, message.toString());
        Log.e(TAG, "-------------" + mVideoView.getDuration());
        Log.e(TAG, "-------------" + mVideoView.getCurrentPosition());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoInfo.setText("当前播放时间" + mVideoView.getCurrentPosition());

                long playTime = Integer.parseInt(message.toString()) % mVideoView.getDuration();

                Log.e(TAG, "playTime: " + playTime);
                Log.e(TAG, "offset: " + Math.abs(playTime - mVideoView.getCurrentPosition()));

                mOffsetInfo.setText("offset : " + (playTime - mVideoView.getCurrentPosition() - 100));

                if (Math.abs(playTime - mVideoView.getCurrentPosition() - 100) > 200)
                    //设定播放时间
                    mVideoView.seekTo(playTime);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放掉视频
        mVideoListenerManage.videoPlayEnd();
    }

    @Override
    public void mqttConnectSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSignalState.setBackgroundResource(R.mipmap.signal_good);
            }
        });

    }

    @Override
    public void mqttConnectError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSignalState.setBackgroundResource(R.mipmap.signal_bad);
            }
        });
    }
}

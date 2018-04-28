package com.lichens.licht.filedownload;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final String videoUrl = "http://192.168.3.93:8080/DataServer/15.mp4";
    private TextView tv_progress;//进度显示
    private ProgressBar progressBar;//进度条
    private Button downLoad;//下载按钮
    private Button pause;//暂停按钮
    private String path;//下载路径
    private int max;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_progress = (TextView) findViewById(R.id.tv_Progress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        downLoad = (Button) findViewById(R.id.downLoad);
        pause = (Button) findViewById(R.id.pause);

        checkPermission();

        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/local";
        final DownloadUtil downloadUtil = new DownloadUtil(4, path, "drum.mp4", videoUrl, this);
        downloadUtil.setOnDownloadListener(new DownloadUtil.OnDownloadListener() {
            @Override
            public void downloadStart(int fileSize) {
                Log.i("TAG---fileSize", fileSize + "");
                max = fileSize;//文件总长度
                progressBar.setMax(fileSize);
            }
            @Override
            public void downloadProgress(int downloadedSize) {
                Log.i("TAG---downloadedSize", downloadedSize + "");
                progressBar.setProgress(downloadedSize);
                tv_progress.setText((int) downloadedSize * 100 / max + "%");
            }
            @Override
            public void downloadEnd() {
                Log.i("TAG---end", "End");
            }
        });
        /**
         * 下载的点击事件
         */
        downLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadUtil.start();
            }
        });
        /**
         * 暂停的点击事件
         */
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadUtil.pause();
            }
        });
    }

    private void checkPermission() {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 00);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "checkPermission: 已经授权！");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e(TAG, "授权结果 : : 已经授权！-- " + requestCode);
    }
}
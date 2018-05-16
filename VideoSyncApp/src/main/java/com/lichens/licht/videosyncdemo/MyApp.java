package com.lichens.licht.videosyncdemo;

import android.app.Application;

import com.lichens.licht.videosyncdemo.utils.SharePrefUtil;

/**
 * Created by licht on 2018/5/16.
 */

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharePrefUtil.init(this);
    }
}

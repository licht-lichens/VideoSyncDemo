package com.lichens.licht.filedownload;

import android.app.Application;

import com.example.greendaodemo.database.DaoMaster;
import com.example.greendaodemo.database.DaoSession;
import com.example.greendaodemo.database.UserDao;

/**
 * Created by licht on 2018/4/25.
 */

public class App extends Application {
    public static UserDao userDao;
    @Override
    public void onCreate() {
        super.onCreate();
        DaoMaster.DevOpenHelper devOpenHelper = new DaoMaster.DevOpenHelper(this, "licht_db", null);
        DaoMaster daoMaster = new DaoMaster(devOpenHelper.getWritableDb());
        DaoSession daoSession = daoMaster.newSession();
        userDao = daoSession.getUserDao();
    }

}

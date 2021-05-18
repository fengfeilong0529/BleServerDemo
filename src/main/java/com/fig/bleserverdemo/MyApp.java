package com.fig.bleserverdemo;

import android.app.Application;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BleServerManager.getInstance().init(this);
    }
}

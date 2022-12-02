package com.example.serialportlamp;

import android.app.Application;


import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class LampApplication extends Application {
    public static CH34xUARTDriver driver;
    @Override
    public void onCreate() {
        super.onCreate();
//        CrashHandler crashHandler = CrashHandler.getInstance();
//        crashHandler.init();
//        FileUitls.getInstance().setFileTempDir(0);
    }
}

package com.viifo.tgvideocompress;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

public class ApplicationLoader extends Application {
    public static ApplicationLoader applicationLoaderInstance;
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationLoaderInstance = this;
        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {}
        applicationHandler = new Handler(applicationContext.getMainLooper());
    }
}

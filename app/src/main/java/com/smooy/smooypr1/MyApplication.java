package com.smooy.smooypr1;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Log.d(TAG, "MyApplication inicializada correctamente");
    }

    public static Context getContext() {
        Log.d(TAG, "Contexto " + (context == null ? "NO disponible" : "disponible"));
        return context;
    }
}
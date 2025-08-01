package com.example.relwallpaper.ui;

import android.app.Application;
import com.example.relwallpaper.ui.utils.ImageCacheManager;
import com.google.android.material.color.DynamicColors;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        ImageCacheManager.getInstance(this);
        setupGlobalImageLoading();
    }

    private void setupGlobalImageLoading() {
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ImageCacheManager.getInstance(this).clearCache();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ImageCacheManager.getInstance(this).cleanup();
    }
}
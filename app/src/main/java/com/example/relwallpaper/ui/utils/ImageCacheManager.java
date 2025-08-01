package com.example.relwallpaper.ui.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.FutureTarget;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.concurrent.Future;
import java.util.ArrayList;

public class ImageCacheManager {
    private static final String TAG = "ImageCacheManager";
    private static ImageCacheManager instance;
    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final List<Future<?>> preloadTasks;

    private ImageCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.preloadTasks = new ArrayList<>();
    }

    public static synchronized ImageCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCacheManager(context);
        }
        return instance;
    }

    public void preloadImages(List<String> imageUrls, PreloadCallback callback) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onPreloadComplete(0));
            }
            return;
        }

        Future<?> task = executor.submit(() -> {
            int successCount = 0;
            int totalImages = imageUrls.size();

            for (int i = 0; i < totalImages; i++) {
                String imageUrl = imageUrls.get(i);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    try {
                        RequestOptions preloadOptions = new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .override(400, 600)
                                .skipMemoryCache(false);

                        FutureTarget<Bitmap> futureTarget = Glide.with(context)
                                .asBitmap()
                                .load(imageUrl)
                                .apply(preloadOptions)
                                .submit();

                        futureTarget.get();
                        successCount++;


                        final int currentProgress = i + 1;
                        final int currentSuccessCount = successCount;
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onPreloadProgress(currentProgress, totalImages);
                            }
                        });

                    } catch (Exception e) {
                        Log.w(TAG, "Failed to preload image: " + imageUrl, e);
                    }
                }
            }

            final int finalSuccessCount = successCount;
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onPreloadComplete(finalSuccessCount);
                }
            });
        });

        preloadTasks.add(task);
    }

    public void preloadImageWithSize(String imageUrl, int width, int height) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        executor.submit(() -> {
            try {
                RequestOptions options = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(width, height);

                FutureTarget<Bitmap> futureTarget = Glide.with(context)
                        .asBitmap()
                        .load(imageUrl)
                        .apply(options)
                        .submit();

                futureTarget.get();
            } catch (Exception e) {
                Log.w(TAG, "Failed to preload image with custom size: " + imageUrl, e);
            }
        });
    }

    public void clearCache() {
        executor.submit(() -> {
            try {
                Glide.get(context).clearDiskCache();
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear disk cache", e);
            }
        });

        mainHandler.post(() -> {
            try {
                Glide.get(context).clearMemory();
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear memory cache", e);
            }
        });
    }

    public void getCacheSize(CacheSizeCallback callback) {
        executor.submit(() -> {
            try {
                long cacheSize = calculateCacheSize();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onCacheSizeCalculated(cacheSize);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to calculate cache size", e);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onCacheSizeCalculated(0);
                    }
                });
            }
        });
    }

    private long calculateCacheSize() {
        try {
            return context.getCacheDir().length();
        } catch (Exception e) {
            return 0;
        }
    }

    public void cancelPreloadTasks() {
        for (Future<?> task : preloadTasks) {
            if (!task.isDone()) {
                task.cancel(true);
            }
        }
        preloadTasks.clear();
    }

    public void cleanup() {
        cancelPreloadTasks();
        executor.shutdown();
    }

    public interface PreloadCallback {
        void onPreloadProgress(int loaded, int total);
        void onPreloadComplete(int successCount);
    }

    public interface CacheSizeCallback {
        void onCacheSizeCalculated(long sizeInBytes);
    }

    public static String formatCacheSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
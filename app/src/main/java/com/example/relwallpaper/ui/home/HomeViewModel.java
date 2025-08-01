package com.example.relwallpaper.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.relwallpaper.ui.home.model.Wallpaper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";

    private MutableLiveData<List<Wallpaper>> wallpapers = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<List<Wallpaper>> carouselWallpapers = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> hasMoreData = new MutableLiveData<>(true);
    private MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);

    private long lastLoadTime = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000;

    public HomeViewModel() {
        super();
    }

    public LiveData<List<Wallpaper>> getWallpapers() {
        return wallpapers;
    }

    public LiveData<List<Wallpaper>> getCarouselWallpapers() {
        return carouselWallpapers;
    }

    public LiveData<Boolean> getLoadingState() {
        return isLoading;
    }

    public LiveData<Boolean> getHasMoreData() {
        return hasMoreData;
    }

    public LiveData<Integer> getCurrentPage() {
        return currentPage;
    }

    public void setWallpapers(List<Wallpaper> newWallpapers) {
        List<Wallpaper> currentList = wallpapers.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        currentList.clear();
        currentList.addAll(newWallpapers);
        wallpapers.setValue(currentList);
        lastLoadTime = System.currentTimeMillis();

        updateCarousel(newWallpapers);

    }

    public void addWallpapers(List<Wallpaper> newWallpapers) {
        List<Wallpaper> currentList = wallpapers.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }

        currentList.addAll(newWallpapers);
        wallpapers.setValue(currentList);

    }

    private void updateCarousel(List<Wallpaper> wallpaperList) {
        List<Wallpaper> carousel = new ArrayList<>();
        int count = Math.min(5, wallpaperList.size());
        for (int i = 0; i < count; i++) {
            carousel.add(wallpaperList.get(i));
        }
        carouselWallpapers.setValue(carousel);
    }

    public void setLoadingState(boolean loading) {
        isLoading.setValue(loading);
    }

    public void setHasMoreData(boolean hasMore) {
        hasMoreData.setValue(hasMore);
    }

    public void setCurrentPage(int page) {
        currentPage.setValue(page);
    }

    public boolean needsRefresh() {
        List<Wallpaper> currentWallpapers = wallpapers.getValue();
        boolean isEmpty = currentWallpapers == null || currentWallpapers.isEmpty();
        boolean isStale = (System.currentTimeMillis() - lastLoadTime) > CACHE_DURATION;

        return isEmpty || isStale;
    }

    public int getWallpaperCount() {
        List<Wallpaper> currentWallpapers = wallpapers.getValue();
        return currentWallpapers != null ? currentWallpapers.size() : 0;
    }

    public void clearData() {
        wallpapers.setValue(new ArrayList<>());
        carouselWallpapers.setValue(new ArrayList<>());
        currentPage.setValue(0);
        hasMoreData.setValue(true);
        lastLoadTime = 0;

    }

    public void forceRefresh() {
        lastLoadTime = 0;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
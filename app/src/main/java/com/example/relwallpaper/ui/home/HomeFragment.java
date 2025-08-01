package com.example.relwallpaper.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.databinding.FragmentHomeBinding;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.example.relwallpaper.ui.home.adapter.CarouselAdapter;
import com.example.relwallpaper.ui.home.adapter.WallpaperAdapter;
import com.example.relwallpaper.ui.home.model.Wallpaper;
import com.example.relwallpaper.ui.pages.WallpaperDetailActivity;
import com.example.relwallpaper.ui.utils.ImageCacheManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class HomeFragment extends Fragment implements WallpaperAdapter.OnWallpaperClickListener, CarouselAdapter.OnCarouselItemClickListener {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;

    private static final int PAGE_SIZE = 20;
    private static final int PRELOAD_THRESHOLD = 5;

    private FragmentHomeBinding binding;
    private WallpaperAdapter adapter;
    private CarouselAdapter carouselAdapter;
    private List<Wallpaper> wallpapers;
    private List<Wallpaper> carouselWallpapers;
    private OkHttpClient httpClient;
    private String accessToken;

    private RecyclerView recyclerView;
    private RecyclerView carouselRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout initialLoadingLayout;

    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 0;
    private int totalLoadedItems = 0;

    private boolean isDataLoaded = false;
    private boolean isViewCreated = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentHomeBinding.inflate(inflater, container, false);

            accessToken = requireActivity().getSharedPreferences("auth", requireContext().MODE_PRIVATE)
                    .getString("supabase_access_token", null);

            if (accessToken == null) {
                startActivity(new Intent(getContext(), LoginActivity.class));
                requireActivity().finish();
                return binding.getRoot();
            }

            initializeComponents();
            setupRecyclerViews();
        }

        if (wallpapers.isEmpty()) {
            binding.initialLoadingLayout.setVisibility(View.VISIBLE);
            binding.mainContentScroll.setVisibility(View.GONE);

            binding.getRoot().postDelayed(() -> loadInitialData(), 0);
        } else {
            binding.initialLoadingLayout.setVisibility(View.GONE);
            binding.mainContentScroll.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
            carouselAdapter.notifyDataSetChanged();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        observeViewModel(homeViewModel);
    }

    private void initializeComponents() {
        recyclerView = binding.wallpaperRecyclerView;
        carouselRecyclerView = binding.carouselRecyclerView;
        swipeRefreshLayout = binding.swipeRefreshLayout;
        initialLoadingLayout = binding.initialLoadingLayout;

        wallpapers = new ArrayList<>();
        carouselWallpapers = new ArrayList<>();

        Cache cache = new Cache(requireContext().getCacheDir(), 50 * 1024 * 1024);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(cache)
                .build();

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(
                com.google.android.material.R.color.material_dynamic_primary10,
                com.google.android.material.R.color.material_dynamic_primary20,
                com.google.android.material.R.color.material_dynamic_primary30
        );
    }

    private void setupRecyclerViews() {
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);

        recyclerView.setLayoutManager(layoutManager);
        adapter = new WallpaperAdapter(wallpapers, this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new WallpaperItemDecoration(16));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!isLoading && hasMoreData && dy > 0) {
                    StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int[] lastVisibleItems = layoutManager.findLastVisibleItemPositions(null);
                        int lastVisible = getMaxFromArray(lastVisibleItems);

                        if (lastVisible >= wallpapers.size() - PRELOAD_THRESHOLD) {
                            loadMoreWallpapers();
                        }
                    }
                }
            }
        });

        CarouselAdapter.setupCarousel(carouselRecyclerView);
        carouselAdapter = new CarouselAdapter(carouselWallpapers, this);
        carouselRecyclerView.setAdapter(carouselAdapter);
    }

    private int getMaxFromArray(int[] array) {
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    private void loadInitialData() {
        resetPaginationState();
        loadWallpapers(true);
    }

    private void refreshData() {
        resetPaginationState();
        wallpapers.clear();
        carouselWallpapers.clear();
        adapter.notifyDataSetChanged();
        carouselAdapter.notifyDataSetChanged();

        binding.mainContentScroll.setVisibility(View.GONE);
        loadWallpapers(true);
    }

    private void resetPaginationState() {
        currentPage = 0;
        totalLoadedItems = 0;
        hasMoreData = true;
        isLoading = false;
    }

    private void loadMoreWallpapers() {
        if (!isLoading && hasMoreData) {
            currentPage++;
            loadWallpapers(false);
        }
    }

    private void loadWallpapers(boolean isInitialLoad) {
        if (isLoading) return;

        isLoading = true;

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> setLoadingState(isInitialLoad));
        }

        int offset = currentPage * PAGE_SIZE;
        String url = BuildConfig.SUPABASE_URL + "/rest/v1/wallpapers?select=*,users(username)" +
                "&order=created_at.desc" +
                "&limit=" + PAGE_SIZE +
                "&offset=" + offset;

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json");

        if (!isInitialLoad) {
            requestBuilder.addHeader("Cache-Control", "max-age=300");
        }

        Request request = requestBuilder.build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        setLoadingState(false);
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        new android.os.Handler().postDelayed(() -> {
                            isLoading = false;

                            if (response.isSuccessful()) {
                                parseWallpapers(responseBody, isInitialLoad);
                                setLoadingState(false);
                                showMainContent();
                            } else {
                                setLoadingState(false);
                            }
                        }, 3000);
                    });
                }
            }
        });
    }

    private void parseWallpapers(String jsonResponse, boolean isInitialLoad) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            int newItemsCount = jsonArray.length();

            if (newItemsCount < PAGE_SIZE) {
                hasMoreData = false;
            }

            List<Wallpaper> newWallpapers = new ArrayList<>();

            for (int i = 0; i < newItemsCount; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Wallpaper wallpaper = new Wallpaper();
                wallpaper.setId(jsonObject.optString("id"));
                wallpaper.setName(jsonObject.optString("name"));
                wallpaper.setDescription(jsonObject.optString("description"));
                wallpaper.setImageUrl(jsonObject.optString("image_url"));
                wallpaper.setFileName(jsonObject.optString("file_name"));
                wallpaper.setCreatedAt(jsonObject.optString("created_at"));

                JSONObject usersObject = jsonObject.optJSONObject("users");
                if (usersObject != null) {
                    String username = usersObject.optString("username", "Anonymous");
                    wallpaper.setUsername(username);
                } else {
                    wallpaper.setUsername("Anonymous");
                }

                JSONArray tagsArray = jsonObject.optJSONArray("tags");
                if (tagsArray != null) {
                    List<String> tags = new ArrayList<>();
                    for (int j = 0; j < tagsArray.length(); j++) {
                        tags.add(tagsArray.getString(j));
                    }
                    wallpaper.setTags(tags);
                }

                JSONArray colorPaletteArray = jsonObject.optJSONArray("color_palette");
                if (colorPaletteArray != null) {
                    List<String> colorPalette = new ArrayList<>();
                    for (int j = 0; j < colorPaletteArray.length(); j++) {
                        colorPalette.add(colorPaletteArray.getString(j));
                    }
                    wallpaper.setColorPalette(colorPalette);
                }

                newWallpapers.add(wallpaper);
            }

            int insertPosition = wallpapers.size();
            wallpapers.addAll(newWallpapers);
            totalLoadedItems += newItemsCount;

            if (isInitialLoad && !newWallpapers.isEmpty()) {
                carouselWallpapers.clear();
                int carouselCount = Math.min(5, newWallpapers.size());
                for (int i = 0; i < carouselCount; i++) {
                    carouselWallpapers.add(newWallpapers.get(i));
                }
                carouselAdapter.notifyDataSetChanged();
            }

            if (isInitialLoad) {
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyItemRangeInserted(insertPosition, newItemsCount);
            }

        } catch (Exception e) {
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (getActivity() == null || binding == null) return;

        getActivity().runOnUiThread(() -> {
            if (isLoading && currentPage == 0) {
                if (!swipeRefreshLayout.isRefreshing()) {
                    binding.initialLoadingLayout.setVisibility(View.VISIBLE);
                    binding.mainContentScroll.setVisibility(View.GONE);
                }
            } else {
                binding.initialLoadingLayout.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showMainContent() {
        if (getActivity() == null || binding == null) return;

        getActivity().runOnUiThread(() -> {
            binding.mainContentScroll.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onWallpaperClick(Wallpaper wallpaper, int position) {
        Intent intent = new Intent(getContext(), WallpaperDetailActivity.class);
        intent.putExtra("wallpaper", wallpaper);
        startActivity(intent);
    }

    @Override
    public void onWallpaperLongClick(Wallpaper wallpaper, int position) {
    }

    @Override
    public void onCarouselItemClick(Wallpaper wallpaper, int position) {
        Intent intent = new Intent(getContext(), WallpaperDetailActivity.class);
        intent.putExtra("wallpaper", wallpaper);
        startActivity(intent);
    }

    private void observeViewModel(HomeViewModel homeViewModel) {
        homeViewModel.getWallpapers().observe(getViewLifecycleOwner(), wallpaperList -> {
            if (wallpaperList != null && !wallpaperList.isEmpty()) {
                wallpapers.clear();
                wallpapers.addAll(wallpaperList);
                adapter.notifyDataSetChanged();
                isDataLoaded = true;
                showMainContent();
            }
        });

        homeViewModel.getCarouselWallpapers().observe(getViewLifecycleOwner(), carouselList -> {
            if (carouselList != null && !carouselList.isEmpty()) {
                carouselWallpapers.clear();
                carouselWallpapers.addAll(carouselList);
                carouselAdapter.notifyDataSetChanged();
            }
        });

        homeViewModel.getLoadingState().observe(getViewLifecycleOwner(), isLoading -> {
            setLoadingState(isLoading != null && isLoading);
            if (isLoading == null || !isLoading) {
                showMainContent();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter != null && wallpapers.size() > 0) {
            int visibleItems = 10;
            adapter.preloadImages(wallpapers.size() - visibleItems, visibleItems);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        ImageCacheManager.getInstance(requireContext()).cancelPreloadTasks();
    }
}
package com.example.relwallpaper.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.R;
import com.example.relwallpaper.databinding.ActivitySearchResultsBinding;
import com.example.relwallpaper.ui.home.WallpaperItemDecoration;
import com.example.relwallpaper.ui.home.adapter.WallpaperAdapter;
import com.example.relwallpaper.ui.home.model.Wallpaper;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.loadingindicator.LoadingIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchResultsActivity extends AppCompatActivity implements WallpaperAdapter.OnWallpaperClickListener {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String TAG = "SearchResultsActivity";
    public static final String EXTRA_SEARCH_QUERY = "search_query";

    private ActivitySearchResultsBinding binding;
    private WallpaperAdapter adapter;
    private List<Wallpaper> wallpapers;
    private List<Wallpaper> allWallpapers;
    private OkHttpClient httpClient;
    private String accessToken;
    private String searchQuery;

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private MaterialButton backButton;
    private TextView toolbarTitle;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LoadingIndicator progressIndicator;
    private View emptyState;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySearchResultsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            searchQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
        }

        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);
        backButton = findViewById(R.id.back_button_search);
        toolbarTitle = findViewById(R.id.toolbar_title_search);

        String titleText = searchQuery != null && !searchQuery.isEmpty() ?
                "Results for \"" + searchQuery + "\"" : "Search Results";
        toolbarTitle.setText(titleText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        collapsingToolbar.setTitle(titleText);
        backButton.setOnClickListener(v -> onBackPressed());
        setupCollapsingToolbarTitleAnimation(appBarLayout);

        accessToken = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("supabase_access_token", null);

        if (accessToken == null) {
            Toast.makeText(this, "Authentication token missing. Please log in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeComponents();
        setupRecyclerView();
        loadAndSearchWallpapers();
    }

    private void setupCollapsingToolbarTitleAnimation(AppBarLayout appBarLayout) {
        appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
            int totalScrollRange = appBarLayout1.getTotalScrollRange();
            float percentage = Math.abs(verticalOffset) / (float) totalScrollRange;

            String titleText = searchQuery != null && !searchQuery.isEmpty() ?
                    "Results for \"" + searchQuery + "\"" : "Search Results";

            if (percentage > 0.7f) {
                toolbarTitle.setVisibility(View.VISIBLE);
                float alpha = (percentage - 0.7f) / 0.3f;
                toolbarTitle.setAlpha(alpha);
                collapsingToolbar.setTitle("");
            } else {
                toolbarTitle.setVisibility(View.INVISIBLE);
                toolbarTitle.setAlpha(0f);
                collapsingToolbar.setTitle(titleText);
            }
        });
    }

    private void initializeComponents() {
        recyclerView = binding.wallpaperRecyclerView;
        swipeRefreshLayout = binding.swipeRefreshLayout;
        progressIndicator = binding.progressIndicator;
        emptyState = binding.emptyState;
        emptyStateText = binding.emptyStateText;

        wallpapers = new ArrayList<>();
        allWallpapers = new ArrayList<>();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        swipeRefreshLayout.setOnRefreshListener(this::loadAndSearchWallpapers);
        swipeRefreshLayout.setColorSchemeResources(
                com.google.android.material.R.color.material_dynamic_primary10,
                com.google.android.material.R.color.material_dynamic_primary20,
                com.google.android.material.R.color.material_dynamic_primary30
        );
    }

    private void setupRecyclerView() {
        adapter = new WallpaperAdapter(wallpapers, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.StaggeredGridLayoutManager(2, androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL));
        recyclerView.addItemDecoration(new WallpaperItemDecoration(16));
    }

    private void loadAndSearchWallpapers() {
        setLoadingState(true);

        String url = SUPABASE_URL + "/rest/v1/wallpapers?select=*,users(username)&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    showEmptyState(true, "Failed to load wallpapers: " + e.getMessage());
                    Toast.makeText(SearchResultsActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                runOnUiThread(() -> {
                    setLoadingState(false);
                    if (response.isSuccessful()) {
                        parseAndSearchWallpapers(responseBody);
                    } else {
                        showEmptyState(true, "Failed to load wallpapers. Error: " + response.code());
                        Toast.makeText(SearchResultsActivity.this, "Failed: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void parseAndSearchWallpapers(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            allWallpapers.clear();
            wallpapers.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Wallpaper wallpaper = new Wallpaper();
                wallpaper.setId(jsonObject.optString("id"));
                wallpaper.setName(jsonObject.optString("name"));
                wallpaper.setDescription(jsonObject.optString("description"));
                wallpaper.setImageUrl(jsonObject.optString("image_url"));
                wallpaper.setFileName(jsonObject.optString("file_name"));
                wallpaper.setCreatedAt(jsonObject.optString("created_at"));

                JSONObject userObj = jsonObject.optJSONObject("users");
                wallpaper.setUsername(userObj != null ? userObj.optString("username", "Anonymous") : "Anonymous");

                JSONArray tagsArray = jsonObject.optJSONArray("tags");
                if (tagsArray != null) {
                    List<String> tags = new ArrayList<>();
                    for (int j = 0; j < tagsArray.length(); j++) {
                        tags.add(tagsArray.getString(j));
                    }
                    wallpaper.setTags(tags);
                }

                JSONArray paletteArray = jsonObject.optJSONArray("color_palette");
                if (paletteArray != null) {
                    List<String> colorPalette = new ArrayList<>();
                    for (int j = 0; j < paletteArray.length(); j++) {
                        colorPalette.add(paletteArray.getString(j));
                    }
                    wallpaper.setColorPalette(colorPalette);
                }

                allWallpapers.add(wallpaper);
            }

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                filterWallpapers(searchQuery.trim());
            } else {
                wallpapers.addAll(allWallpapers);
            }

            adapter.notifyDataSetChanged();

            if (wallpapers.isEmpty()) {
                String emptyMessage = "No wallpapers available";
                showEmptyState(true, emptyMessage);
            } else {
                showEmptyState(false, "");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing wallpapers", e);
            Toast.makeText(this, "Error parsing wallpapers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showEmptyState(true, "Error loading wallpapers");
        }
    }

    private void filterWallpapers(String query) {
        wallpapers.clear();
        String lowerQuery = query.toLowerCase().trim();

        for (Wallpaper wallpaper : allWallpapers) {
            boolean matches = false;

            if (wallpaper.getName() != null &&
                    wallpaper.getName().toLowerCase().contains(lowerQuery)) {
                matches = true;
            }

            if (!matches && wallpaper.getDescription() != null &&
                    wallpaper.getDescription().toLowerCase().contains(lowerQuery)) {
                matches = true;
            }

            if (!matches && wallpaper.getTags() != null) {
                for (String tag : wallpaper.getTags()) {
                    if (tag.toLowerCase().contains(lowerQuery)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (!matches && wallpaper.getUsername() != null &&
                    wallpaper.getUsername().toLowerCase().contains(lowerQuery)) {
                matches = true;
            }

            if (matches) {
                wallpapers.add(wallpaper);
            }
        }
    }

    public void updateSearchQuery(String newQuery) {
        this.searchQuery = newQuery;

        String titleText = newQuery != null && !newQuery.isEmpty() ?
                "Results for \"" + newQuery + "\"" : "Search Results";
        toolbarTitle.setText(titleText);
        collapsingToolbar.setTitle(titleText);

        if (!allWallpapers.isEmpty()) {
            if (newQuery != null && !newQuery.trim().isEmpty()) {
                filterWallpapers(newQuery.trim());
            } else {
                wallpapers.clear();
                wallpapers.addAll(allWallpapers);
            }

            adapter.notifyDataSetChanged();

            if (wallpapers.isEmpty()) {
                String emptyMessage = "No wallpapers available";
                showEmptyState(true, emptyMessage);
            } else {
                showEmptyState(false, "");
            }
        } else {
            loadAndSearchWallpapers();
        }
    }

    private void setLoadingState(boolean isLoading) {
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showEmptyState(boolean show, String message) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);

        if (show && emptyStateText != null) {
            emptyStateText.setText(message);
        }
    }

    @Override
    public void onWallpaperClick(Wallpaper wallpaper, int position) {
        Intent intent = new Intent(this, WallpaperDetailActivity.class);
        intent.putExtra("wallpaper", wallpaper);
        startActivity(intent);
    }

    @Override
    public void onWallpaperLongClick(Wallpaper wallpaper, int position) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
        binding = null;
    }
}
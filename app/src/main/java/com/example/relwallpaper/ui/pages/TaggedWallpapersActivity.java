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
import com.example.relwallpaper.databinding.ActivityTaggedWallpaperBinding;
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

public class TaggedWallpapersActivity extends AppCompatActivity implements WallpaperAdapter.OnWallpaperClickListener {

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String TAG = "TaggedWallpapersAct";
    public static final String EXTRA_TAG_NAME = "tag_name";

    private ActivityTaggedWallpaperBinding binding;
    private WallpaperAdapter adapter;
    private List<Wallpaper> wallpapers;
    private OkHttpClient httpClient;
    private String accessToken;
    private String tagName;

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private MaterialButton backButton;
    private TextView toolbarTitle;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LoadingIndicator progressIndicator;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTaggedWallpaperBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            tagName = getIntent().getStringExtra(EXTRA_TAG_NAME);
        }

        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);
        backButton = findViewById(R.id.back_button_tw);
        toolbarTitle = findViewById(R.id.toolbar_title_tw);

        toolbarTitle.setText(tagName != null ? tagName + " Wallpapers" : "Wallpapers");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        collapsingToolbar.setTitle(tagName != null ? tagName + " Wallpapers" : "Wallpapers");
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
        loadWallpapersByTag();
    }

    private void setupCollapsingToolbarTitleAnimation(AppBarLayout appBarLayout) {
        appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
            int totalScrollRange = appBarLayout1.getTotalScrollRange();
            float percentage = Math.abs(verticalOffset) / (float) totalScrollRange;

            if (percentage > 0.7f) {
                toolbarTitle.setVisibility(View.VISIBLE);
                float alpha = (percentage - 0.7f) / 0.3f;
                toolbarTitle.setAlpha(alpha);
                collapsingToolbar.setTitle("");
            } else {
                toolbarTitle.setVisibility(View.INVISIBLE);
                toolbarTitle.setAlpha(0f);
                collapsingToolbar.setTitle(tagName != null ? tagName + " Wallpapers" : "Wallpapers");
            }
        });
    }

    private void initializeComponents() {
        recyclerView = binding.wallpaperRecyclerView;
        swipeRefreshLayout = binding.swipeRefreshLayout;
        progressIndicator = binding.progressIndicator;
        emptyState = binding.emptyState;

        wallpapers = new ArrayList<>();
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        swipeRefreshLayout.setOnRefreshListener(this::loadWallpapersByTag);
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

    private void loadWallpapersByTag() {
        if (tagName == null || tagName.isEmpty()) {
            Log.e(TAG, "Tag name is null or empty.");
            Toast.makeText(this, "Error: No tag specified.", Toast.LENGTH_SHORT).show();
            setLoadingState(false);
            return;
        }

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
                    showEmptyState(true);
                    Toast.makeText(TaggedWallpapersActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                runOnUiThread(() -> {
                    setLoadingState(false);
                    if (response.isSuccessful()) {
                        parseAndFilterWallpapers(responseBody);
                    } else {
                        showEmptyState(true);
                        Toast.makeText(TaggedWallpapersActivity.this, "Failed: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void parseAndFilterWallpapers(String jsonResponse) {
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            wallpapers.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                JSONArray tagsArray = jsonObject.optJSONArray("tags");
                boolean hasTag = false;
                if (tagsArray != null) {
                    for (int j = 0; j < tagsArray.length(); j++) {
                        if (tagsArray.getString(j).equalsIgnoreCase(tagName)) {
                            hasTag = true;
                            break;
                        }
                    }
                }

                if (hasTag) {
                    Wallpaper wallpaper = new Wallpaper();
                    wallpaper.setId(jsonObject.optString("id"));
                    wallpaper.setName(jsonObject.optString("name"));
                    wallpaper.setDescription(jsonObject.optString("description"));
                    wallpaper.setImageUrl(jsonObject.optString("image_url"));
                    wallpaper.setFileName(jsonObject.optString("file_name"));
                    wallpaper.setCreatedAt(jsonObject.optString("created_at"));

                    JSONObject userObj = jsonObject.optJSONObject("users");
                    wallpaper.setUsername(userObj != null ? userObj.optString("username", "Anonymous") : "Anonymous");

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

                    wallpapers.add(wallpaper);
                }
            }

            adapter.notifyDataSetChanged();
            showEmptyState(wallpapers.isEmpty());

        } catch (Exception e) {
            Toast.makeText(this, "Error parsing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showEmptyState(true);
        }
    }

    private void setLoadingState(boolean isLoading) {
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
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

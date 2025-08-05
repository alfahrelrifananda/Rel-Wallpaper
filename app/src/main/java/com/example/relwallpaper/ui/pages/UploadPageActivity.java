package com.example.relwallpaper.ui.pages;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.R;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.example.relwallpaper.ui.home.WallpaperItemDecoration;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.loadingindicator.LoadingIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadPageActivity extends AppCompatActivity {
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String TAG = "UploadPageActivity";

    private RecyclerView wallpaperRecyclerView;
    private WallpaperAdapter wallpaperAdapter;
    private LoadingIndicator progressIndicator;
    private TextView emptyStateText;
    private LinearLayout emptyStateLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private MaterialButton backButton;
    private TextView toolbarTitle;

    private OkHttpClient httpClient;
    private String accessToken;
    private String currentUserId;
    private List<Wallpaper> wallpaperList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_page);

        accessToken = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("supabase_access_token", null);
        currentUserId = getUserIdFromToken(accessToken);

        if (accessToken == null || currentUserId == null || isTokenExpired(accessToken)) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupSwipeRefresh();
        loadUserWallpapers();
    }

    private void initializeViews() {
        wallpaperRecyclerView = findViewById(R.id.wallpaper_recycler_view);
        progressIndicator = findViewById(R.id.progress_indicator);
        emptyStateText = findViewById(R.id.empty_state_text);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);
        backButton = findViewById(R.id.back_button);
        toolbarTitle = findViewById(R.id.toolbar_title);

        wallpaperList = new ArrayList<>();
        wallpaperAdapter = new WallpaperAdapter(wallpaperList);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        wallpaperRecyclerView.setLayoutManager(layoutManager);
        wallpaperRecyclerView.setAdapter(wallpaperAdapter);

        wallpaperRecyclerView.addItemDecoration(new WallpaperItemDecoration(16));
    }

    private void setupToolbar() {
        toolbarTitle.setText("My Uploads");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        collapsingToolbar.setTitle("My Uploads");
        setupCollapsingToolbarTitleAnimation(appBarLayout);
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
                collapsingToolbar.setTitle("My Uploads");
            }
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadUserWallpapers);
            swipeRefreshLayout.setColorSchemeResources(
                    com.google.android.material.R.color.material_dynamic_primary10,
                    com.google.android.material.R.color.material_dynamic_primary20,
                    com.google.android.material.R.color.material_dynamic_primary30
            );
        }
    }

    private void loadUserWallpapers() {
        setLoadingState(true);

        String url = SUPABASE_URL + "/rest/v1/wallpapers?user_id=eq." + currentUserId + "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    showError("Failed to load wallpapers: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";

                runOnUiThread(() -> {
                    setLoadingState(false);
                    if (response.isSuccessful()) {
                        parseWallpapers(responseBody);
                    } else {
                        showError("Failed to load wallpapers: " + response.code());
                    }
                });
            }
        });
    }

    private void parseWallpapers(String jsonResponse) {
        try {
            JSONArray wallpapers = new JSONArray(jsonResponse);
            wallpaperList.clear();

            for (int i = 0; i < wallpapers.length(); i++) {
                JSONObject wallpaperObj = wallpapers.getJSONObject(i);
                Wallpaper wallpaper = new Wallpaper();
                wallpaper.id = wallpaperObj.optString("id");
                wallpaper.name = wallpaperObj.optString("name");
                wallpaper.description = wallpaperObj.optString("description");
                wallpaper.imageUrl = wallpaperObj.optString("image_url");
                wallpaper.fileName = wallpaperObj.optString("file_name");
                wallpaper.createdAt = wallpaperObj.optString("created_at");

                JSONArray tagsArray = wallpaperObj.optJSONArray("tags");
                if (tagsArray != null) {
                    wallpaper.tags = new ArrayList<>();
                    for (int j = 0; j < tagsArray.length(); j++) {
                        wallpaper.tags.add(tagsArray.getString(j));
                    }
                }

                wallpaperList.add(wallpaper);
            }

            wallpaperAdapter.notifyDataSetChanged();
            updateEmptyState();

        } catch (Exception e) {
            showError("Error parsing wallpapers");
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = wallpaperList.isEmpty();

        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            wallpaperRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            wallpaperRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void deleteWallpaper(Wallpaper wallpaper, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Wallpaper")
                .setMessage("Are you sure you want to delete \"" + wallpaper.name + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDeleteWallpaper(wallpaper, position);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                })
                .show();
    }

    private void performDeleteWallpaper(Wallpaper wallpaper, int position) {
        setLoadingState(true);

        String deleteUrl = SUPABASE_URL + "/rest/v1/wallpapers?id=eq." + wallpaper.id;

        Request deleteRequest = new Request.Builder()
                .url(deleteUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .build();

        httpClient.newCall(deleteRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    showError("Failed to delete wallpaper: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";

                if (response.isSuccessful()) {
                    deleteFromStorage(wallpaper.fileName, position);
                } else {
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        showError("Failed to delete wallpaper from database. Response: " + response.code());
                    });
                }
            }
        });
    }

    private void deleteFromStorage(String fileName, int position) {
        String storageDeleteUrl = SUPABASE_URL + "/storage/v1/object/wallpapers/" + fileName;

        Request storageDeleteRequest = new Request.Builder()
                .url(storageDeleteUrl)
                .delete()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .build();

        httpClient.newCall(storageDeleteRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    if (position >= 0 && position < wallpaperList.size()) {
                        wallpaperList.remove(position);
                        wallpaperAdapter.notifyItemRemoved(position);
                        updateEmptyState();
                    }
                    Toast.makeText(UploadPageActivity.this, "Wallpaper deleted successfully (storage cleanup failed)", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";

                runOnUiThread(() -> {
                    setLoadingState(false);
                    if (position >= 0 && position < wallpaperList.size()) {
                        wallpaperList.remove(position);
                        wallpaperAdapter.notifyItemRemoved(position);
                        updateEmptyState();
                    }
                    Toast.makeText(UploadPageActivity.this, "Wallpaper deleted successfully", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadUserWallpapers();
        }
    }

    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            wallpaperRecyclerView.setEnabled(!isLoading);

            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing() && !isLoading) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return true;
            }
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            long exp = json.getLong("exp");
            long now = System.currentTimeMillis() / 1000;
            boolean expired = exp < now;
            return expired;
        } catch (Exception e) {
            return true;
        }
    }

    private String getUserIdFromToken(String token) {
        try {
            if (token == null) {
                return null;
            }
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            String userId = json.optString("sub", null);
            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }

    private static class Wallpaper {
        String id;
        String name;
        String description;
        String imageUrl;
        String fileName;
        String createdAt;
        List<String> tags;
    }

    private class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {
        private List<Wallpaper> wallpapers;

        public WallpaperAdapter(List<Wallpaper> wallpapers) {
            this.wallpapers = wallpapers;
        }

        @NonNull
        @Override
        public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_upload_wallpaper, parent, false);
            return new WallpaperViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull WallpaperViewHolder holder, int position) {
            Wallpaper wallpaper = wallpapers.get(position);
            holder.bind(wallpaper, position);
        }

        @Override
        public int getItemCount() {
            return wallpapers.size();
        }

        class WallpaperViewHolder extends RecyclerView.ViewHolder {
            private ImageView wallpaperImage;
            private TextView wallpaperName;
            private TextView wallpaperDate;
            private TextView wallpaperTags;
            private MaterialButton deleteButton;
            private MaterialCardView cardView;

            public WallpaperViewHolder(@NonNull View itemView) {
                super(itemView);
                wallpaperImage = itemView.findViewById(R.id.wallpaper_image);
                wallpaperName = itemView.findViewById(R.id.wallpaper_name);
                wallpaperDate = itemView.findViewById(R.id.wallpaper_date);
                wallpaperTags = itemView.findViewById(R.id.wallpaper_tags);
                deleteButton = itemView.findViewById(R.id.delete_button);
                cardView = itemView.findViewById(R.id.card_view);
            }

            public void bind(Wallpaper wallpaper, int position) {
                wallpaperName.setText(wallpaper.name);

                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                    Date date = inputFormat.parse(wallpaper.createdAt);
                    wallpaperDate.setText(outputFormat.format(date));
                } catch (Exception e) {
                    wallpaperDate.setText(wallpaper.createdAt);
                }

                if (wallpaper.tags != null && !wallpaper.tags.isEmpty()) {
                    wallpaperTags.setText(String.join(", ", wallpaper.tags));
                    wallpaperTags.setVisibility(View.VISIBLE);
                } else {
                    wallpaperTags.setVisibility(View.GONE);
                }

                Glide.with(itemView.getContext())
                        .load(wallpaper.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(wallpaperImage);

                deleteButton.setOnClickListener(v -> {
                    deleteWallpaper(wallpaper, position);
                });

                cardView.setOnClickListener(v -> {
                    Toast.makeText(itemView.getContext(), "Preview: " + wallpaper.name, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
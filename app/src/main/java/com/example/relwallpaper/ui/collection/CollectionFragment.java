package com.example.relwallpaper.ui.collection;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.R;
import com.example.relwallpaper.databinding.FragmentCollectionBinding;
import com.example.relwallpaper.ui.pages.TaggedWallpapersActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.example.relwallpaper.ui.collection.CollectionAdapter;
import com.example.relwallpaper.ui.collection.CollectionItem;

public class CollectionFragment extends Fragment implements CollectionAdapter.OnCollectionClickListener {

    private static final String TAG = "CollectionFragment";
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;

    private FragmentCollectionBinding binding;
    private CollectionAdapter adapter;
    private OkHttpClient httpClient;
    private String accessToken;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CollectionViewModel collectionViewModel =
                new ViewModelProvider(this).get(CollectionViewModel.class);

        binding = FragmentCollectionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        accessToken = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("supabase_access_token", null);

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        setupRecyclerView();
        setupPredefinedTags();
        fetchWallpaperCounts();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new CollectionAdapter(getContext());
        adapter.setOnCollectionClickListener(this);
        binding.tagsRecyclerView.setAdapter(adapter);
        binding.tagsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
    }

    private void setupPredefinedTags() {
        CollectionItem[] predefinedCollections = {
                new CollectionItem("For Fun", "Playful and entertaining wallpapers for casual moments", 0),
                new CollectionItem("Doodle", "Hand-drawn artistic sketches and creative doodles", 0),
                new CollectionItem("Nature", "Beautiful landscapes, flora, and natural scenery", 0),
                new CollectionItem("Shape", "Geometric patterns and abstract forms", 0),
                new CollectionItem("Retro", "Vintage-inspired designs with nostalgic appeal", 0),
                new CollectionItem("Branding", "Corporate and business-focused wallpapers", 0),
                new CollectionItem("Motif", "Decorative patterns and recurring design elements", 0),
                new CollectionItem("Blob", "Organic fluid shapes and abstract blobs", 0),
                new CollectionItem("Gradients", "Smooth color transitions and gradient effects", 0),
                new CollectionItem("Elements", "Basic design components and UI elements", 0),
                new CollectionItem("Art & Culture", "Artistic masterpieces and cultural references", 0),
                new CollectionItem("Blurred", "Soft focus and artistic blur effects", 0)
        };
        adapter.setInitialCollections(predefinedCollections);
    }


    private void fetchWallpaperCounts() {
        if (accessToken == null) {
            Log.e(TAG, "No access token available");
            return;
        }

        String url = SUPABASE_URL + "/rest/v1/wallpapers?select=tags";
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
                Log.e(TAG, "Failed to fetch wallpaper counts: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to load wallpaper counts", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to fetch wallpaper counts: HTTP " + response.code());
                    return;
                }

                try {
                    JSONArray wallpapers = new JSONArray(responseBody);
                    Map<String, Integer> tagCounts = new HashMap<>();

                    String[] predefinedTags = {
                            "For Fun", "Doodle", "Nature", "Shape", "Retro",
                            "Branding", "Motif", "Blob", "Gradients", "Elements",
                            "Art & Culture", "Blurred"
                    };
                    for (String tag : predefinedTags) {
                        tagCounts.put(tag.toLowerCase().trim(), 0);
                    }

                    for (int i = 0; i < wallpapers.length(); i++) {
                        JSONObject wallpaper = wallpapers.getJSONObject(i);
                        JSONArray tags = wallpaper.optJSONArray("tags");
                        if (tags != null) {
                            for (int j = 0; j < tags.length(); j++) {
                                String tag = tags.getString(j);
                                String normalizedTag = tag.toLowerCase().trim();
                                if (tagCounts.containsKey(normalizedTag)) {
                                    tagCounts.put(normalizedTag, tagCounts.get(normalizedTag) + 1);
                                }
                            }
                        }
                    }

                    Map<String, Integer> finalTagCounts = new HashMap<>();
                    for (String tag : predefinedTags) {
                        finalTagCounts.put(tag, tagCounts.getOrDefault(tag.toLowerCase().trim(), 0));
                    }


                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateCollectionCounts(finalTagCounts);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing wallpaper counts response: " + e.getMessage(), e);
                }
            }
        });
    }

    private void updateCollectionCounts(Map<String, Integer> tagCounts) {
        CollectionItem[] updatedCollections = {
                new CollectionItem("For Fun", "Playful and entertaining wallpapers for casual moments",
                        tagCounts.getOrDefault("For Fun", 0)),
                new CollectionItem("Doodle", "Hand-drawn artistic sketches and creative doodles",
                        tagCounts.getOrDefault("Doodle", 0)),
                new CollectionItem("Nature", "Beautiful landscapes, flora, and natural scenery",
                        tagCounts.getOrDefault("Nature", 0)),
                new CollectionItem("Shape", "Geometric patterns and abstract forms",
                        tagCounts.getOrDefault("Shape", 0)),
                new CollectionItem("Retro", "Vintage-inspired designs with nostalgic appeal",
                        tagCounts.getOrDefault("Retro", 0)),
                new CollectionItem("Branding", "Corporate and business-focused wallpapers",
                        tagCounts.getOrDefault("Branding", 0)),
                new CollectionItem("Motif", "Decorative patterns and recurring design elements",
                        tagCounts.getOrDefault("Motif", 0)),
                new CollectionItem("Blob", "Organic fluid shapes and abstract blobs",
                        tagCounts.getOrDefault("Blob", 0)),
                new CollectionItem("Gradients", "Smooth color transitions and gradient effects",
                        tagCounts.getOrDefault("Gradients", 0)),
                new CollectionItem("Elements", "Basic design components and UI elements",
                        tagCounts.getOrDefault("Elements", 0)),
                new CollectionItem("Art & Culture", "Artistic masterpieces and cultural references",
                        tagCounts.getOrDefault("Art & Culture", 0)),
                new CollectionItem("Blurred", "Soft focus and artistic blur effects",
                        tagCounts.getOrDefault("Blurred", 0))
        };
        adapter.updateCollections(updatedCollections);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }

    @Override
    public void onCollectionClick(CollectionItem collection) {
        Intent intent = new Intent(getContext(), TaggedWallpapersActivity.class);
        intent.putExtra(TaggedWallpapersActivity.EXTRA_TAG_NAME, collection.getTitle());
        startActivity(intent);
    }
}

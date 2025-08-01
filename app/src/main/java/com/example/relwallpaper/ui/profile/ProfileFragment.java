package com.example.relwallpaper.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.R;
import com.example.relwallpaper.databinding.FragmentProfileBinding;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.example.relwallpaper.ui.pages.AddWallpaperActivity;
import com.example.relwallpaper.ui.pages.HelpAndSupportActivity;
import com.example.relwallpaper.ui.pages.NotificationActivity;
import com.example.relwallpaper.ui.pages.ProfilePageActivity;
import com.example.relwallpaper.ui.pages.SettingsActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String TAG = "ProfileFragment";
    private final OkHttpClient httpClient = new OkHttpClient();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        hideAdminElements(root);

        checkAdminStatusAndSetupUI(root);
        setupNavigation(root);

        return root;
    }

    private void hideAdminElements(View root) {
        FloatingActionButton addWallpaperFab = root.findViewById(R.id.add_wallpaper_fab);
        MaterialCardView uploadsCard = root.findViewById(R.id.uploads_card);

        if (addWallpaperFab != null) {
            addWallpaperFab.setVisibility(View.GONE);
        }

        if (uploadsCard != null) {
            uploadsCard.setVisibility(View.GONE);
        }
    }

    private void checkAdminStatusAndSetupUI(View root) {
        SharedPreferences prefs = getActivity().getSharedPreferences("auth", getActivity().MODE_PRIVATE);
        String accessToken = prefs.getString("supabase_access_token", null);

        if (accessToken != null) {
            String currentUserId = getUserIdFromToken(accessToken);
            if (currentUserId != null) {
                checkUserAdminStatus(currentUserId, accessToken, root);
            }
        }
    }

    private String getUserIdFromToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject tokenData = new JSONObject(payload);
            return tokenData.optString("sub");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JWT token: " + e.getMessage());
            return null;
        }
    }

    private void checkUserAdminStatus(String userId, String accessToken, View root) {
        String userUrl = SUPABASE_URL + "/rest/v1/users?id=eq." + userId + "&select=isAdmin";

        Request request = new Request.Builder()
                .url(userUrl)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to check admin status: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray usersArray = new JSONArray(responseBody);

                        if (usersArray.length() > 0) {
                            JSONObject user = usersArray.getJSONObject(0);
                            boolean isAdmin = user.optBoolean("isAdmin", false);

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    setupUIBasedOnAdminStatus(isAdmin, root);
                                });
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing admin status response: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Failed to fetch admin status: " + response.code());
                }
            }
        });
    }

    private void setupUIBasedOnAdminStatus(boolean isAdmin, View root) {
        FloatingActionButton addWallpaperFab = root.findViewById(R.id.add_wallpaper_fab);
        MaterialCardView uploadsCard = root.findViewById(R.id.uploads_card);

        if (isAdmin) {
            if (addWallpaperFab != null) {
                addWallpaperFab.setVisibility(View.VISIBLE);
                setupAddWallpaperButton(addWallpaperFab);
            }

            if (uploadsCard != null) {
                uploadsCard.setVisibility(View.VISIBLE);
                setupUploadsCard(uploadsCard);
            }
        } else {
            if (addWallpaperFab != null) {
                addWallpaperFab.setVisibility(View.GONE);
            }

            if (uploadsCard != null) {
                uploadsCard.setVisibility(View.GONE);
            }
        }
    }

    private void setupAddWallpaperButton(FloatingActionButton addWallpaperFab) {
        addWallpaperFab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddWallpaperActivity.class);
            startActivity(intent);
        });
    }

    private void setupUploadsCard(MaterialCardView uploadsCard) {
        uploadsCard.setOnClickListener(v -> {
            // TODO : Initialize upload Activity
        });
    }

    private void setupNavigation(View root) {
        MaterialCardView profileCard = root.findViewById(R.id.profile_card);
        MaterialCardView notificationCard = root.findViewById(R.id.notifications_card);
        MaterialCardView settingsCard = root.findViewById(R.id.settings_card);
        MaterialCardView helpAndSupportCard = root.findViewById(R.id.help_card);

        if (profileCard != null) {
            profileCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ProfilePageActivity.class);
                startActivity(intent);
            });
        }

        if (notificationCard != null) {
            notificationCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), NotificationActivity.class);
                startActivity(intent);
            });
        }

        if (settingsCard != null) {
            settingsCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        if (helpAndSupportCard != null) {
            helpAndSupportCard.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), HelpAndSupportActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}
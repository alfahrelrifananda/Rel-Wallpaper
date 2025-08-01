package com.example.relwallpaper.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.MainActivity;
import com.example.relwallpaper.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.*;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_AUTH_URL = SUPABASE_URL + "/auth/v1/token?grant_type=id_token";
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;

    private MaterialButton googleSignInButton;
    private GoogleSignInClient googleSignInClient;
    private final OkHttpClient httpClient = new OkHttpClient();
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        googleSignInButton = findViewById(R.id.google_sign_in_material_button);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInButton.setOnClickListener(v -> {
            googleSignInButton.setEnabled(false);
            googleSignInButton.setText(R.string.signing_in_loading);
            showLoadingDialog();

            new Handler(Looper.getMainLooper()).postDelayed(this::executeGoogleSignIn, 3000);
        });
    }

    private boolean isUserLoggedIn() {
        String accessToken = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("supabase_access_token", null);

        if (accessToken != null) {
            return true;
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        return account != null;
    }

    private void showLoadingDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        loadingDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        loadingDialog.show();
    }

    private void executeGoogleSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleSignInButton != null) {
            googleSignInButton.setEnabled(true);
            googleSignInButton.setText(R.string.sign_in_with_google_string);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();

                if (idToken != null) {
                    loginToSupabase(idToken);
                } else {
                    showError("Failed to get ID token");
                }
            } catch (ApiException e) {
                showError("Google sign-in failed: " + getErrorMessage(e.getStatusCode()));
            }
        }
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case 10:
                return "Developer error - Check SHA-1 fingerprint and OAuth client configuration";
            case 12501:
                return "User cancelled sign-in";
            case 7:
                return "Network error";
            default:
                return "Unknown error (code: " + errorCode + ")";
        }
    }

    private void loginToSupabase(String idToken) {
        try {
            JSONObject json = new JSONObject();
            json.put("provider", "google");
            json.put("id_token", idToken);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(SUPABASE_AUTH_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    showError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        try {
                            JSONObject res = new JSONObject(responseBody);
                            String accessToken = res.getString("access_token");
                            String refreshToken = res.getString("refresh_token");

                            getSharedPreferences("auth", MODE_PRIVATE)
                                    .edit()
                                    .putString("supabase_access_token", accessToken)
                                    .putString("supabase_refresh_token", refreshToken)
                                    .putLong("login_timestamp", System.currentTimeMillis())
                                    .apply();

                            getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putLong("login_time", System.currentTimeMillis())
                                    .apply();

                            createUserInDatabase(accessToken);

                        } catch (JSONException e) {
                            showError("Failed to parse authentication response");
                        }
                    } else {
                        showError("Authentication failed: " + response.message());
                    }
                }
            });

        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
        }
    }

    private void createUserInDatabase(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                navigateToMainActivity();
                return;
            }

            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject tokenData = new JSONObject(payload);

            String userId = tokenData.optString("sub");
            String email = tokenData.optString("email", "");
            String name = tokenData.optString("name", "");

            if (name.isEmpty()) {
                JSONObject userMetadata = tokenData.optJSONObject("user_metadata");
                if (userMetadata != null) {
                    name = userMetadata.optString("full_name", "");
                    if (name.isEmpty()) {
                        name = userMetadata.optString("name", "User");
                    }
                } else {
                    name = "User";
                }
            }

            checkAndCreateUser(userId, email, name, accessToken);

        } catch (Exception e) {
            runOnUiThread(() -> {
                dismissLoading();
                navigateToMainActivity();
            });
        }
    }

    private void checkAndCreateUser(String userId, String email, String name, String accessToken) {
        String checkUserUrl = BuildConfig.SUPABASE_URL + "/rest/v1/users?id=eq." + userId;

        Request checkRequest = new Request.Builder()
                .url(checkUserUrl)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                .build();

        httpClient.newCall(checkRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    dismissLoading();
                    navigateToMainActivity();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject responseJson = new JSONObject(responseBody);

                    boolean userExists;
                    if (responseJson.has("data")) {
                        userExists = responseJson.getJSONArray("data").length() > 0;
                    } else {
                        userExists = new JSONObject(responseBody).length() > 0;
                    }

                    if (!userExists) {
                        createUser(userId, email, name, accessToken);
                    } else {
                        runOnUiThread(() -> {
                            dismissLoading();
                            navigateToMainActivity();
                        });
                    }
                } catch (Exception e) {
                    createUser(userId, email, name, accessToken);
                }
            }
        });
    }

    private void createUser(String userId, String email, String name, String accessToken) {
        try {
            SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
            String createdAt = iso8601.format(new Date());

            JSONObject userData = new JSONObject();
            userData.put("id", userId);
            userData.put("email", email);
            userData.put("name", name);
            userData.put("created_at", createdAt);

            String usersUrl = BuildConfig.SUPABASE_URL + "/rest/v1/users";
            RequestBody requestBody = RequestBody.create(
                    userData.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(usersUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", BuildConfig.SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        dismissLoading();
                        navigateToMainActivity();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body().string();

                    runOnUiThread(() -> {
                        dismissLoading();
                        if (response.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                        }
                        navigateToMainActivity();
                    });
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                dismissLoading();
                navigateToMainActivity();
            });
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            dismissLoading();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            googleSignInButton.setEnabled(true);
            googleSignInButton.setText(R.string.sign_in_with_google_string);
        });
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoading();
        httpClient.dispatcher().executorService().shutdown();
    }

    public static void logout(AppCompatActivity activity) {
        activity.getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        activity.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, gso);
        googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        });
    }
}
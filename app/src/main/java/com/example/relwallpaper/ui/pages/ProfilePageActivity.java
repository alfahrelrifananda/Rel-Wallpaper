package com.example.relwallpaper.ui.pages;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.relwallpaper.BuildConfig;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.relwallpaper.R;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class ProfilePageActivity extends AppCompatActivity {

    private TextView toolbarTitle;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView userNameText, userEmailText, jwtCountdownText;
    private CountDownTimer jwtCountdownTimer;
    private AlertDialog loadingDialog;

    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String TAG = "ProfilePageActivity";
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);

        initializeViews();
        setupToolbar();
        loadUserData();
        setupListeners();
        startJwtCountdown();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title_pp);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        MaterialButton backButton = findViewById(R.id.back_button_pp);

        userNameText = findViewById(R.id.user_name_text);
        userEmailText = findViewById(R.id.user_email_text);
        jwtCountdownText = findViewById(R.id.jwt_countdown_text);

        setSupportActionBar(toolbar);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        collapsingToolbar.setTitle("Profile");

        MaterialButton backButton = findViewById(R.id.back_button_pp);
        backButton.setOnClickListener(v -> onBackPressed());

        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        setupCollapsingToolbarTitleAnimation(appBarLayout);
    }

    private void setupCollapsingToolbarTitleAnimation(AppBarLayout appBarLayout) {
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int totalScrollRange = appBarLayout.getTotalScrollRange();
                float percentage = Math.abs(verticalOffset) / (float) totalScrollRange;

                if (percentage > 0.7f) {
                    toolbarTitle.setVisibility(View.VISIBLE);
                    float alpha = (percentage - 0.7f) / 0.3f;
                    toolbarTitle.setAlpha(alpha);
                    collapsingToolbar.setTitle("");
                } else {
                    toolbarTitle.setVisibility(View.INVISIBLE);
                    toolbarTitle.setAlpha(0f);
                    collapsingToolbar.setTitle("Profile");
                }
            }
        });
    }

    private void loadUserData() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            userNameText.setText(account.getDisplayName() != null ? account.getDisplayName() : "User");
            userEmailText.setText(account.getEmail() != null ? account.getEmail() : "");
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
    }

    private void startJwtCountdown() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long loginTime = prefs.getLong("login_time", System.currentTimeMillis());

        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
        long expirationTime = loginTime + oneWeekInMillis;
        long remainingTime = expirationTime - System.currentTimeMillis();

        if (remainingTime <= 0) {
            jwtCountdownText.setText("Token expired - Please login again");
            jwtCountdownText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            return;
        }

        jwtCountdownTimer = new CountDownTimer(remainingTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished);
                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;

                String timeFormatted;
                if (days > 0) {
                    timeFormatted = String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
                } else {
                    timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                }
                jwtCountdownText.setText("Token expires in: " + timeFormatted);
            }

            @Override
            public void onFinish() {
                jwtCountdownText.setText("Token expired - Please login again");
                jwtCountdownText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }.start();
    }

    private void setupListeners() {
        MaterialCardView logoutCard = findViewById(R.id.logout_card);
        logoutCard.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout from your account?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    showLoadingDialog();
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void showLoadingDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        loadingDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        loadingDialog.show();
    }

    private void performLogout() {
        if (jwtCountdownTimer != null) {
            jwtCountdownTimer.cancel();
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            dismissLoadingDialog();
            LoginActivity.logout(this);
        }, 3000);
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    public void onBackPressedDispatcher() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (jwtCountdownTimer != null) {
            jwtCountdownTimer.cancel();
        }
        dismissLoadingDialog();
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
    }
}
package com.example.relwallpaper.ui.pages;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.relwallpaper.R;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    private TextView toolbarTitle;
    private CollapsingToolbarLayout collapsingToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title_s);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        MaterialButton backButton = findViewById(R.id.back_button_s);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        collapsingToolbar.setTitle("Settings");
        backButton.setOnClickListener(v -> onBackPressed());
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

                    collapsingToolbar.setTitle("Settings");
                }
            }
        });
    }

    public void onBackPressedDispatcher() {
        super.onBackPressed();
    }
}
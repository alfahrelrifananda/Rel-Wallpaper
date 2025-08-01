package com.example.relwallpaper.ui.pages;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.relwallpaper.R;
import com.example.relwallpaper.ui.component.WallpaperOptionBottomSheet;
import com.example.relwallpaper.ui.home.model.Wallpaper;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WallpaperDetailActivity extends AppCompatActivity {

    private static final String TAG = "WallpaperDetailActivity";
    private static final int PERMISSION_REQUEST_CODE_STORAGE = 101;
    private static final int PERMISSION_REQUEST_CODE_WALLPAPER = 102;

    private Wallpaper currentWallpaper;
    private ImageView detailWallpaperImage;
    private TextView detailWallpaperName;
    private TextView detailWallpaperUsername;
    private TextView detailWallpaperDate;
    private TextView detailWallpaperDescription;
    private ChipGroup detailTagChipGroup;
    private FlexboxLayout detailColorPaletteContainer;
    private MaterialButton btnSetWallpaper;
    private MaterialButton btnDownloadWallpaper;
    private MaterialToolbar toolbar;
    private LinearLayout bottomSheet;
    private Integer pendingWallpaperFlag = null;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private int dominantColor = -1;
    private int primaryColor = -1;
    private int onPrimaryColor = -1;
    private int surfaceColor = -1;
    private int onSurfaceColor = -1;
    private int surfaceContainerHighColor = -1;
    private int colorTertiaryContainer = -1;
    private int colorPrimaryContainer = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_detail);

        initializeViews();
        setupInitialTheme();

        currentWallpaper = (Wallpaper) getIntent().getSerializableExtra("wallpaper");

        if (currentWallpaper != null) {
            displayWallpaperDetails();
            setupClickListeners();
        } else {
            Toast.makeText(this, "Wallpaper details not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        detailWallpaperImage = findViewById(R.id.detail_wallpaper_image);
        detailWallpaperName = findViewById(R.id.detail_wallpaper_name);
        detailWallpaperUsername = findViewById(R.id.detail_wallpaper_username);
        detailWallpaperDate = findViewById(R.id.detail_wallpaper_date);
        detailWallpaperDescription = findViewById(R.id.detail_wallpaper_description);
        detailTagChipGroup = findViewById(R.id.detail_tag_chip_group);
        detailColorPaletteContainer = findViewById(R.id.detail_color_palette_container);
        btnSetWallpaper = findViewById(R.id.btn_set_wallpaper);
        btnDownloadWallpaper = findViewById(R.id.btn_download_wallpaper);
        bottomSheet = findViewById(R.id.bottom_sheet);
    }

    private void setupInitialTheme() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimarySurface, typedValue, true);
        primaryColor = typedValue.data;

        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
        onPrimaryColor = typedValue.data;

        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        surfaceColor = typedValue.data;

        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        onSurfaceColor = typedValue.data;

        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true);
        surfaceContainerHighColor = typedValue.data;
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorTertiaryContainer, typedValue, true);
        colorTertiaryContainer = typedValue.data;
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
        colorPrimaryContainer = typedValue.data;

        setInitialBottomSheetBackground();
    }

    private void setInitialBottomSheetBackground() {
        if (bottomSheet != null) {
            GradientDrawable initialBackground = new GradientDrawable();
            initialBackground.setShape(GradientDrawable.RECTANGLE);
            initialBackground.setColor(surfaceColor);
            initialBackground.setCornerRadii(new float[]{
                    dpToPx(16), dpToPx(16),
                    dpToPx(16), dpToPx(16),
                    0, 0,
                    0, 0
            });
            bottomSheet.setBackground(initialBackground);
        }
    }

    private void displayWallpaperDetails() {
        btnSetWallpaper.setEnabled(false);
        btnDownloadWallpaper.setEnabled(false);

        Glide.with(this)
                .asBitmap()
                .load(currentWallpaper.getImageUrl())
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Toast.makeText(WallpaperDetailActivity.this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        btnSetWallpaper.setEnabled(true);
                        btnDownloadWallpaper.setEnabled(true);
                        extractDominantColorAndApplyTheme(resource);
                        return false;
                    }
                })
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(detailWallpaperImage);

        detailWallpaperName.setText(currentWallpaper.getName());
        detailWallpaperUsername.setText(currentWallpaper.getFormattedUsername());
        detailWallpaperDescription.setText(currentWallpaper.getDescription());
        detailWallpaperDate.setText(formatDate(currentWallpaper.getCreatedAt()));

        setupTags();
        setupColorPalette();
    }

    private void extractDominantColorAndApplyTheme(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, false);

        Palette.from(scaledBitmap).generate(palette -> {
            if (palette != null) {
                Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
                Palette.Swatch dominantSwatch = palette.getDominantSwatch();
                Palette.Swatch mutedSwatch = palette.getMutedSwatch();

                Palette.Swatch selectedSwatch = vibrantSwatch != null ? vibrantSwatch :
                        dominantSwatch != null ? dominantSwatch : mutedSwatch;

                if (selectedSwatch != null) {
                    dominantColor = selectedSwatch.getRgb();
                    applyDynamicTheme(dominantColor);
                }
            }
        });
    }

    private boolean isNightModeActive() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyDynamicTheme(int dominantColor) {
        boolean isDarkMode = isNightModeActive();

        if (isDarkMode) {
            primaryColor = ColorUtils.blendARGB(dominantColor, Color.WHITE, 0.3f);
            onPrimaryColor = getContrastColor(primaryColor);
            surfaceColor = ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.85f);
            onSurfaceColor = Color.WHITE;
            surfaceContainerHighColor = ColorUtils.blendARGB(surfaceColor, Color.WHITE, 0.05f);
            colorTertiaryContainer = ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.5f);
            colorPrimaryContainer = ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.3f);
        } else {
            primaryColor = dominantColor;
            onPrimaryColor = getContrastColor(dominantColor);
            surfaceColor = ColorUtils.blendARGB(dominantColor, Color.WHITE, 0.92f);
            onSurfaceColor = getContrastColor(surfaceColor);
            surfaceContainerHighColor = ColorUtils.blendARGB(surfaceColor, Color.BLACK, 0.03f);
            colorTertiaryContainer = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.6f);
            colorPrimaryContainer = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.4f);
        }

        runOnUiThread(() -> {
            applyColorsToViews();
            updateStatusBarColor();
        });
    }

    private void applyColorsToViews() {
        btnSetWallpaper.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        btnSetWallpaper.setTextColor(onPrimaryColor);

        btnDownloadWallpaper.setStrokeColor(ColorStateList.valueOf(primaryColor));
        btnDownloadWallpaper.setTextColor(primaryColor);

        if (bottomSheet != null) {
            GradientDrawable bottomSheetBackground = new GradientDrawable();
            bottomSheetBackground.setShape(GradientDrawable.RECTANGLE);
            bottomSheetBackground.setColor(surfaceColor);
            bottomSheetBackground.setCornerRadii(new float[]{
                    dpToPx(16), dpToPx(16),
                    dpToPx(16), dpToPx(16),
                    0, 0,
                    0, 0
            });
            bottomSheet.setBackground(bottomSheetBackground);
        }

        if (toolbar != null) {
            toolbar.setNavigationIconTint(Color.WHITE);
        }

        updateTextColors();
        setupTags();
        setupColorPalette();
    }

    private void updateTextColors() {
        int secondaryTextColor = ColorUtils.blendARGB(onSurfaceColor, surfaceColor, 0.3f);

        detailWallpaperName.setTextColor(onSurfaceColor);
        detailWallpaperDescription.setTextColor(onSurfaceColor);
        detailWallpaperUsername.setTextColor(secondaryTextColor);
        detailWallpaperDate.setTextColor(secondaryTextColor);
    }

    private void updateStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            int statusBarColor = ColorUtils.setAlphaComponent(surfaceColor, 0x50);
            window.setStatusBarColor(statusBarColor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                if (isNightModeActive()) {
                    decor.setSystemUiVisibility(0);
                } else {
                    boolean isLightColor = ColorUtils.calculateLuminance(statusBarColor) > 0.5;
                    if (isLightColor) {
                        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    } else {
                        decor.setSystemUiVisibility(0);
                    }
                }
            }
        }
    }

    private void setupTags() {
        detailTagChipGroup.removeAllViews();
        if (currentWallpaper.getTags() != null) {
            for (String tag : currentWallpaper.getTags()) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setFocusable(false);

                if (dominantColor != -1) {
                    int chipBackgroundColor;
                    if (isNightModeActive()) {
                        chipBackgroundColor = ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.7f);
                    } else {
                        chipBackgroundColor = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.8f);
                    }
                    int chipTextColor = getContrastColor(chipBackgroundColor);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(chipBackgroundColor));
                    chip.setTextColor(chipTextColor);
                }
                detailTagChipGroup.addView(chip);
            }
        }
    }

    private void setupColorPalette() {
        detailColorPaletteContainer.removeAllViews();
        if (currentWallpaper.getColorPalette() != null && !currentWallpaper.getColorPalette().isEmpty()) {
            for (String hexColor : currentWallpaper.getColorPalette()) {
                createColorPaletteChip(hexColor);
            }
        } else {
            TextView noColorsText = new TextView(this);
            noColorsText.setText("No color palette available.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                noColorsText.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
            } else {
                noColorsText.setTextAppearance(this, android.R.style.TextAppearance_Material_Body1);
            }
            noColorsText.setTextColor(onSurfaceColor);
            detailColorPaletteContainer.addView(noColorsText);
        }
    }

    private void createColorPaletteChip(String hexColor) {
        TextView colorChip = new TextView(this);
        colorChip.setText(hexColor.toUpperCase());
        colorChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        colorChip.setTextColor(getContrastColor(hexColor));
        colorChip.setGravity(Gravity.CENTER);
        colorChip.setPadding((int) dpToPx(12), (int) dpToPx(8), (int) dpToPx(12), (int) dpToPx(8));
        colorChip.setSingleLine(true);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dpToPx(6));
        try {
            background.setColor(Color.parseColor(hexColor));
        } catch (IllegalArgumentException e) {
            background.setColor(Color.GRAY);
        }

        int strokeColor = dominantColor != -1 ?
                ColorUtils.blendARGB(primaryColor, onSurfaceColor, 0.5f) :
                ContextCompat.getColor(this, android.R.color.darker_gray);
        background.setStroke((int) dpToPx(1), strokeColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            android.graphics.drawable.Drawable ripple = ContextCompat.getDrawable(this, outValue.resourceId);
            android.graphics.drawable.LayerDrawable layerDrawable = new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{background, ripple}
            );
            colorChip.setBackground(layerDrawable);
        } else {
            colorChip.setBackground(background);
        }

        colorChip.setClickable(true);
        colorChip.setFocusable(true);

        ViewGroup.LayoutParams chipParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        colorChip.setLayoutParams(chipParams);

        ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(chipParams);
        marginParams.setMargins((int) dpToPx(4), (int) dpToPx(2), (int) dpToPx(4), (int) dpToPx(2));
        colorChip.setLayoutParams(marginParams);

        colorChip.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Color Code", hexColor);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, hexColor + " copied!", Toast.LENGTH_SHORT).show();
        });

        detailColorPaletteContainer.addView(colorChip);
    }

    private int getContrastColor(String hexColor) {
        try {
            int color = Color.parseColor(hexColor);
            return getContrastColor(color);
        } catch (IllegalArgumentException e) {
            return Color.WHITE;
        }
    }

    private int getContrastColor(int color) {
        double luminance = ColorUtils.calculateLuminance(color);
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private void setupClickListeners() {
        btnSetWallpaper.setOnClickListener(v -> {
            WallpaperOptionBottomSheet sheet = new WallpaperOptionBottomSheet();
            sheet.setDynamicColors(primaryColor, onPrimaryColor, surfaceColor, onSurfaceColor,
                    surfaceContainerHighColor, colorTertiaryContainer, colorPrimaryContainer);
            sheet.setWallpaperOptionListener(flag -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER) != PackageManager.PERMISSION_GRANTED) {
                    pendingWallpaperFlag = flag;
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.SET_WALLPAPER},
                            PERMISSION_REQUEST_CODE_WALLPAPER);
                } else {
                    setWallpaper(flag);
                }
            });
            sheet.show(getSupportFragmentManager(), "WallpaperOptionSheet");
        });
        btnDownloadWallpaper.setOnClickListener(v -> downloadWallpaper());
    }

    private String formatDate(String isoDateString) {
        try {
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            };

            formats[0].setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            formats[1].setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            Date date = null;
            for (SimpleDateFormat format : formats) {
                try {
                    date = format.parse(isoDateString);
                    break;
                } catch (ParseException ignored) {
                }
            }
            if (date != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return "Uploaded on " + displayFormat.format(date);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error parsing date: " + isoDateString, e);
        }
        return "Unknown date";
    }

    private void setWallpaper(int flag) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SET_WALLPAPER}, PERMISSION_REQUEST_CODE_WALLPAPER);
            return;
        }

        btnSetWallpaper.setEnabled(false);
        btnDownloadWallpaper.setEnabled(false);

        Glide.with(this)
                .asBitmap()
                .load(currentWallpaper.getImageUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @androidx.annotation.Nullable Transition<? super Bitmap> transition) {
                        executorService.execute(() -> {
                            try {
                                WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    wallpaperManager.setBitmap(resource, null, true, flag);
                                } else {
                                    wallpaperManager.setBitmap(resource);
                                }
                                runOnUiThread(() -> Toast.makeText(WallpaperDetailActivity.this, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show());
                            } catch (IOException e) {
                                runOnUiThread(() -> Toast.makeText(WallpaperDetailActivity.this, "Error setting wallpaper: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            } finally {
                                runOnUiThread(() -> {
                                    btnSetWallpaper.setEnabled(true);
                                    btnDownloadWallpaper.setEnabled(true);
                                });
                            }
                        });
                    }

                    @Override
                    public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }

    private void downloadWallpaper() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_STORAGE);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE_STORAGE);
            return;
        }

        btnSetWallpaper.setEnabled(false);
        btnDownloadWallpaper.setEnabled(false);

        Glide.with(this)
                .asBitmap()
                .load(currentWallpaper.getImageUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(@androidx.annotation.Nullable android.graphics.drawable.Drawable errorDrawable) {
                        runOnUiThread(() -> {
                            Toast.makeText(WallpaperDetailActivity.this, "Failed to load image for download.", Toast.LENGTH_SHORT).show();
                            btnSetWallpaper.setEnabled(true);
                            btnDownloadWallpaper.setEnabled(true);
                        });
                    }

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @androidx.annotation.Nullable Transition<? super Bitmap> transition) {
                        executorService.execute(() -> {
                            boolean saved = saveBitmapToGallery(resource, currentWallpaper.getName());
                            runOnUiThread(() -> {
                                if (saved) {
                                    Toast.makeText(WallpaperDetailActivity.this, "Wallpaper downloaded to gallery!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(WallpaperDetailActivity.this, "Failed to download wallpaper.", Toast.LENGTH_SHORT).show();
                                }
                                btnSetWallpaper.setEnabled(true);
                                btnDownloadWallpaper.setEnabled(true);
                            });
                        });
                    }

                    @Override
                    public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }

    private boolean saveBitmapToGallery(Bitmap bitmap, String name) {
        String filename = name.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "RelWallpaper");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        } else {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "RelWallpaper");
            if (!directory.exists() && !directory.mkdirs()) {
                return false;
            }
            values.put(MediaStore.Images.Media.DATA, new File(directory, filename).getAbsolutePath());
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return false;
        }

        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) return false;
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

}
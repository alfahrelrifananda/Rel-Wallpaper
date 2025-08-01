package com.example.relwallpaper.ui.pages;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.relwallpaper.BuildConfig;
import com.example.relwallpaper.R;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import android.util.Log;

public class AddWallpaperActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_API_KEY = BuildConfig.SUPABASE_API_KEY;
    private static final String TAG = "AddWallpaperActivity";
    private TextInputEditText nameEditText, descriptionEditText;
    private ChipGroup tagChipGroup;
    private MaterialButton uploadButton;
    private LinearProgressIndicator progressIndicator;
    private FloatingActionButton backButton;
    private ImageView imagePreview;
    private LinearLayout imagePlaceholder;
    private LinearLayout imageInfoOverlay;
    private MaterialCardView heroSection;
    private TextView imageNameText;
    private TextView imageSizeText;
    private TextView imageFileSizeText;
    private Uri selectedImageUri;
    private OkHttpClient httpClient;
    private String accessToken;
    private String currentUserId;
    private String selectedFileName;
    private String selectedFileFormat;
    private long selectedFileSize;
    private int selectedImageWidth;
    private int selectedImageHeight;
    private List<String> extractedColorPalette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallpaper);

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
        setupClickListeners();
        setupPredefinedTags();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.wallpaper_name_input);
        descriptionEditText = findViewById(R.id.wallpaper_description_input);
        tagChipGroup = findViewById(R.id.tag_chip_group);
        uploadButton = findViewById(R.id.upload_button);
        progressIndicator = findViewById(R.id.progress_indicator);
        backButton = findViewById(R.id.back_button);
        imagePreview = findViewById(R.id.image_preview);
        imagePlaceholder = findViewById(R.id.image_placeholder);
        imageInfoOverlay = findViewById(R.id.image_info_overlay);
        heroSection = findViewById(R.id.hero_section);
        imageNameText = findViewById(R.id.image_name_text);
        imageSizeText = findViewById(R.id.image_size_text);
        imageFileSizeText = findViewById(R.id.image_file_size_text);
    }

    private void setupClickListeners() {
        heroSection.setOnClickListener(v -> {
            if (checkPermissions()) {
                openImagePicker();
            } else {
                requestPermissions();
            }
        });
        uploadButton.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadWallpaper();
            }
        });
        backButton.setOnClickListener(v -> finish());
    }

    private void setupPredefinedTags() {
        String[] predefinedTags = {
                "For Fun", "Doodle", "Nature", "Shape", "Retro",
                "Branding", "Motif", "Blob", "Gradients", "Elements", "Art & Culture", "Blurred"
        };
        for (String tag : predefinedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            tagChipGroup.addView(chip);
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                            == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                processSelectedImage();
            }
        }
    }

    private void processSelectedImage() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedImageWidth = bitmap.getWidth();
            selectedImageHeight = bitmap.getHeight();
            selectedFileName = getFileName(selectedImageUri);
            selectedFileFormat = getFileFormat(selectedFileName);
            selectedFileSize = getFileSize(selectedImageUri);
            extractedColorPalette = extractColorPalette(bitmap);
            updateImagePreview();
            uploadButton.setEnabled(true);
        } catch (Exception e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Extract 6 dominant colors from the image using k-means clustering algorithm
     * @param bitmap The image bitmap
     * @return List of hex color codes
     */
    private List<String> extractColorPalette(Bitmap bitmap) {
        int maxSize = 100;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxSize || height > maxSize) {
            float scale = Math.min((float) maxSize / width, (float) maxSize / height);
            width = (int) (width * scale);
            height = (int) (height * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        List<ColorData> pixels = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                pixels.add(new ColorData(
                        Color.red(pixel),
                        Color.green(pixel),
                        Color.blue(pixel)
                ));
            }
        }
        List<ColorData> dominantColors = performKMeansClustering(pixels, 6);
        List<String> hexColors = new ArrayList<>();
        for (ColorData color : dominantColors) {
            String hex = String.format("#%02X%02X%02X", color.r, color.g, color.b);
            hexColors.add(hex);
        }
        return hexColors;
    }

    private List<ColorData> performKMeansClustering(List<ColorData> pixels, int k) {
        List<ColorData> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            ColorData randomPixel = pixels.get((int) (Math.random() * pixels.size()));
            centroids.add(new ColorData(randomPixel.r, randomPixel.g, randomPixel.b));
        }
        for (int iteration = 0; iteration < 10; iteration++) {
            List<List<ColorData>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                clusters.add(new ArrayList<>());
            }
            for (ColorData pixel : pixels) {
                int nearestCentroid = 0;
                double minDistance = Double.MAX_VALUE;
                for (int i = 0; i < centroids.size(); i++) {
                    double distance = calculateColorDistance(pixel, centroids.get(i));
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCentroid = i;
                    }
                }
                clusters.get(nearestCentroid).add(pixel);
            }
            for (int i = 0; i < k; i++) {
                if (!clusters.get(i).isEmpty()) {
                    int avgR = 0, avgG = 0, avgB = 0;
                    for (ColorData pixel : clusters.get(i)) {
                        avgR += pixel.r;
                        avgG += pixel.g;
                        avgB += pixel.b;
                    }
                    avgR /= clusters.get(i).size();
                    avgG /= clusters.get(i).size();
                    avgB /= clusters.get(i).size();
                    centroids.set(i, new ColorData(avgR, avgG, avgB));
                }
            }
        }
        List<List<ColorData>> finalClusters = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            finalClusters.add(new ArrayList<>());
        }
        for (ColorData pixel : pixels) {
            int nearestCentroid = 0;
            double minDistance = Double.MAX_VALUE;
            for (int i = 0; i < centroids.size(); i++) {
                double distance = calculateColorDistance(pixel, centroids.get(i));
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCentroid = i;
                }
            }
            finalClusters.get(nearestCentroid).add(pixel);
        }
        Collections.sort(centroids, new Comparator<ColorData>() {
            @Override
            public int compare(ColorData c1, ColorData c2) {
                int size1 = 0, size2 = 0;
                for (int i = 0; i < centroids.size(); i++) {
                    if (centroids.get(i) == c1) size1 = finalClusters.get(i).size();
                    if (centroids.get(i) == c2) size2 = finalClusters.get(i).size();
                }
                return Integer.compare(size2, size1);
            }
        });
        return centroids;
    }

    private double calculateColorDistance(ColorData c1, ColorData c2) {
        int dr = c1.r - c2.r;
        int dg = c1.g - c2.g;
        int db = c1.b - c2.b;
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    private static class ColorData {
        int r, g, b;
        ColorData(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private void updateImagePreview() {
        imagePlaceholder.setVisibility(View.GONE);
        imagePreview.setVisibility(View.VISIBLE);
        imageInfoOverlay.setVisibility(View.VISIBLE);
        imagePreview.setImageURI(selectedImageUri);
        imageNameText.setText(selectedFileName);
        imageSizeText.setText(selectedImageWidth + " × " + selectedImageHeight);
        imageFileSizeText.setText(formatFileSize(selectedFileSize));
    }

    private String getFileName(Uri uri) {
        String fileName = "unknown";
        try {
            String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
            android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                fileName = cursor.getString(columnIndex);
                cursor.close();
            }
        } catch (Exception e) {
            fileName = "image_" + System.currentTimeMillis();
        }
        return fileName;
    }

    private String getFileFormat(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "jpeg";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return "png";
        } else if (fileName.toLowerCase().endsWith(".webp")) {
            return "webp";
        } else {
            return "jpeg";
        }
    }

    private long getFileSize(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                long size = inputStream.available();
                inputStream.close();
                return size;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return new DecimalFormat("#.#").format(sizeInBytes / 1024.0) + " KB";
        } else {
            return new DecimalFormat("#.#").format(sizeInBytes / (1024.0 * 1024.0)) + " MB";
        }
    }

    private boolean validateInputs() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            return false;
        }
        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return false;
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (tagChipGroup.getCheckedChipId() == View.NO_ID) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadWallpaper() {
        setLoadingState(true);
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String fileName = "wallpaper_" + UUID.randomUUID().toString() + ".jpg";
            uploadToSupabaseStorage(imageBytes, fileName);
        } catch (IOException e) {
            showError("Failed to process image: " + e.getMessage());
            setLoadingState(false);
        }
    }

    private void uploadToSupabaseStorage(byte[] imageBytes, String fileName) {
        String storageUrl = SUPABASE_URL + "/storage/v1/object/wallpapers/" + fileName + "?upsert=true";
        RequestBody requestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
        Request request = new Request.Builder()
                .url(storageUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Content-Type", "image/jpeg")
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showError("Upload failed: " + e.getMessage()));
                setLoadingState(false);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> showError("Upload failed: HTTP " + response.code()));
                    setLoadingState(false);
                    return;
                }
                String imageUrl = SUPABASE_URL + "/storage/v1/object/public/wallpapers/" + fileName;
                saveWallpaperMetadata(imageUrl, fileName);
            }
        });
    }

    private void saveWallpaperMetadata(String imageUrl, String fileName) {
        SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
        String createdAt = iso8601.format(new Date());
        try {
            JSONObject metadata = new JSONObject();
            metadata.put("name", nameEditText.getText().toString().trim());
            metadata.put("description", descriptionEditText.getText().toString().trim());
            metadata.put("image_url", imageUrl);
            metadata.put("file_name", fileName);
            metadata.put("tags", new JSONArray(getSelectedTags()));
            metadata.put("created_at", createdAt);
            metadata.put("user_id", currentUserId);

            if (extractedColorPalette != null && !extractedColorPalette.isEmpty()) {
                JSONArray colorPaletteArray = new JSONArray();
                for (String hexColor : extractedColorPalette) {
                    colorPaletteArray.put(hexColor);
                }
                metadata.put("color_palette", colorPaletteArray);
            }
            String metadataUrl = SUPABASE_URL + "/rest/v1/wallpapers";


            RequestBody requestBody = RequestBody.create(
                    metadata.toString(),
                    MediaType.parse("application/json")
            );
            Request request = new Request.Builder()
                    .url(metadataUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to save metadata: " + e.getMessage(), e);
                    runOnUiThread(() -> showError("Failed to save metadata: " + e.getMessage()));
                    setLoadingState(false);
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(AddWallpaperActivity.this, "Wallpaper uploaded successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            showError("Failed to save metadata: " + response.code() + "\n" + responseBody);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating metadata JSON: " + e.getMessage(), e);
            showError("Error creating metadata: " + e.getMessage());
            setLoadingState(false);
        }
    }

    private List<String> getSelectedTags() {
        List<String> selectedTags = new ArrayList<>();
        int checkedChipId = tagChipGroup.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip selectedChip = findViewById(checkedChipId);
            if (selectedChip != null) {
                selectedTags.add(selectedChip.getText().toString());
            }
        }
        return selectedTags;
    }

    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            uploadButton.setEnabled(!isLoading && selectedImageUri != null);
            heroSection.setEnabled(!isLoading);
            uploadButton.setText(isLoading ? "Uploading..." : "Upload Wallpaper");
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(AddWallpaperActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return true;
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            long exp = json.getLong("exp");
            long now = System.currentTimeMillis() / 1000;
            return exp < now;
        } catch (Exception e) {
            return true;
        }
    }

    private String getUserIdFromToken(String token) {
        try {
            if (token == null) return null;
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            return json.optString("sub", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        httpClient.dispatcher().executorService().shutdown();
    }
}

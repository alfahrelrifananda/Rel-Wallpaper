package com.example.relwallpaper.ui.home.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.relwallpaper.R;
import com.example.relwallpaper.ui.home.model.Wallpaper;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.WallpaperViewHolder> {

    private List<Wallpaper> wallpapers;
    private OnWallpaperClickListener listener;
    private Context context;

    private final int[] itemHeights = {300, 350, 400, 450, 500, 380, 420};

    public interface OnWallpaperClickListener {
        void onWallpaperClick(Wallpaper wallpaper, int position);
        void onWallpaperLongClick(Wallpaper wallpaper, int position);
    }

    public WallpaperAdapter(List<Wallpaper> wallpapers, OnWallpaperClickListener listener) {
        this.wallpapers = wallpapers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WallpaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallpaper_card, parent, false);
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
        private MaterialCardView cardView;
        private ImageView wallpaperImage;
        private View overlayGradient;
        private TextView wallpaperName;
        private TextView wallpaperUsername;
        private View colorIndicator;

        public WallpaperViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.wallpaper_card);
            wallpaperImage = itemView.findViewById(R.id.wallpaper_image);
            overlayGradient = itemView.findViewById(R.id.overlay_gradient);
            wallpaperName = itemView.findViewById(R.id.wallpaper_name);
            wallpaperUsername = itemView.findViewById(R.id.wallpaper_username);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(Wallpaper wallpaper, int position) {
            ViewGroup.LayoutParams layoutParams = cardView.getLayoutParams();
            layoutParams.height = dpToPx(itemHeights[position % itemHeights.length]);
            cardView.setLayoutParams(layoutParams);

            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.error_image)
                    .transform(new CenterCrop(), new RoundedCorners(dpToPx(12)))
                    .override(400, 600)
                    .skipMemoryCache(false);

            Glide.with(context)
                    .load(wallpaper.getImageUrl())
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(wallpaperImage);

            wallpaperName.setText(wallpaper.getName());
            wallpaperUsername.setText(wallpaper.getFormattedUsername());

            if (wallpaper.getColorPalette() != null && !wallpaper.getColorPalette().isEmpty()) {
                setColorIndicator(wallpaper.getColorPalette());
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWallpaperClick(wallpaper, position);
                }
            });

            cardView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onWallpaperLongClick(wallpaper, position);
                }
                return true;
            });

            cardView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        cardView.animate()
                                .scaleX(0.95f)
                                .scaleY(0.95f)
                                .setDuration(100)
                                .withLayer();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        cardView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .withLayer();
                        break;
                }
                return false;
            });
        }

        private void setColorIndicator(List<String> colorPalette) {
            if (colorIndicator == null) return;

            int[] colors = new int[Math.min(colorPalette.size(), 3)];

            for (int i = 0; i < colors.length; i++) {
                try {
                    colors[i] = Color.parseColor(colorPalette.get(i));
                } catch (Exception e) {
                    colors[i] = context.getColor(com.google.android.material.R.color.material_dynamic_primary20);
                }
            }

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT, colors);
            gradient.setCornerRadius(dpToPx(8));

            colorIndicator.setBackground(gradient);
            colorIndicator.setVisibility(View.VISIBLE);
        }

        private int getContrastColor(int color) {
            double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return luminance > 0.5 ? Color.BLACK : Color.WHITE;
        }

        private int dpToPx(int dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
    }

    public void clearImageCache() {
        if (context != null) {
            Glide.get(context).clearMemory();
            new Thread(() -> Glide.get(context).clearDiskCache()).start();
        }
    }

    public void preloadImages(int startPosition, int count) {
        if (context == null) return;

        int endPosition = Math.min(startPosition + count, wallpapers.size());
        for (int i = startPosition; i < endPosition; i++) {
            Wallpaper wallpaper = wallpapers.get(i);
            if (wallpaper != null && wallpaper.getImageUrl() != null) {
                Glide.with(context)
                        .load(wallpaper.getImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload(400, 600);
            }
        }
    }
}
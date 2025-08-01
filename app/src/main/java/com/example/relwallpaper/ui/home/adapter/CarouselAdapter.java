package com.example.relwallpaper.ui.home.adapter;

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
import com.google.android.material.carousel.CarouselLayoutManager;
import com.google.android.material.carousel.CarouselSnapHelper;
import com.google.android.material.carousel.MultiBrowseCarouselStrategy;
import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private List<Wallpaper> wallpapers;
    private OnCarouselItemClickListener listener;

    public interface OnCarouselItemClickListener {
        void onCarouselItemClick(Wallpaper wallpaper, int position);
    }

    public CarouselAdapter(List<Wallpaper> wallpapers, OnCarouselItemClickListener listener) {
        this.wallpapers = wallpapers;
        this.listener = listener;
    }

    public static void setupCarousel(RecyclerView recyclerView) {
        CarouselLayoutManager layoutManager = new CarouselLayoutManager();

        layoutManager.setCarouselStrategy(new MultiBrowseCarouselStrategy());

        recyclerView.setLayoutManager(layoutManager);

        CarouselSnapHelper snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.setNestedScrollingEnabled(false);
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel_card, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        if (wallpapers != null && position < wallpapers.size()) {
            Wallpaper wallpaper = wallpapers.get(position);
            holder.bind(wallpaper, position);
        }
    }

    @Override
    public int getItemCount() {
        return wallpapers != null ? wallpapers.size() : 0;
    }

    class CarouselViewHolder extends RecyclerView.ViewHolder {
        private com.google.android.material.carousel.MaskableFrameLayout container;
        private MaterialCardView cardView;
        private ImageView imageView;
        private TextView titleText;
        private TextView subtitleText;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            container = (com.google.android.material.carousel.MaskableFrameLayout) itemView.findViewById(R.id.carousel_item_container);
            cardView = itemView.findViewById(R.id.carousel_card_view);
            imageView = itemView.findViewById(R.id.carousel_image_view);
            titleText = itemView.findViewById(R.id.carousel_title);
            subtitleText = itemView.findViewById(R.id.carousel_subtitle);
        }

        public void bind(Wallpaper wallpaper, int position) {
            if (imageView == null || wallpaper == null) {
                return;
            }

            String imageUrl = wallpaper.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                RequestOptions requestOptions = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.drawable.error_image)
                        .transform(new CenterCrop(), new RoundedCorners(dpToPx(28)))
                        .priority(com.bumptech.glide.Priority.HIGH)
                        .skipMemoryCache(false);

                Glide.with(imageView.getContext())
                        .load(imageUrl)
                        .apply(requestOptions)
                        .transition(DrawableTransitionOptions.withCrossFade(200))
                        .into(imageView);
            }

            titleText.setText(wallpaper.getName() != null ? wallpaper.getName() : "Wallpaper");

            if (position == 0) {
                subtitleText.setText("Featured");
            } else if (position < 3) {
                subtitleText.setText("Popular");
            } else {
                subtitleText.setText("Latest");
            }

            container.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCarouselItemClick(wallpaper, position);
                }
            });

            container.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        container.animate()
                                .scaleX(0.98f)
                                .scaleY(0.98f)
                                .setDuration(150)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        container.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                        break;
                }
                return false;
            });
        }

        private int dpToPx(int dp) {
            return (int) (dp * imageView.getContext().getResources().getDisplayMetrics().density);
        }
    }

    public void updateWallpapers(List<Wallpaper> newWallpapers) {
        this.wallpapers = newWallpapers;
        notifyDataSetChanged();
    }
}
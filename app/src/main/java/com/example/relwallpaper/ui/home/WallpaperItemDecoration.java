package com.example.relwallpaper.ui.home;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class WallpaperItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;

    public WallpaperItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

        StaggeredGridLayoutManager.LayoutParams layoutParams =
                (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();

        int spanIndex = layoutParams.getSpanIndex();
        int position = parent.getChildAdapterPosition(view);

        outRect.top = spacing / 2;
        outRect.bottom = spacing / 2;

        if (spanIndex == 0) {
            outRect.left = spacing;
            outRect.right = spacing / 2;
        } else {
            outRect.left = spacing / 2;
            outRect.right = spacing;
        }

        if (position < 2) {
            outRect.top = spacing;
        }
    }
}

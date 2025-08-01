package com.example.relwallpaper.ui.component;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.example.relwallpaper.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

public class WallpaperOptionBottomSheet extends BottomSheetDialogFragment {

    public interface WallpaperOptionListener {
        void onOptionSelected(int flag);
    }

    private WallpaperOptionListener listener;
    private MaterialRadioButton homeRadio, lockRadio, bothRadio;
    private MaterialCardView homeCard, lockCard, bothCard;
    private MaterialButton setWallpaperButton;

    private TextView homeTitle, homeDescription;
    private TextView lockTitle, lockDescription;
    private TextView bothTitle, bothDescription;

    private MaterialCardView homeIconCard, lockIconCard, bothIconCard;
    private ImageView homeIcon, lockIcon, bothIcon;

    private int selectedFlag = android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK;

    private int primaryColor = -1;
    private int onPrimaryColor = -1;
    private int surfaceColor = -1;
    private int onSurfaceColor = -1;
    private int surfaceContainerHighColor = -1;
    private int colorTertiaryContainer = -1;
    private int colorPrimaryContainer = -1;

    public void setDynamicColors(int primary, int onPrimary, int surface, int onSurface,
                                 int surfaceContainerHigh, int tertiaryContainer, int primaryContainer) {
        this.primaryColor = primary;
        this.onPrimaryColor = onPrimary;
        this.surfaceColor = surface;
        this.onSurfaceColor = onSurface;
        this.surfaceContainerHighColor = surfaceContainerHigh;
        this.colorTertiaryContainer = tertiaryContainer;
        this.colorPrimaryContainer = primaryContainer;
    }

    public void setWallpaperOptionListener(WallpaperOptionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_wallpaper_option, container, false);

        homeRadio = view.findViewById(R.id.home_radio);
        lockRadio = view.findViewById(R.id.lock_radio);
        bothRadio = view.findViewById(R.id.both_radio);

        homeCard = view.findViewById(R.id.home_screen_card);
        lockCard = view.findViewById(R.id.lock_screen_card);
        bothCard = view.findViewById(R.id.both_screens_card);

        setWallpaperButton = view.findViewById(R.id.set_wallpaper_button);

        homeTitle = view.findViewById(R.id.home_title);
        homeDescription = view.findViewById(R.id.home_description);
        lockTitle = view.findViewById(R.id.lock_title);
        lockDescription = view.findViewById(R.id.lock_description);
        bothTitle = view.findViewById(R.id.both_title);
        bothDescription = view.findViewById(R.id.both_description);

        homeIconCard = view.findViewById(R.id.home_icon_card);
        lockIconCard = view.findViewById(R.id.lock_icon_card);
        bothIconCard = view.findViewById(R.id.both_icon_card);
        homeIcon = view.findViewById(R.id.home_icon);
        lockIcon = view.findViewById(R.id.lock_icon);
        bothIcon = view.findViewById(R.id.both_icon);

        if (primaryColor != -1) {
            applyDynamicColorsToViews(view);
        } else {
            setupInitialThemeColors();
            applyDynamicColorsToViews(view);
        }

        setupRadioGroup();
        setupCardClickListeners();
        setupSetWallpaperButton();

        return view;
    }

    private void setupInitialThemeColors() {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null) {
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
            primaryColor = typedValue.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true);
            onPrimaryColor = typedValue.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
            surfaceColor = typedValue.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
            onSurfaceColor = typedValue.data;

            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true);
            surfaceContainerHighColor = typedValue.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorTertiaryContainer, typedValue, true);
            colorTertiaryContainer = typedValue.data;
            getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
            colorPrimaryContainer = typedValue.data;
        }
    }

    private void applyDynamicColorsToViews(View view) {
        view.setBackground(createBottomSheetBackground(surfaceColor));

        setWallpaperButton.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        setWallpaperButton.setTextColor(onPrimaryColor);
        setWallpaperButton.setStrokeColor(ColorStateList.valueOf(primaryColor));

        ColorStateList radioTint = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{
                        primaryColor,
                        ColorUtils.setAlphaComponent(onSurfaceColor, 150)
                }
        );

        homeRadio.setButtonTintList(radioTint);
        lockRadio.setButtonTintList(radioTint);
        bothRadio.setButtonTintList(radioTint);

        int cardBackgroundColor = surfaceContainerHighColor != -1 ? surfaceContainerHighColor :
                ColorUtils.blendARGB(surfaceColor, Color.WHITE, 0.1f);
        int rippleColor = ColorUtils.setAlphaComponent(primaryColor, (int) (255 * 0.2f));

        homeCard.setCardBackgroundColor(cardBackgroundColor);
        lockCard.setCardBackgroundColor(cardBackgroundColor);
        bothCard.setCardBackgroundColor(cardBackgroundColor);

        homeCard.setRippleColor(ColorStateList.valueOf(rippleColor));
        lockCard.setRippleColor(ColorStateList.valueOf(rippleColor));
        bothCard.setRippleColor(ColorStateList.valueOf(rippleColor));

        int primaryTextColor = onSurfaceColor;
        int secondaryTextColor = ColorUtils.blendARGB(onSurfaceColor, surfaceColor, 0.4f);

        if (homeTitle != null) homeTitle.setTextColor(primaryTextColor);
        if (homeDescription != null) homeDescription.setTextColor(secondaryTextColor);
        if (lockTitle != null) lockTitle.setTextColor(primaryTextColor);
        if (lockDescription != null) lockDescription.setTextColor(secondaryTextColor);
        if (bothTitle != null) bothTitle.setTextColor(primaryTextColor);
        if (bothDescription != null) bothDescription.setTextColor(secondaryTextColor);

        if (homeIconCard != null) homeIconCard.setCardBackgroundColor(colorTertiaryContainer != -1 ? colorTertiaryContainer : ColorUtils.blendARGB(surfaceColor, primaryColor, 0.1f));
        if (lockIconCard != null) lockIconCard.setCardBackgroundColor(colorTertiaryContainer != -1 ? colorTertiaryContainer : ColorUtils.blendARGB(surfaceColor, primaryColor, 0.1f));
        if (bothIconCard != null) bothIconCard.setCardBackgroundColor(colorPrimaryContainer != -1 ? colorPrimaryContainer : ColorUtils.blendARGB(surfaceColor, primaryColor, 0.2f));

        if (homeIcon != null) homeIcon.setImageTintList(ColorStateList.valueOf(onSurfaceColor));
        if (lockIcon != null) lockIcon.setImageTintList(ColorStateList.valueOf(onSurfaceColor));
        if (bothIcon != null) bothIcon.setImageTintList(ColorStateList.valueOf(onSurfaceColor));
    }

    private GradientDrawable createBottomSheetBackground(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(color);
        background.setCornerRadii(new float[]{
                dpToPx(16), dpToPx(16),
                dpToPx(16), dpToPx(16),
                0, 0,
                0, 0
        });
        return background;
    }

    private float dpToPx(int dp) {
        if (getContext() == null) return dp;
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void setupRadioGroup() {
        bothRadio.setChecked(true);

        homeRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                lockRadio.setChecked(false);
                bothRadio.setChecked(false);
                selectedFlag = android.app.WallpaperManager.FLAG_SYSTEM;
            }
        });

        lockRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                homeRadio.setChecked(false);
                bothRadio.setChecked(false);
                selectedFlag = android.app.WallpaperManager.FLAG_LOCK;
            }
        });

        bothRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                homeRadio.setChecked(false);
                lockRadio.setChecked(false);
                selectedFlag = android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK;
            }
        });
    }

    private void setupCardClickListeners() {
        homeCard.setOnClickListener(v -> {
            if (!homeRadio.isChecked()) {
                homeRadio.setChecked(true);
            }
        });

        lockCard.setOnClickListener(v -> {
            if (!lockRadio.isChecked()) {
                lockRadio.setChecked(true);
            }
        });

        bothCard.setOnClickListener(v -> {
            if (!bothRadio.isChecked()) {
                bothRadio.setChecked(true);
            }
        });
    }

    private void setupSetWallpaperButton() {
        setWallpaperButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOptionSelected(selectedFlag);
            }
            dismiss();
        });
    }
}
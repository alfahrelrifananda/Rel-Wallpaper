package com.example.relwallpaper.ui.collection;
import com.example.relwallpaper.R;

public class CollectionItem {
    private String title;
    private String description;
    private int itemCount;
    private int imageResource;

    public CollectionItem(String title, String description, int itemCount) {
        this.title = title;
        this.description = description;
        this.itemCount = itemCount;
        this.imageResource = getImageResourceByTitle(title);
    }

    private int getImageResourceByTitle(String title) {
        switch (title.toLowerCase()) {
            case "for fun":
                return R.drawable.for_placeholder;
            case "doodle":
                return R.drawable.doodle_placeholder;
            case "nature":
                return R.drawable.nature_placeholder;
            case "shape":
                return R.drawable.shape_placeholder;
            case "retro":
                return R.drawable.retro_placeholder;
            case "branding":
                return R.drawable.brand_wallpaper;
            case "motif":
                return R.drawable.motif_placeholder;
            case "blob":
                return R.drawable.blob_placeholder;
            case "gradients":
                return R.drawable.gradients_placeholder;
            case "elements":
                return R.drawable.elements_placeholder;
            case "art & culture":
                return R.drawable.arts_placeholder;
            case "blurred":
                return R.drawable.blur_placeholder;
            default:
                return android.R.drawable.ic_dialog_info;
        }
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getItemCount() { return itemCount; }
    public int getImageResource() { return imageResource; }
}

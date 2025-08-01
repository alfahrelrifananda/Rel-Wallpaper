package com.example.relwallpaper.ui.home.model;

import java.io.Serializable;
import java.util.List;

public class Wallpaper implements Serializable {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String fileName;
    private String createdAt;
    private List<String> tags;
    private List<String> colorPalette;
    private String username;

    public Wallpaper() {}

    public Wallpaper(String id, String name, String description, String imageUrl,
                     String fileName, String createdAt, List<String> tags, List<String> colorPalette, String username) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.fileName = fileName;
        this.createdAt = createdAt;
        this.tags = tags;
        this.colorPalette = colorPalette;
        this.username = username;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getColorPalette() { return colorPalette; }
    public void setColorPalette(List<String> colorPalette) { this.colorPalette = colorPalette; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstTag() {
        return (tags != null && !tags.isEmpty()) ? tags.get(0) : "";
    }

    public String getPrimaryColor() {
        return (colorPalette != null && !colorPalette.isEmpty()) ? colorPalette.get(0) : "#6750A4";
    }

    public String getFormattedUsername() {
        return (username != null && !username.isEmpty()) ? "By " + username : "By Anonymous";
    }
}

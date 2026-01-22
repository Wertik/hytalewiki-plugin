package org.hytalewiki.net.response;

import com.google.gson.annotations.SerializedName;

public class SearchEntry {
    private int id;

    private String key;
    private String title;
    private String excerpt;

    @SerializedName("matched_title")
    private String matchedTitle;

    private String fragment;
    private String description;
    private Thumbnail thumbnail;

    public SearchEntry() {
        // gson
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getMatchedTitle() {
        return matchedTitle;
    }

    public void setMatchedTitle(String matchedTitle) {
        this.matchedTitle = matchedTitle;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public static class Thumbnail {

        @SerializedName("mimetype")
        private String mimeType;
        private int width;
        private int height;
        private int duration;
        private String url;

        public Thumbnail() {
            // gson
        }

        public String getMimeType() {
            return mimeType;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getDuration() {
            return duration;
        }

        public String getUrl() {
            return url;
        }
    }
}

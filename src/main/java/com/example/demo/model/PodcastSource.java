package com.example.demo.model;

public enum PodcastSource {

    GOOAYE(
            "gooaye",
            "股癌",
            "https://feeds.soundon.fm/podcasts/954689a5-3096-43a4-a80b-7810b219cef3.xml "
    ),
    JENNY(
            "jenny",
            "財女Jenny",
            "https://feeds.soundon.fm/podcasts/4a8660a0-e0d0-490b-8d46-c28219606f47.xml"
    ),
    HORN(
            "horn",
            "游庭澔財經號角",
            "https://feeds.soundon.fm/podcasts/5bab8352-94c5-4f2d-b809-53c057278dd1.xml"
    );

    private final String id;
    private final String displayName;
    private final String rssUrl;

    PodcastSource(String id, String displayName, String rssUrl) {
        this.id = id;
        this.displayName = displayName;
        this.rssUrl = rssUrl;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public static PodcastSource fromId(String id) {
        for (PodcastSource source : values()) {
            if (source.id.equalsIgnoreCase(id)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的 podcast source: " + id);
    }
}

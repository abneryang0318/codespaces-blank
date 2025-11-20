package com.example.demo.model;

import java.util.List;

public class PodcastEpisodesByQueryResponse {

    private String query;
    private String sourceId;
    private String sourceName;
    private String rssUrl;
    private int limit;
    private List<PodcastEpisode> episodes;
    private String message;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public void setRssUrl(String rssUrl) {
        this.rssUrl = rssUrl;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<PodcastEpisode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<PodcastEpisode> episodes) {
        this.episodes = episodes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

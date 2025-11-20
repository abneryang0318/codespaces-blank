package com.example.demo.model;

public class PodcastQueryRequest {

    private String query;

    public PodcastQueryRequest() {
    }

    public PodcastQueryRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}

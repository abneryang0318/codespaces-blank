package com.example.demo.service;

import com.example.demo.model.PodcastSource;
import org.springframework.stereotype.Service;

@Service
public class PodcastSourceResolver {

    public PodcastSource resolveByKeyword(String keyword) {
        if (keyword == null) {
            throw new IllegalArgumentException("keyword 不可為 null");
        }
        String k = keyword.trim().toLowerCase();

        if (k.contains("股癌") || k.contains("gooaye")) {
            return PodcastSource.GOOAYE;
        }
        if (k.contains("財女") || k.contains("jenny")) {
            return PodcastSource.JENNY;
        }
        if (k.contains("號角") || k.contains("庭澔")) {
            return PodcastSource.HORN;
        }

        throw new IllegalArgumentException("無法從關鍵字判斷節目: " + keyword);
    }
}

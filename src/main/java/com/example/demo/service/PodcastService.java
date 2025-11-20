package com.example.demo.service;

import com.example.demo.model.PodcastEpisode;
import com.example.demo.model.PodcastEpisodesByQueryResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PodcastService {

    private final SoundOnRssService soundOnRssService;

    // sourceId -> RSS URL
    private final Map<String, String> rssMap;
    // sourceId -> 顯示用名稱
    private final Map<String, String> displayNameMap;

    public PodcastService(SoundOnRssService soundOnRssService) {
        this.soundOnRssService = soundOnRssService;

        Map<String, String> map = new HashMap<>();
        // 股癌
        map.put("gooaye", "https://feeds.soundon.fm/podcasts/954689a5-3096-43a4-a80b-7810b219cef3.xml");
        // 財女 Jenny
        map.put("jenny", "https://feeds.soundon.fm/podcasts/4a8660a0-e0d0-490b-8d46-c28219606f47.xml");
        // 游庭澔 財經號角
        map.put("horn", "https://feeds.soundon.fm/podcasts/5bab8352-94c5-4f2d-b809-53c057278dd1.xml");
        this.rssMap = Collections.unmodifiableMap(map);

        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("gooaye", "股癌");
        nameMap.put("jenny", "財女 Jenny");
        nameMap.put("horn", "游庭澔 財經號角");
        this.displayNameMap = Collections.unmodifiableMap(nameMap);
    }

    // 既有功能：用 sourceId 直接抓最新 N 集
    public List<PodcastEpisode> getEpisodesBySourceId(String sourceId, int limit) {
        String rssUrl = rssMap.get(sourceId);
        if (rssUrl == null) {
            throw new IllegalArgumentException("Unknown podcast source id: " + sourceId);
        }
        return soundOnRssService.fetchEpisodes(rssUrl, limit);
    }

    // 新功能：從自然語言 query 解析出來源與集數，並回傳結果
    public PodcastEpisodesByQueryResponse getEpisodesByQuery(String query) {
        PodcastEpisodesByQueryResponse response = new PodcastEpisodesByQueryResponse();
        response.setQuery(query);

        String sourceId = resolveSourceId(query);
        if (sourceId == null) {
            response.setMessage("無法從輸入文字中辨識節目來源，請包含關鍵字，例如：股癌、財女、庭澔、財經號角。");
            response.setLimit(0);
            response.setEpisodes(Collections.emptyList());
            return response;
        }

        int limit = resolveLimit(query);

        response.setSourceId(sourceId);
        response.setSourceName(displayNameMap.getOrDefault(sourceId, sourceId));
        response.setRssUrl(rssMap.get(sourceId));
        response.setLimit(limit);

        try {
            List<PodcastEpisode> episodes = getEpisodesBySourceId(sourceId, limit);
            response.setEpisodes(episodes);
            response.setMessage("OK");
        } catch (Exception e) {
            response.setEpisodes(Collections.emptyList());
            response.setMessage("取得 RSS 失敗: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        return response;
    }

    // 解析節目來源
    private String resolveSourceId(String query) {
        if (query == null) {
            return null;
        }
        String lower = query.toLowerCase(Locale.ROOT);

        if (lower.contains("股癌") || lower.contains("gooaye") || lower.contains("古來")) {
            return "gooaye";
        }
        if (lower.contains("財女") || lower.contains("jenny")) {
            return "jenny";
        }
        if (lower.contains("庭澔") || lower.contains("游庭") || lower.contains("財經號角") || lower.contains("horn")) {
            return "horn";
        }
        return null;
    }

    // 解析要抓幾集
    private int resolveLimit(String query) {
        if (query == null) {
            return 1;
        }

        // 先找阿拉伯數字: "3 集"、"最近 5 集" 等
        String digitsPattern = "(\\d+)\\s*集";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(digitsPattern).matcher(query);
        if (m.find()) {
            try {
                int value = Integer.parseInt(m.group(1));
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 常見中文數字: 一二三四五
        Map<Character, Integer> cnNumberMap = new HashMap<>();
        cnNumberMap.put('一', 1);
        cnNumberMap.put('二', 2);
        cnNumberMap.put('三', 3);
        cnNumberMap.put('四', 4);
        cnNumberMap.put('五', 5);

        for (Map.Entry<Character, Integer> entry : cnNumberMap.entrySet()) {
            char ch = entry.getKey();
            if (query.indexOf(ch) >= 0 && query.contains("集")) {
                return entry.getValue();
            }
        }

        // 預設: 最新一集
        return 1;
    }
}

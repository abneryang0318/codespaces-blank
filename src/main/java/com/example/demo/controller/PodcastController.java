package com.example.demo.controller;

import com.example.demo.model.PodcastEpisode;
import com.example.demo.model.PodcastEpisodesByQueryResponse;
import com.example.demo.model.PodcastQueryRequest;
import com.example.demo.service.PodcastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/podcasts")
public class PodcastController {

    private final PodcastService podcastService;

    public PodcastController(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    // 既有：用 sourceId + limit 直接抓
    // 例：GET /api/podcasts/episodes?source=jenny&limit=3
    @GetMapping("/episodes")
    public ResponseEntity<List<PodcastEpisode>> getEpisodes(
            @RequestParam("source") String source,
            @RequestParam(name = "limit", defaultValue = "1") int limit
    ) {
        List<PodcastEpisode> episodes = podcastService.getEpisodesBySourceId(source, limit);
        return ResponseEntity.ok(episodes);
    }

    // 新增：用自然語言 query 解析來源與集數
    // 例：
    //  POST /api/podcasts/episodes/resolve
    //  body: { "query": "財女 jenny 最近五集" }
    @PostMapping("/episodes/resolve")
    public ResponseEntity<PodcastEpisodesByQueryResponse> getEpisodesByQuery(
            @RequestBody PodcastQueryRequest request
    ) {
        PodcastEpisodesByQueryResponse resp =
                podcastService.getEpisodesByQuery(request.getQuery());
        return ResponseEntity.ok(resp);
    }
}

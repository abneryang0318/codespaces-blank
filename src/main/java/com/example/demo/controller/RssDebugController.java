package com.example.demo.controller;

import com.example.demo.model.PodcastEpisode;
import com.example.demo.service.SoundOnRssService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rss")
public class RssDebugController {

    private final SoundOnRssService soundOnRssService;

    public RssDebugController(SoundOnRssService soundOnRssService) {
        this.soundOnRssService = soundOnRssService;
    }

    @GetMapping("/episodes")
    public List<PodcastEpisode> listEpisodes(
            @RequestParam("rssUrl") String rssUrl,
            @RequestParam(name = "limit", defaultValue = "5") int limit
    ) {
        return soundOnRssService.fetchEpisodes(rssUrl, limit);
    }
}

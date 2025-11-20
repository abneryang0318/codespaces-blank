package com.example.demo.controller;

import com.example.demo.api.AnalyzeTextRequest;
import com.example.demo.api.AnalyzeTextResponse;
import com.example.demo.api.CleanTranscriptRequest;
import com.example.demo.api.CleanTranscriptResponse;
import com.example.demo.api.ProcessPodcastVttResponse;
import com.example.demo.model.PodcastAnalysisResult;
import com.example.demo.model.PodcastWithTaResponse;
import com.example.demo.service.AiService;
import com.example.demo.service.PodcastEnrichmentService;
import com.example.demo.util.TranscriptCleaner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final AiService aiService;
    private final PodcastEnrichmentService podcastEnrichmentService;

    public AnalyzeController(AiService aiService,
                             PodcastEnrichmentService podcastEnrichmentService) {
        this.aiService = aiService;
        this.podcastEnrichmentService = podcastEnrichmentService;
    }

    @PostMapping("/analyze-text")
    public ResponseEntity<AnalyzeTextResponse> analyzeText(@RequestBody AnalyzeTextRequest request) {
        String input = request.getText();
        String result = aiService.summarizeWithGemini(input);
        return ResponseEntity.ok(new AnalyzeTextResponse(result));
    }

    @PostMapping("/analyze-podcast-text")
    public ResponseEntity<PodcastAnalysisResult> analyzePodcast(@RequestBody AnalyzeTextRequest request) {
        PodcastAnalysisResult result = aiService.analyzePodcastText(request.getText());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze-podcast-with-ta")
    public ResponseEntity<PodcastWithTaResponse> analyzePodcastWithTa(
            @RequestBody AnalyzeTextRequest request
    ) {
        PodcastWithTaResponse response = podcastEnrichmentService.analyzeWithTa(request.getText());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clean-transcript")
    public ResponseEntity<CleanTranscriptResponse> cleanTranscript(
            @RequestBody CleanTranscriptRequest request
    ) {
        String cleaned = TranscriptCleaner.clean(request.getRawText());
        return ResponseEntity.ok(new CleanTranscriptResponse(cleaned));
    }

    // 新增：從 VTT 一路跑到 podcast 分析 + TA 的一條龍 endpoint
    @PostMapping("/process-podcast-vtt")
    public ResponseEntity<ProcessPodcastVttResponse> processPodcastVtt(
            @RequestBody CleanTranscriptRequest request
    ) {
        String cleaned = TranscriptCleaner.clean(request.getRawText());
        PodcastWithTaResponse analysis = podcastEnrichmentService.analyzeWithTa(cleaned);

        ProcessPodcastVttResponse response = new ProcessPodcastVttResponse();
        response.setCleanedText(cleaned);
        response.setAnalysis(analysis);

        return ResponseEntity.ok(response);
    }
}

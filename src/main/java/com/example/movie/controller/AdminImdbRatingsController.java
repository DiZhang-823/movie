package com.example.movie.controller;

import com.example.movie.dto.ImdbRatingsSyncStatusResponse;
import com.example.movie.service.ImdbRatingsSyncService;
import com.example.movie.service.TvSeasonImdbRatingsSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/imdb-ratings")
public class AdminImdbRatingsController {

    private final ImdbRatingsSyncService imdbRatingsSyncService;
    private final TvSeasonImdbRatingsSyncService tvSeasonImdbRatingsSyncService;

    public AdminImdbRatingsController(
            ImdbRatingsSyncService imdbRatingsSyncService,
            TvSeasonImdbRatingsSyncService tvSeasonImdbRatingsSyncService
    ) {
        this.imdbRatingsSyncService = imdbRatingsSyncService;
        this.tvSeasonImdbRatingsSyncService = tvSeasonImdbRatingsSyncService;
    }

    @PostMapping("/sync")
    public ImdbRatingsSyncStatusResponse sync() {
        return imdbRatingsSyncService.startSyncRatings();
    }

    @PostMapping("/sync-missing")
    public ImdbRatingsSyncStatusResponse syncMissing() {
        return imdbRatingsSyncService.startSyncMissingRatings();
    }

    @GetMapping("/status")
    public ImdbRatingsSyncStatusResponse status() {
        return imdbRatingsSyncService.getStatus();
    }

    @PostMapping("/tv-seasons/sync")
    public ImdbRatingsSyncStatusResponse syncTvSeasons() {
        return tvSeasonImdbRatingsSyncService.startSyncRatings();
    }

    @PostMapping("/tv-seasons/sync-missing")
    public ImdbRatingsSyncStatusResponse syncMissingTvSeasons() {
        return tvSeasonImdbRatingsSyncService.startSyncMissingRatings();
    }

    @GetMapping("/tv-seasons/status")
    public ImdbRatingsSyncStatusResponse tvSeasonStatus() {
        return tvSeasonImdbRatingsSyncService.getStatus();
    }
}

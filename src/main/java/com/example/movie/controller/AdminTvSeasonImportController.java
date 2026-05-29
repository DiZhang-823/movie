package com.example.movie.controller;

import com.example.movie.dto.TmdbImportIntervalRequest;
import com.example.movie.dto.TmdbImportRunRequest;
import com.example.movie.dto.TmdbImportRunResponse;
import com.example.movie.dto.TmdbImportStatusResponse;
import com.example.movie.dto.TmdbImportSwitchRequest;
import com.example.movie.dto.TvSeasonImportRequest;
import com.example.movie.dto.TvSeasonImportResponse;
import com.example.movie.entity.TvSeason;
import com.example.movie.service.TmdbTvSeasonImportAdminService;
import com.example.movie.service.TvSeasonService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tmdb-tv-seasons")
public class AdminTvSeasonImportController {

    private final TvSeasonService tvSeasonService;
    private final TmdbTvSeasonImportAdminService tmdbTvSeasonImportAdminService;

    public AdminTvSeasonImportController(
            TvSeasonService tvSeasonService,
            TmdbTvSeasonImportAdminService tmdbTvSeasonImportAdminService) {
        this.tvSeasonService = tvSeasonService;
        this.tmdbTvSeasonImportAdminService = tmdbTvSeasonImportAdminService;
    }

    @GetMapping("/status")
    public TmdbImportStatusResponse status() {
        return tmdbTvSeasonImportAdminService.getStatus();
    }

    @PutMapping("/switch")
    public TmdbImportStatusResponse switchImport(@Valid @RequestBody TmdbImportSwitchRequest request) {
        return tmdbTvSeasonImportAdminService.setEnabled(request.getEnabled());
    }

    @PutMapping("/interval")
    public TmdbImportStatusResponse updateInterval(@Valid @RequestBody TmdbImportIntervalRequest request) {
        return tmdbTvSeasonImportAdminService.setIntervalSeconds(request.getIntervalSeconds());
    }

    @PostMapping("/run")
    public TmdbImportRunResponse run(@Valid @RequestBody TmdbImportRunRequest request) {
        return tmdbTvSeasonImportAdminService.runBatch(request.getTmdbId(), request.getCount());
    }

    @PostMapping("/run-once")
    public TvSeasonImportResponse runOnce(@Valid @RequestBody TvSeasonImportRequest request) {
        TvSeason tvSeason = tvSeasonService.syncTvSeason(request.getTvId(), request.getSeasonNumber());

        TvSeasonImportResponse response = new TvSeasonImportResponse();
        response.setId(tvSeason.getId());
        response.setTmdbTvId(tvSeason.getTmdbTvId());
        response.setTmdbSeasonId(tvSeason.getTmdbSeasonId());
        response.setSeasonNumber(tvSeason.getSeasonNumber());
        response.setTitle(tvSeason.getTitle());
        response.setEpisodeCount(tvSeason.getEpisodeCount());
        response.setImdbParentId(tvSeason.getImdbParentId());
        return response;
    }
}

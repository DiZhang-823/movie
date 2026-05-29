package com.example.movie.controller;

import com.example.movie.dto.TvSeasonImportRequest;
import com.example.movie.dto.TvSeasonImportResponse;
import com.example.movie.entity.TvSeason;
import com.example.movie.service.TvSeasonService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tmdb-tv-seasons")
public class AdminTvSeasonImportController {

    private final TvSeasonService tvSeasonService;

    public AdminTvSeasonImportController(TvSeasonService tvSeasonService) {
        this.tvSeasonService = tvSeasonService;
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

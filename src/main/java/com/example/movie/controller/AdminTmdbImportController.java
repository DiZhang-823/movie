package com.example.movie.controller;

import com.example.movie.dto.TmdbImportRunRequest;
import com.example.movie.dto.TmdbImportStatusResponse;
import com.example.movie.dto.TmdbImportSwitchRequest;
import com.example.movie.service.TmdbImportAdminService;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tmdb-import")
public class AdminTmdbImportController {

    private final TmdbImportAdminService tmdbImportAdminService;

    public AdminTmdbImportController(TmdbImportAdminService tmdbImportAdminService) {
        this.tmdbImportAdminService = tmdbImportAdminService;
    }

    @GetMapping("/status")
    public TmdbImportStatusResponse status() {
        return tmdbImportAdminService.getStatus();
    }

    @PutMapping("/switch")
    public TmdbImportStatusResponse switchImport(@Valid @RequestBody TmdbImportSwitchRequest request) {
        return tmdbImportAdminService.setEnabled(request.getEnabled());
    }

    @PostMapping("/run-once")
    public TmdbMovieResponse runOnce(@Valid @RequestBody TmdbImportRunRequest request) {
        return tmdbImportAdminService.runOnce(request.getTmdbId());
    }
}

package com.example.movie.service;

import com.example.movie.dto.TmdbImportStatusResponse;
import com.example.movie.entity.ImportProgress;
import com.example.movie.repository.ImportProgressRepository;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TmdbImportAdminService {

    private static final String TASK_NAME = "tmdb-movie-auto-import";

    private final MovieService movieService;
    private final ImportProgressRepository importProgressRepository;
    private final long startId;

    public TmdbImportAdminService(
            MovieService movieService,
            ImportProgressRepository importProgressRepository,
            @Value("${movie.tmdb.auto-import.start-id:151}") long startId) {
        this.movieService = movieService;
        this.importProgressRepository = importProgressRepository;
        this.startId = startId;
    }

    @Transactional
    public TmdbImportStatusResponse getStatus() {
        ImportProgress progress = getOrCreateProgress();
        return toResponse(progress);
    }

    @Transactional
    public TmdbImportStatusResponse setEnabled(Boolean enabled) {
        ImportProgress progress = getOrCreateProgress();
        progress.setEnabled(enabled);
        return toResponse(importProgressRepository.save(progress));
    }

    public TmdbMovieResponse runOnce(Long tmdbId) {
        return movieService.syncMovie(tmdbId);
    }

    private ImportProgress getOrCreateProgress() {
        return importProgressRepository.findByTaskName(TASK_NAME)
                .orElseGet(() -> {
                    ImportProgress progress = new ImportProgress();
                    progress.setTaskName(TASK_NAME);
                    progress.setNextTmdbId(startId);
                    progress.setEnabled(true);
                    return importProgressRepository.save(progress);
                });
    }

    private TmdbImportStatusResponse toResponse(ImportProgress progress) {
        return new TmdbImportStatusResponse(
                progress.getNextTmdbId(),
                Boolean.TRUE.equals(progress.getEnabled()),
                progress.getUpdatedAt()
        );
    }
}

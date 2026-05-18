package com.example.movie.service;

import com.example.movie.dto.TmdbImportStatusResponse;
import com.example.movie.dto.TmdbImportRunResponse;
import com.example.movie.entity.ImportProgress;
import com.example.movie.repository.ImportProgressRepository;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    public TmdbImportRunResponse runBatch(Long startTmdbId, Integer count) {
        int requestedCount = count == null ? 1 : count;
        List<TmdbImportRunResponse.Item> items = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (int index = 0; index < requestedCount; index++) {
            long tmdbId = startTmdbId + index;
            try {
                TmdbMovieResponse movie = movieService.syncMovie(tmdbId);
                items.add(new TmdbImportRunResponse.Item(
                        tmdbId,
                        true,
                        movie.getTitle(),
                        movie.getOriginalTitle(),
                        null
                ));
                successCount++;
            } catch (Exception ex) {
                items.add(new TmdbImportRunResponse.Item(
                        tmdbId,
                        false,
                        null,
                        null,
                        ex.getMessage()
                ));
                failureCount++;
            }
        }

        return new TmdbImportRunResponse(
                startTmdbId,
                requestedCount,
                successCount,
                failureCount,
                items
        );
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

package com.example.movie.service;

import com.example.movie.entity.ImportProgress;
import com.example.movie.repository.ImportProgressRepository;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TmdbMovieImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbMovieImportScheduler.class);
    private static final String TASK_NAME = "tmdb-movie-auto-import";

    private final MovieService movieService;
    private final ImportProgressRepository importProgressRepository;
    private final boolean enabled;
    private final long startId;
    private final int defaultIntervalSeconds;

    public TmdbMovieImportScheduler(
            MovieService movieService,
            ImportProgressRepository importProgressRepository,
            @Value("${movie.tmdb.auto-import.enabled:true}") boolean enabled,
            @Value("${movie.tmdb.auto-import.start-id:151}") long startId,
            @Value("${movie.tmdb.auto-import.interval-seconds:0}") int configuredIntervalSeconds,
            @Value("${movie.tmdb.auto-import.delay-ms:300000}") long configuredDelayMs) {
        this.movieService = movieService;
        this.importProgressRepository = importProgressRepository;
        this.enabled = enabled;
        this.startId = startId;
        this.defaultIntervalSeconds = resolveDefaultIntervalSeconds(configuredIntervalSeconds, configuredDelayMs);
    }

    @Scheduled(
            initialDelayString = "${movie.tmdb.auto-import.initial-delay-ms:3000}",
            fixedDelay = 1000
    )
    public void importNextMovie() {
        if (!enabled) {
            return;
        }

        ImportProgress progress = getOrCreateProgress();
        if (!Boolean.TRUE.equals(progress.getEnabled())) {
            return;
        }
        if (!shouldRunNow(progress)) {
            return;
        }

        long tmdbId = progress.getNextTmdbId();

        try {
            log.info("Starting scheduled TMDB movie import. tmdbId={}", tmdbId);
            TmdbMovieResponse response = movieService.syncMovie(tmdbId);
            log.info("Finished scheduled TMDB movie import. tmdbId={}, title={}", tmdbId, response.getTitle());
        } catch (Exception ex) {
            log.warn("Skipped scheduled TMDB movie import. tmdbId={}", tmdbId, ex);
        } finally {
            progress.setNextTmdbId(tmdbId + 1);
            importProgressRepository.save(progress);
        }
    }

    private ImportProgress getOrCreateProgress() {
        ImportProgress progress = importProgressRepository.findByTaskName(TASK_NAME)
                .orElseGet(() -> {
                    ImportProgress newProgress = new ImportProgress();
                    newProgress.setTaskName(TASK_NAME);
                    newProgress.setNextTmdbId(startId);
                    newProgress.setEnabled(true);
                    newProgress.setIntervalSeconds(defaultIntervalSeconds);
                    return importProgressRepository.save(newProgress);
                });

        if (progress.getIntervalSeconds() == null) {
            progress.setIntervalSeconds(defaultIntervalSeconds);
            return importProgressRepository.save(progress);
        }

        return progress;
    }

    private boolean shouldRunNow(ImportProgress progress) {
        LocalDateTime updatedAt = progress.getUpdatedAt();
        if (updatedAt == null) {
            return true;
        }

        return !updatedAt.plusSeconds(resolveIntervalSeconds(progress)).isAfter(LocalDateTime.now());
    }

    private int resolveIntervalSeconds(ImportProgress progress) {
        return progress.getIntervalSeconds() == null ? defaultIntervalSeconds : progress.getIntervalSeconds();
    }

    private int resolveDefaultIntervalSeconds(int configuredIntervalSeconds, long configuredDelayMs) {
        if (configuredIntervalSeconds > 0) {
            return configuredIntervalSeconds;
        }
        return Math.max(1, (int) Math.ceil(configuredDelayMs / 1000.0));
    }
}

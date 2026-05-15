package com.example.movie.service;

import com.example.movie.entity.ImportProgress;
import com.example.movie.repository.ImportProgressRepository;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TmdbMovieImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbMovieImportScheduler.class);
    private static final String TASK_NAME = "tmdb-movie-auto-import";

    private final MovieService movieService;
    private final ImportProgressRepository importProgressRepository;
    private final boolean enabled;
    private final long startId;

    public TmdbMovieImportScheduler(
            MovieService movieService,
            ImportProgressRepository importProgressRepository,
            @Value("${movie.tmdb.auto-import.enabled:true}") boolean enabled,
            @Value("${movie.tmdb.auto-import.start-id:151}") long startId) {
        this.movieService = movieService;
        this.importProgressRepository = importProgressRepository;
        this.enabled = enabled;
        this.startId = startId;
    }

    @Scheduled(
            initialDelayString = "${movie.tmdb.auto-import.initial-delay-ms:3000}",
            fixedDelayString = "${movie.tmdb.auto-import.delay-ms:5000}"
    )
    public void importNextMovie() {
        if (!enabled) {
            return;
        }

        ImportProgress progress = getOrCreateProgress();
        if (!Boolean.TRUE.equals(progress.getEnabled())) {
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
        return importProgressRepository.findByTaskName(TASK_NAME)
                .orElseGet(() -> {
                    ImportProgress progress = new ImportProgress();
                    progress.setTaskName(TASK_NAME);
                    progress.setNextTmdbId(startId);
                    progress.setEnabled(true);
                    return importProgressRepository.save(progress);
                });
    }
}

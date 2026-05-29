package com.example.movie.service;

import com.example.movie.entity.ImportProgress;
import com.example.movie.entity.TvSeason;
import com.example.movie.repository.ImportProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TmdbTvSeasonImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbTvSeasonImportScheduler.class);
    private static final String TASK_NAME = "tmdb-tv-season-auto-import";

    private final TvSeasonService tvSeasonService;
    private final ImportProgressRepository importProgressRepository;
    private final boolean enabled;
    private final long startId;
    private final int defaultIntervalSeconds;

    public TmdbTvSeasonImportScheduler(
            TvSeasonService tvSeasonService,
            ImportProgressRepository importProgressRepository,
            @Value("${tv.tmdb.auto-import.enabled:true}") boolean enabled,
            @Value("${tv.tmdb.auto-import.start-id:1}") long startId,
            @Value("${tv.tmdb.auto-import.interval-seconds:0}") int configuredIntervalSeconds,
            @Value("${tv.tmdb.auto-import.delay-ms:300000}") long configuredDelayMs) {
        this.tvSeasonService = tvSeasonService;
        this.importProgressRepository = importProgressRepository;
        this.enabled = enabled;
        this.startId = startId;
        this.defaultIntervalSeconds = resolveDefaultIntervalSeconds(configuredIntervalSeconds, configuredDelayMs);
    }

    @Scheduled(
            initialDelayString = "${tv.tmdb.auto-import.initial-delay-ms:3000}",
            fixedDelay = 1000
    )
    public void importNextTvShow() {
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

        long tvId = progress.getNextTmdbId();

        try {
            log.info("Starting scheduled TMDB TV season import. tvId={}", tvId);
            List<TvSeason> seasons = tvSeasonService.syncTvShowSeasons(tvId);
            log.info("Finished scheduled TMDB TV season import. tvId={}, seasons={}", tvId, seasons.size());
        } catch (Exception ex) {
            log.warn("Skipped scheduled TMDB TV season import. tvId={}", tvId, ex);
        } finally {
            progress.setNextTmdbId(tvId + 1);
            importProgressRepository.save(progress);
        }
    }

    private ImportProgress getOrCreateProgress() {
        ImportProgress progress = importProgressRepository.findByTaskName(TASK_NAME)
                .orElseGet(() -> {
                    ImportProgress newProgress = new ImportProgress();
                    newProgress.setTaskName(TASK_NAME);
                    newProgress.setNextTmdbId(startId);
                    newProgress.setEnabled(false);
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

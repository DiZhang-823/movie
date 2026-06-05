package com.example.movie.service;

import com.example.movie.dto.ImdbRatingsSyncResponse;
import com.example.movie.dto.ImdbRatingsSyncStatusResponse;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

@Service
public class TvSeasonImdbRatingsSyncService {

    private static final Logger log = LoggerFactory.getLogger(TvSeasonImdbRatingsSyncService.class);
    private static final int BATCH_SIZE = 500;
    private static final int DEFAULT_SYNC_BATCH_SIZE = 500;
    private static final String UPDATE_SQL = """
            UPDATE tv_season
            SET imdb_vote_average = ?,
                imdb_vote_count = ?,
                imdb_rating_updated_at = ?
            WHERE id = ?
            """;
    private static final String LAST_SYNCED_AT_SQL = """
            SELECT MAX(imdb_rating_updated_at)
            FROM tv_season
            WHERE imdb_rating_updated_at IS NOT NULL
            """;
    private static final String MISSING_RATING_COUNT_SQL = """
            SELECT COUNT(*)
            FROM tv_season
            WHERE imdb_parent_id IS NOT NULL
              AND TRIM(imdb_parent_id) <> ''
              AND (imdb_vote_average IS NULL OR imdb_vote_count IS NULL)
            """;
    private static final String TV_SEASONS_WITH_IMDB_PARENT_SQL = """
            SELECT id, imdb_parent_id, season_number
            FROM tv_season
            WHERE imdb_parent_id IS NOT NULL
              AND TRIM(imdb_parent_id) <> ''
              AND season_number IS NOT NULL
            """;
    private static final String TV_SEASONS_MISSING_IMDB_RATING_SQL = """
            SELECT id, imdb_parent_id, season_number
            FROM tv_season
            WHERE imdb_parent_id IS NOT NULL
              AND TRIM(imdb_parent_id) <> ''
              AND season_number IS NOT NULL
              AND (imdb_vote_average IS NULL OR imdb_vote_count IS NULL)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final HttpClient httpClient;
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean syncing = false;
    private volatile LocalDateTime currentStartedAt;
    private volatile String lastErrorMessage;
    private volatile ImdbRatingsSyncResponse lastResult;

    @Value("${imdb.ratings.url:https://datasets.imdbws.com/title.ratings.tsv.gz}")
    private String ratingsUrl;

    @Value("${imdb.episodes.url:https://datasets.imdbws.com/title.episode.tsv.gz}")
    private String episodesUrl;

    @Value("${imdb.ratings.timeout-minutes:15}")
    private Long ratingsTimeoutMinutes;

    @Value("${imdb.tv-season.sync.batch-size:" + DEFAULT_SYNC_BATCH_SIZE + "}")
    private Integer syncBatchSize;

    public TvSeasonImdbRatingsSyncService(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public synchronized ImdbRatingsSyncStatusResponse startSyncRatings() {
        return startSyncRatings(false);
    }

    public synchronized ImdbRatingsSyncStatusResponse startSyncMissingRatings() {
        return startSyncRatings(true);
    }

    private synchronized ImdbRatingsSyncStatusResponse startSyncRatings(boolean missingOnly) {
        if (!syncing) {
            syncing = true;
            currentStartedAt = LocalDateTime.now();
            lastErrorMessage = null;
            syncExecutor.submit(() -> runSyncJob(missingOnly));
        }

        return getStatus();
    }

    private void runSyncJob(boolean missingOnly) {
        try {
            ImdbRatingsSyncResponse result = syncRatings(missingOnly);
            lastResult = result;
            lastErrorMessage = null;
        } catch (Exception error) {
            log.error("Failed to sync TV season IMDb ratings", error);
            lastErrorMessage = getRootErrorMessage(error);
        } finally {
            syncing = false;
            currentStartedAt = null;
        }
    }

    private ImdbRatingsSyncResponse syncRatings(boolean missingOnly) {
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime ratingUpdatedAt = LocalDateTime.now();
        Map<SeasonKey, List<Long>> seasonIdsByKey = loadSeasonIdsByKey(missingOnly);
        int seasonsWithImdbParentId = seasonIdsByKey.values().stream().mapToInt(List::size).sum();

        if (seasonsWithImdbParentId == 0) {
            return buildSyncResponse(startedAt, missingOnly, 0, 0, 0, 0);
        }

        int totalRatingsRows = 0;
        int matchedSeasons = 0;
        int updatedSeasons = 0;
        Set<Long> matchedSeasonIds = new HashSet<>();
        int batchSize = resolveSyncBatchSize();
        List<SeasonBatch> seasonBatches = buildSeasonBatches(seasonIdsByKey, batchSize);

        for (SeasonBatch seasonBatch : seasonBatches) {
            BatchSyncResult batchResult = syncSeasonBatch(seasonBatch, ratingUpdatedAt);
            totalRatingsRows = Math.max(totalRatingsRows, batchResult.totalRatingsRows());
            updatedSeasons += batchResult.updatedSeasons();
            matchedSeasonIds.addAll(batchResult.matchedSeasonIds());
            matchedSeasons = matchedSeasonIds.size();
        }

        return buildSyncResponse(
                startedAt,
                missingOnly,
                seasonsWithImdbParentId,
                totalRatingsRows,
                matchedSeasons,
                updatedSeasons
        );
    }

    private BatchSyncResult syncSeasonBatch(SeasonBatch seasonBatch, LocalDateTime ratingUpdatedAt) {
        EpisodeMapping episodeMapping = loadEpisodeMapping(seasonBatch.seasonIdsByKey());
        if (episodeMapping.episodeIdsByImdbId().isEmpty()) {
            return new BatchSyncResult(0, 0, Set.of());
        }

        int totalRatingsRows = 0;
        Map<Long, RatingAccumulator> ratingsBySeasonId = new HashMap<>();

        try (InputStream responseBody = openDatasetStream(ratingsUrl);
             GZIPInputStream gzipInputStream = new GZIPInputStream(responseBody);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))) {

            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                totalRatingsRows++;
                String[] columns = line.split("\t");
                if (columns.length < 3) {
                    continue;
                }

                List<Long> seasonIds = episodeMapping.episodeIdsByImdbId().remove(normalizeImdbId(columns[0]));
                if (seasonIds == null || seasonIds.isEmpty()) {
                    continue;
                }

                BigDecimal rating = new BigDecimal(columns[1]);
                int voteCount = Integer.parseInt(columns[2]);
                for (Long seasonId : seasonIds) {
                    ratingsBySeasonId.computeIfAbsent(seasonId, ignored -> new RatingAccumulator())
                            .add(rating, voteCount);
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Failed to sync TV season IMDb ratings", error);
        }

        List<Object[]> updateArgs = buildUpdateArgs(ratingsBySeasonId, ratingUpdatedAt);
        int updatedSeasons = updateMatchedSeasons(updateArgs);

        return new BatchSyncResult(totalRatingsRows, updatedSeasons, ratingsBySeasonId.keySet());
    }

    private List<SeasonBatch> buildSeasonBatches(Map<SeasonKey, List<Long>> seasonIdsByKey, int batchSize) {
        List<SeasonBatch> batches = new ArrayList<>();
        Map<SeasonKey, List<Long>> currentBatch = new HashMap<>();
        int currentSeasonCount = 0;

        for (Map.Entry<SeasonKey, List<Long>> entry : seasonIdsByKey.entrySet()) {
            if (!currentBatch.isEmpty() && currentSeasonCount + entry.getValue().size() > batchSize) {
                batches.add(new SeasonBatch(currentBatch));
                currentBatch = new HashMap<>();
                currentSeasonCount = 0;
            }

            currentBatch.put(entry.getKey(), entry.getValue());
            currentSeasonCount += entry.getValue().size();
        }

        if (!currentBatch.isEmpty()) {
            batches.add(new SeasonBatch(currentBatch));
        }

        return batches;
    }

    private int resolveSyncBatchSize() {
        if (syncBatchSize == null || syncBatchSize < 1) {
            return DEFAULT_SYNC_BATCH_SIZE;
        }
        return syncBatchSize;
    }

    private EpisodeMapping loadEpisodeMapping(Map<SeasonKey, List<Long>> seasonIdsByKey) {
        Map<String, List<Long>> episodeIdsByImdbId = new HashMap<>();
        Set<Long> matchedSeasonIds = new HashSet<>();

        try (InputStream responseBody = openDatasetStream(episodesUrl);
             GZIPInputStream gzipInputStream = new GZIPInputStream(responseBody);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))) {

            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t");
                if (columns.length < 4 || "\\N".equals(columns[2])) {
                    continue;
                }

                List<Long> seasonIds = seasonIdsByKey.get(new SeasonKey(normalizeImdbId(columns[1]), columns[2]));
                if (seasonIds == null || seasonIds.isEmpty()) {
                    continue;
                }

                String episodeImdbId = normalizeImdbId(columns[0]);
                episodeIdsByImdbId.computeIfAbsent(episodeImdbId, ignored -> new ArrayList<>())
                        .addAll(seasonIds);
                matchedSeasonIds.addAll(seasonIds);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Failed to read IMDb episode dataset", error);
        }

        return new EpisodeMapping(episodeIdsByImdbId, matchedSeasonIds.size());
    }

    private List<Object[]> buildUpdateArgs(Map<Long, RatingAccumulator> ratingsBySeasonId, LocalDateTime ratingUpdatedAt) {
        List<Object[]> updateArgs = new ArrayList<>();
        Timestamp updatedAt = Timestamp.valueOf(ratingUpdatedAt);
        ratingsBySeasonId.forEach((seasonId, accumulator) -> updateArgs.add(new Object[] {
                accumulator.averageRating(),
                accumulator.voteCount(),
                updatedAt,
                seasonId
        }));
        return updateArgs;
    }

    private ImdbRatingsSyncResponse buildSyncResponse(
            LocalDateTime startedAt,
            boolean missingOnly,
            int seasonsWithImdbParentId,
            int totalRatingsRows,
            int matchedSeasons,
            int updatedSeasons
    ) {
        ImdbRatingsSyncResponse response = new ImdbRatingsSyncResponse();
        response.setDatasetUrl(ratingsUrl);
        response.setMoviesWithImdbId(seasonsWithImdbParentId);
        response.setTotalRatingsRows(totalRatingsRows);
        response.setMatchedMovies(matchedSeasons);
        response.setUpdatedMovies(updatedSeasons);
        response.setMissingMovies(seasonsWithImdbParentId - matchedSeasons);
        response.setMissingOnly(missingOnly);
        response.setStartedAt(startedAt);
        response.setFinishedAt(LocalDateTime.now());
        return response;
    }

    public ImdbRatingsSyncStatusResponse getStatus() {
        Timestamp lastSyncedAt = jdbcTemplate.queryForObject(LAST_SYNCED_AT_SQL, Timestamp.class);
        Long missingRatingCount = jdbcTemplate.queryForObject(MISSING_RATING_COUNT_SQL, Long.class);

        ImdbRatingsSyncStatusResponse response = new ImdbRatingsSyncStatusResponse();
        response.setLastSyncedAt(lastSyncedAt == null ? null : lastSyncedAt.toLocalDateTime());
        response.setMissingRatingCount(missingRatingCount == null ? 0 : missingRatingCount);
        response.setSyncing(syncing);
        response.setCurrentStartedAt(currentStartedAt);
        response.setLastErrorMessage(lastErrorMessage);
        response.setLastResult(lastResult);
        return response;
    }

    private Map<SeasonKey, List<Long>> loadSeasonIdsByKey(boolean missingOnly) {
        Map<SeasonKey, List<Long>> seasonIdsByKey = new HashMap<>();

        jdbcTemplate.query(
                missingOnly ? TV_SEASONS_MISSING_IMDB_RATING_SQL : TV_SEASONS_WITH_IMDB_PARENT_SQL,
                resultSet -> {
                    String imdbParentId = normalizeImdbId(resultSet.getString("imdb_parent_id"));
                    int seasonNumber = resultSet.getInt("season_number");
                    if (!imdbParentId.isBlank()) {
                        seasonIdsByKey.computeIfAbsent(
                                        new SeasonKey(imdbParentId, Integer.toString(seasonNumber)),
                                        ignored -> new ArrayList<>()
                                )
                                .add(resultSet.getLong("id"));
                    }
                }
        );

        return seasonIdsByKey;
    }

    private InputStream openDatasetStream(String datasetUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(datasetUrl))
                .timeout(Duration.ofMinutes(ratingsTimeoutMinutes))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalStateException("IMDb dataset returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String normalizeImdbId(String imdbId) {
        return imdbId == null ? "" : imdbId.trim().toLowerCase(Locale.ROOT);
    }

    private int updateMatchedSeasons(List<Object[]> updateArgs) {
        if (updateArgs.isEmpty()) {
            return 0;
        }

        Integer updatedCount = transactionTemplate.execute(status -> {
            int count = 0;
            for (int index = 0; index < updateArgs.size(); index += BATCH_SIZE) {
                List<Object[]> chunk = updateArgs.subList(index, Math.min(index + BATCH_SIZE, updateArgs.size()));
                jdbcTemplate.batchUpdate(UPDATE_SQL, chunk);
                count += chunk.size();
            }
            return count;
        });
        return updatedCount == null ? 0 : updatedCount;
    }

    private String getRootErrorMessage(Exception error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? error.getMessage() : current.getMessage();
    }

    @PreDestroy
    public void shutdown() {
        syncExecutor.shutdownNow();
    }

    private record SeasonKey(String imdbParentId, String seasonNumber) {
    }

    private record EpisodeMapping(Map<String, List<Long>> episodeIdsByImdbId, int matchedSeasonCount) {
    }

    private record SeasonBatch(Map<SeasonKey, List<Long>> seasonIdsByKey) {
    }

    private record BatchSyncResult(int totalRatingsRows, int updatedSeasons, Set<Long> matchedSeasonIds) {
    }

    private static class RatingAccumulator {
        private BigDecimal ratingTotal = BigDecimal.ZERO;
        private int ratingCount = 0;
        private int voteCount = 0;

        void add(BigDecimal rating, int votes) {
            ratingTotal = ratingTotal.add(rating);
            ratingCount++;
            voteCount += votes;
        }

        BigDecimal averageRating() {
            return ratingTotal.divide(BigDecimal.valueOf(ratingCount), 4, RoundingMode.HALF_UP);
        }

        int voteCount() {
            return voteCount;
        }
    }
}

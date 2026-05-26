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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

@Service
public class ImdbRatingsSyncService {

    private static final Logger log = LoggerFactory.getLogger(ImdbRatingsSyncService.class);
    private static final int BATCH_SIZE = 500;
    private static final String UPDATE_SQL = """
            UPDATE movie
            SET imdb_vote_average = ?,
                imdb_vote_count = ?,
                imdb_rating_updated_at = ?
            WHERE id = ?
            """;
    private static final String LAST_SYNCED_AT_SQL = """
            SELECT MAX(imdb_rating_updated_at)
            FROM movie
            WHERE imdb_rating_updated_at IS NOT NULL
            """;
    private static final String MISSING_RATING_COUNT_SQL = """
            SELECT COUNT(*)
            FROM movie
            WHERE imdb_id IS NOT NULL
              AND TRIM(imdb_id) <> ''
              AND (imdb_vote_average IS NULL OR imdb_vote_count IS NULL)
            """;
    private static final String MOVIES_WITH_IMDB_ID_SQL = """
            SELECT id, imdb_id
            FROM movie
            WHERE imdb_id IS NOT NULL
              AND TRIM(imdb_id) <> ''
            """;
    private static final String MOVIES_MISSING_IMDB_RATING_SQL = """
            SELECT id, imdb_id
            FROM movie
            WHERE imdb_id IS NOT NULL
              AND TRIM(imdb_id) <> ''
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

    @Value("${imdb.ratings.timeout-minutes:15}")
    private Long ratingsTimeoutMinutes;

    public ImdbRatingsSyncService(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
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
            log.error("Failed to sync IMDb ratings", error);
            lastErrorMessage = getRootErrorMessage(error);
        } finally {
            syncing = false;
            currentStartedAt = null;
        }
    }

    private ImdbRatingsSyncResponse syncRatings(boolean missingOnly) {
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime ratingUpdatedAt = LocalDateTime.now();
        Map<String, List<Long>> movieIdsByImdbId = loadMovieIdsByImdbId(missingOnly);
        int moviesWithImdbId = movieIdsByImdbId.values().stream().mapToInt(List::size).sum();
        int totalRows = 0;
        int matchedMovies = 0;
        List<Object[]> updateArgs = new ArrayList<>();

        if (moviesWithImdbId == 0) {
            return buildSyncResponse(startedAt, missingOnly, 0, 0, 0, 0);
        }

        try (InputStream responseBody = openDatasetStream();
             GZIPInputStream gzipInputStream = new GZIPInputStream(responseBody);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))) {

            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                totalRows++;
                String[] columns = line.split("\t");
                if (columns.length < 3) {
                    continue;
                }

                List<Long> movieIds = movieIdsByImdbId.remove(normalizeImdbId(columns[0]));
                if (movieIds == null || movieIds.isEmpty()) {
                    continue;
                }

                matchedMovies += movieIds.size();
                for (Long movieId : movieIds) {
                    updateArgs.add(new Object[] {
                            new BigDecimal(columns[1]),
                            Integer.valueOf(columns[2]),
                            Timestamp.valueOf(ratingUpdatedAt),
                            movieId
                    });
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Failed to sync IMDb ratings", error);
        }

        int updatedMovies = updateMatchedMovies(updateArgs);

        return buildSyncResponse(startedAt, missingOnly, moviesWithImdbId, totalRows, matchedMovies, updatedMovies);
    }

    private ImdbRatingsSyncResponse buildSyncResponse(
            LocalDateTime startedAt,
            boolean missingOnly,
            int moviesWithImdbId,
            int totalRows,
            int matchedMovies,
            int updatedMovies
    ) {
        ImdbRatingsSyncResponse response = new ImdbRatingsSyncResponse();
        response.setDatasetUrl(ratingsUrl);
        response.setMoviesWithImdbId(moviesWithImdbId);
        response.setTotalRatingsRows(totalRows);
        response.setMatchedMovies(matchedMovies);
        response.setUpdatedMovies(updatedMovies);
        response.setMissingMovies(moviesWithImdbId - matchedMovies);
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

    private Map<String, List<Long>> loadMovieIdsByImdbId(boolean missingOnly) {
        Map<String, List<Long>> movieIdsByImdbId = new HashMap<>();

        jdbcTemplate.query(
                missingOnly ? MOVIES_MISSING_IMDB_RATING_SQL : MOVIES_WITH_IMDB_ID_SQL,
                resultSet -> {
                    String imdbId = normalizeImdbId(resultSet.getString("imdb_id"));
                    if (!imdbId.isBlank()) {
                        movieIdsByImdbId.computeIfAbsent(imdbId, ignored -> new ArrayList<>())
                                .add(resultSet.getLong("id"));
                    }
                }
        );

        return movieIdsByImdbId;
    }

    private InputStream openDatasetStream() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ratingsUrl))
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

    private int updateMatchedMovies(List<Object[]> updateArgs) {
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
}

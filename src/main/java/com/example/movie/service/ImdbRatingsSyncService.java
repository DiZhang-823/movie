package com.example.movie.service;

import com.example.movie.dto.ImdbRatingsSyncResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
import java.util.zip.GZIPInputStream;

@Service
public class ImdbRatingsSyncService {

    private static final int BATCH_SIZE = 500;
    private static final String UPDATE_SQL = """
            UPDATE movie
            SET imdb_vote_average = ?,
                imdb_vote_count = ?,
                imdb_rating_updated_at = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient;

    @Value("${imdb.ratings.url:https://datasets.imdbws.com/title.ratings.tsv.gz}")
    private String ratingsUrl;

    public ImdbRatingsSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public synchronized ImdbRatingsSyncResponse syncRatings() {
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime ratingUpdatedAt = LocalDateTime.now();
        Map<String, List<Long>> movieIdsByImdbId = loadMovieIdsByImdbId();
        int moviesWithImdbId = movieIdsByImdbId.values().stream().mapToInt(List::size).sum();
        int totalRows = 0;
        int matchedMovies = 0;
        int updatedMovies = 0;
        List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);

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
                    batchArgs.add(new Object[] {
                            new BigDecimal(columns[1]),
                            Integer.valueOf(columns[2]),
                            Timestamp.valueOf(ratingUpdatedAt),
                            movieId
                    });
                }

                if (batchArgs.size() >= BATCH_SIZE) {
                    updatedMovies += flushBatch(batchArgs);
                }
            }

            updatedMovies += flushBatch(batchArgs);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to sync IMDb ratings", error);
        }

        ImdbRatingsSyncResponse response = new ImdbRatingsSyncResponse();
        response.setDatasetUrl(ratingsUrl);
        response.setMoviesWithImdbId(moviesWithImdbId);
        response.setTotalRatingsRows(totalRows);
        response.setMatchedMovies(matchedMovies);
        response.setUpdatedMovies(updatedMovies);
        response.setMissingMovies(moviesWithImdbId - matchedMovies);
        response.setStartedAt(startedAt);
        response.setFinishedAt(LocalDateTime.now());
        return response;
    }

    private Map<String, List<Long>> loadMovieIdsByImdbId() {
        Map<String, List<Long>> movieIdsByImdbId = new HashMap<>();

        jdbcTemplate.query(
                "SELECT id, imdb_id FROM movie WHERE imdb_id IS NOT NULL AND TRIM(imdb_id) <> ''",
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
                .timeout(Duration.ofMinutes(3))
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

    private int flushBatch(List<Object[]> batchArgs) {
        if (batchArgs.isEmpty()) {
            return 0;
        }

        int count = batchArgs.size();
        jdbcTemplate.batchUpdate(UPDATE_SQL, batchArgs);
        batchArgs.clear();
        return count;
    }
}

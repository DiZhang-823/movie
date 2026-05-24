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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Service
public class ImdbRatingsSyncService {

    private static final int BATCH_SIZE = 500;
    private static final String UPDATE_SQL = """
            UPDATE movie
            SET imdb_vote_average = ?,
                imdb_vote_count = ?,
                imdb_rating_updated_at = ?
            WHERE imdb_id = ?
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
        Set<String> remainingImdbIds = new HashSet<>(loadMovieImdbIds());
        int moviesWithImdbId = remainingImdbIds.size();
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
                if (columns.length < 3 || !remainingImdbIds.contains(columns[0])) {
                    continue;
                }

                matchedMovies++;
                remainingImdbIds.remove(columns[0]);
                batchArgs.add(new Object[] {
                        new BigDecimal(columns[1]),
                        Integer.valueOf(columns[2]),
                        Timestamp.valueOf(ratingUpdatedAt),
                        columns[0]
                });

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

    private List<String> loadMovieImdbIds() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT imdb_id FROM movie WHERE imdb_id IS NOT NULL AND imdb_id <> ''",
                String.class
        );
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

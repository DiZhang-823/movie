package com.example.movie.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImdbRatingsSyncResponse {

    private String datasetUrl;
    private Integer moviesWithImdbId;
    private Integer totalRatingsRows;
    private Integer matchedMovies;
    private Integer updatedMovies;
    private Integer missingMovies;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}

package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MovieCollectionItemResponse {

    private Long id;
    private Long tmdbId;
    private String title;
    private LocalDate releaseDate;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private BigDecimal imdbVoteAverage;
    private Integer imdbVoteCount;
    private LocalDateTime imdbRatingUpdatedAt;
    private String posterPath;
    private Integer sequenceNumber;
}

package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonMovieCreditResponse {

    private Long movieId;
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private LocalDate releaseDate;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private BigDecimal imdbVoteAverage;
    private Integer imdbVoteCount;
    private LocalDateTime imdbRatingUpdatedAt;
    private String posterPath;
    private List<String> jobs;
    private String character;
}

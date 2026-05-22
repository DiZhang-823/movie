package com.example.movie.dto;

import com.example.movie.enums.UserMovieStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
public class MovieResponse {
    private Long id;
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private BigDecimal popularity;
    private LocalDate releaseDate;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private String posterPath;
    private LocalDateTime createdAt;
    private UserMovieStatus userMovieStatus;
    private Byte userRating;
    private LocalDate userWatchedDate;
    private LocalDateTime userMovieUpdatedAt;
}

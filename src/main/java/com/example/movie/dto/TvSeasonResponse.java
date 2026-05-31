package com.example.movie.dto;

import com.example.movie.enums.UserMovieStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class TvSeasonResponse {
    private Long id;
    private Long tmdbTvId;
    private Long tmdbSeasonId;
    private Integer seasonNumber;
    private Integer seasonCount;
    private String title;
    private String originalTitle;
    private String showName;
    private String originalShowName;
    private LocalDate airDate;
    private Integer year;
    private Integer episodeCount;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private BigDecimal imdbVoteAverage;
    private Integer imdbVoteCount;
    private LocalDateTime imdbRatingUpdatedAt;
    private String posterPath;
    private LocalDateTime createdAt;
    private UserMovieStatus userTvSeasonStatus;
    private Byte userRating;
    private LocalDate userWatchedDate;
    private LocalDateTime userTvSeasonUpdatedAt;
}

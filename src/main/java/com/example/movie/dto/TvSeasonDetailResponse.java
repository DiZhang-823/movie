package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TvSeasonDetailResponse {
    private Long id;
    private Long tmdbTvId;
    private Long tmdbSeasonId;
    private Integer seasonNumber;
    private Integer seasonCount;
    private String title;
    private String originalTitle;
    private String showName;
    private String originalShowName;
    private String overview;
    private String posterPath;
    private LocalDate airDate;
    private Integer year;
    private Integer episodeCount;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private BigDecimal imdbVoteAverage;
    private Integer imdbVoteCount;
    private LocalDateTime imdbRatingUpdatedAt;
    private String imdbParentId;
    private List<String> genres;
    private List<String> countries;
    private List<PersonItemResponse> producers;
    private List<PersonItemResponse> directors;
    private List<PersonItemResponse> writers;
    private List<PersonItemResponse> cast;
    private List<TvSeasonCollectionItemResponse> collectionSeasons;
}

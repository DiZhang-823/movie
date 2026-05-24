package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MovieDetailResponse {

    private Long id;
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private BigDecimal popularity;
    private LocalDate releaseDate;
    private Long revenue;
    private Integer runtime;
    private BigDecimal voteAverage;
    private Integer voteCount;
    private String posterPath;
    private String imdbId;

    private List<String> genres;
    private List<String> companies;
    private List<String> countries;

    private String collectionName;
    private List<MovieCollectionItemResponse> collectionMovies;

    private List<PersonItemResponse> directors;
    private List<PersonItemResponse> cast;
}

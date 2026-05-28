package com.example.movie.dto;

import com.example.movie.enums.UserMovieStatusFilter;
import lombok.Data;

@Data
public class MovieQuery {

    private String title;

    private Long genreId;

    private Integer year;

    private String regionCode;

    private UserMovieStatusFilter userMovieStatus;

    private Integer userRatingMin;

    private Integer userRatingMax;

    private Integer movieRatingMin;

    private Integer movieRatingMax;
}

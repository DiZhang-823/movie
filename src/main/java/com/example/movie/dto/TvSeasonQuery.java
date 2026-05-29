package com.example.movie.dto;

import com.example.movie.enums.UserMovieStatusFilter;
import lombok.Data;

@Data
public class TvSeasonQuery {
    private String title;
    private Long genreId;
    private Integer year;
    private String regionCode;
    private UserMovieStatusFilter userTvSeasonStatus;
    private Integer userRatingMin;
    private Integer userRatingMax;
    private Integer tvSeasonRatingMin;
    private Integer tvSeasonRatingMax;
}

package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TvSeasonCollectionItemResponse {

    private Long id;
    private Integer seasonNumber;
    private String seasonName;
    private LocalDate airDate;
    private BigDecimal voteAverage;
    private BigDecimal imdbVoteAverage;
    private String posterPath;
}

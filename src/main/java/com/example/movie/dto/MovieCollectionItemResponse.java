package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovieCollectionItemResponse {

    private Long id;
    private Long tmdbId;
    private String title;
    private LocalDate releaseDate;
    private BigDecimal voteAverage;
    private String posterPath;
    private Integer sequenceNumber;
}

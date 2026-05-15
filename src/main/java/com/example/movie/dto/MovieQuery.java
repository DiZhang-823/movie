package com.example.movie.dto;

import lombok.Data;

@Data
public class MovieQuery {

    private String title;

    private Long genreId;

    private Integer year;

    private String regionCode;

}

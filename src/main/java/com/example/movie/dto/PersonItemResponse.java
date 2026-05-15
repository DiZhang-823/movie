package com.example.movie.dto;

import lombok.Data;

@Data
public class PersonItemResponse {

    private Long personId;
    private String name;
    private String originalName;
    private String profilePath;
    private String character;
    private String job;
}
package com.example.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Crew {

    private Long id;

    private String name;

    @JsonProperty("original_name")
    private String originalName;

    private Byte gender;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    @JsonProperty("profile_path")
    private String profilePath;

    private BigDecimal popularity;

    private String job;

    //暂时没用

    @JsonProperty("credit_id")
    private String creditId;

    private Boolean adult;

    private String department;
}
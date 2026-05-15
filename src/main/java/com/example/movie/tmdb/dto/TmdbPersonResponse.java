package com.example.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TmdbPersonResponse {
    private Long id;

    private String name;

    private String originalName;

    private Byte gender;

    private LocalDate birthday;

    private LocalDate deathday;

    @JsonProperty("place_of_birth")
    private String placeOfBirth;

    private String biography;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    @JsonProperty("profile_path")
    private String profilePath;

    private BigDecimal popularity;
}
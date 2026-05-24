package com.example.movie.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PersonDetailResponse {

    private Long id;
    private Long tmdbId;
    private String name;
    private String originalName;
    private Byte gender;
    private LocalDate birthday;
    private LocalDate deathday;
    private String placeOfBirth;
    private String biography;
    private String knownForDepartment;
    private String profilePath;
    private BigDecimal popularity;
    private Integer movieCreditCount;
    private List<PersonMovieCreditResponse> credits;
}

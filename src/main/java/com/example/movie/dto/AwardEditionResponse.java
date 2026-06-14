package com.example.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AwardEditionResponse {
    private Long id;
    private Long awardBodyId;
    private String awardBodyName;
    private String name;
    private Integer editionNumber;
    private Integer awardYear;
    private LocalDate ceremonyDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

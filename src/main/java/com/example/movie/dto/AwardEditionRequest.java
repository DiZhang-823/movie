package com.example.movie.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AwardEditionRequest {

    @NotNull(message = "Award body id cannot be null")
    private Long awardBodyId;

    @NotBlank(message = "Award edition name cannot be blank")
    @Size(max = 150, message = "Award edition name cannot exceed 150 characters")
    private String name;

    @NotNull(message = "Edition number cannot be null")
    @Min(value = 1, message = "Edition number must be at least 1")
    private Integer editionNumber;

    @Min(value = 1800, message = "Award year must be no earlier than 1800")
    @Max(value = 2100, message = "Award year must be no later than 2100")
    private Integer awardYear;

    private LocalDate ceremonyDate;

    private String description;
}

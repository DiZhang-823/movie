package com.example.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AwardBodyRequest {

    @NotBlank(message = "Award body name cannot be blank")
    @Size(max = 100, message = "Award body name cannot exceed 100 characters")
    private String name;

    @Size(max = 150, message = "Original name cannot exceed 150 characters")
    private String originalName;

    private Integer sortOrder;

    private String description;
}

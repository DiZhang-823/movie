package com.example.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AwardCategoryRequest {

    @NotNull(message = "Award edition id cannot be null")
    private Long awardEditionId;

    @NotBlank(message = "Award category name cannot be blank")
    @Size(max = 150, message = "Award category name cannot exceed 150 characters")
    private String name;

    private Integer sortOrder;

    private String description;
}

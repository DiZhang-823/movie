package com.example.movie.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AwardNominationRequest {

    @NotNull(message = "Award category id cannot be null")
    private Long awardCategoryId;

    private Long movieId;

    private Long tvSeasonId;

    private Long personId;

    private Boolean winner;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;

    private Integer sortOrder;
}

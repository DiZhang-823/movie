package com.example.movie.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmdbImportRunRequest {

    @NotNull
    @Min(1)
    private Long tmdbId;

    @Min(1)
    private Integer count = 1;
}

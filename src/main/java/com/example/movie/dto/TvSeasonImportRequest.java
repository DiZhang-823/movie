package com.example.movie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TvSeasonImportRequest {

    @NotNull
    private Long tvId;

    @NotNull
    private Integer seasonNumber;
}

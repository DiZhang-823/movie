package com.example.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TmdbImportStatusResponse {
    private Long nextTmdbId;
    private Boolean enabled;
    private Integer intervalSeconds;
    private LocalDateTime updatedAt;
}

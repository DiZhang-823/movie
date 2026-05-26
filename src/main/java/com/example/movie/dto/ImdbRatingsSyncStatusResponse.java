package com.example.movie.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImdbRatingsSyncStatusResponse {

    private LocalDateTime lastSyncedAt;
    private Long missingRatingCount;
}

package com.example.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TmdbImportRunResponse {

    private Long startTmdbId;
    private Integer requestedCount;
    private Integer successCount;
    private Integer failureCount;
    private List<Item> items;

    @Getter
    @AllArgsConstructor
    public static class Item {
        private Long tmdbId;
        private Boolean success;
        private String title;
        private String originalTitle;
        private String errorMessage;
    }
}

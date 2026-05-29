package com.example.movie.dto;

import lombok.Data;

@Data
public class TvSeasonImportResponse {
    private Long id;
    private Long tmdbTvId;
    private Long tmdbSeasonId;
    private Integer seasonNumber;
    private String title;
    private Integer episodeCount;
    private String imdbParentId;
}

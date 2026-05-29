package com.example.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TmdbTvSeasonResponse {

    @JsonProperty("_id")
    private String externalObjectId;

    @JsonProperty("air_date")
    private LocalDate airDate;

    private List<Episode> episodes;

    private Long id;

    private String name;

    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    @JsonProperty("vote_average")
    private BigDecimal voteAverage;

    @Data
    public static class Episode {
        @JsonProperty("air_date")
        private LocalDate airDate;

        @JsonProperty("episode_number")
        private Integer episodeNumber;

        private Long id;

        private String name;

        private String overview;

        private Integer runtime;

        @JsonProperty("season_number")
        private Integer seasonNumber;

        @JsonProperty("still_path")
        private String stillPath;

        @JsonProperty("vote_count")
        private Integer voteCount;
    }
}

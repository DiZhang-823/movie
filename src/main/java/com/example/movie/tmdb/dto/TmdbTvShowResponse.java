package com.example.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TmdbTvShowResponse {

    private Long id;

    private List<TmdbMovieResponse.Genre> genres;

    private String name;

    @JsonProperty("original_name")
    private String originalName;

    private String overview;

    @JsonProperty("origin_country")
    private List<String> originCountry;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("vote_average")
    private BigDecimal voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private List<Season> seasons;

    @JsonProperty("external_ids")
    private ExternalIds externalIds;

    @Data
    public static class ExternalIds {
        @JsonProperty("imdb_id")
        private String imdbId;
    }

    @Data
    public static class Season {
        private Long id;

        @JsonProperty("season_number")
        private Integer seasonNumber;

        @JsonProperty("episode_count")
        private Integer episodeCount;

        private String name;
    }
}

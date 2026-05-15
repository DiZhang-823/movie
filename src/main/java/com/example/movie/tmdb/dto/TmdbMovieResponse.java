package com.example.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TmdbMovieResponse {
    private Long id;

    @JsonProperty("belongs_to_collection")
    private CollectionInfo belongsToCollection;

    private List<Genre> genres;

    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;

    private BigDecimal popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("production_companies")
    private List<ProductionCompany> productionCompanies;

    @JsonProperty("production_countries")
    private List<ProductionCountry> productionCountries;

    @JsonProperty("release_date")
    private LocalDate releaseDate;

    private Long revenue;

    private Integer runtime;

    @JsonProperty("vote_average")
    private BigDecimal voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @JsonProperty("imdb_id")
    private String imdbId;

    //================ 内部类 =================
    @Data
    public static class CollectionInfo {
        private Long id;
        private String name;
    }

    @Data
    public static class Genre {
        private Long id;
        private String name;
    }

    @Data
    public static class ProductionCompany {
        private Long id;
        @JsonProperty("logo_path")
        private String logoPath;
        private String name;
    }

    @Data
    public static class ProductionCountry {
        @JsonProperty("iso_3166_1")
        private String iso31661;
        private String name;
    }
}
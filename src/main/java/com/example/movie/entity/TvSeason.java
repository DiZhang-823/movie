package com.example.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "tv_season", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_tv_season_tmdb_tv_id_season_number",
                columnNames = {"tmdb_tv_id", "season_number"}
        )
})
public class TvSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_tv_id", nullable = false)
    private Long tmdbTvId;

    @Column(name = "tmdb_season_id")
    private Long tmdbSeasonId;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "season_count")
    private Integer seasonCount;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "show_name", nullable = false)
    private String showName;

    @Column(name = "original_show_name")
    private String originalShowName;

    @Lob
    private String overview;

    @Column(name = "poster_path")
    private String posterPath;

    @Column(name = "air_date")
    private LocalDate airDate;

    private Integer year;

    @Column(name = "episode_count")
    private Integer episodeCount;

    @Column(name = "vote_average", precision = 10, scale = 4)
    private BigDecimal voteAverage;

    @Column(name = "vote_count")
    private Integer voteCount;

    @Column(name = "imdb_vote_average", precision = 10, scale = 4)
    private BigDecimal imdbVoteAverage;

    @Column(name = "imdb_vote_count")
    private Integer imdbVoteCount;

    @Column(name = "imdb_rating_updated_at")
    private LocalDateTime imdbRatingUpdatedAt;

    @Column(name = "imdb_parent_id", length = 20)
    private String imdbParentId;

    @ManyToMany
    @JoinTable(
            name = "tv_season_genre",
            joinColumns = @JoinColumn(name = "tv_season_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "tv_season_country",
            joinColumns = @JoinColumn(name = "tv_season_id"),
            inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> countries = new HashSet<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

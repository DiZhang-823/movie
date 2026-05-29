package com.example.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "genre", uniqueConstraints = {
        @UniqueConstraint(name = "uk_genre_tmdb_id_media_type", columnNames = {"tmdb_id", "media_type"})
})
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType = "MOVIE";

    @Column(nullable = false)
    private String name;

}

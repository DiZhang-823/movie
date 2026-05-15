package com.example.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "person", uniqueConstraints = {
        @UniqueConstraint(name = "uk_person_tmdb_id", columnNames = "tmdb_id")
})
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String name;

    @Column(name = "original_name")
    private String originalName;

    private Byte gender;

    private LocalDate birthday;

    private LocalDate deathday;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Lob
    private String biography;

    @Column(name = "known_for_department")
    private String knownForDepartment;

    @Column(name = "profile_path")
    private String profilePath;

    @Column(precision = 10, scale = 4)
    private BigDecimal popularity;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

//    /**
//     * 拼接 TMDB 图片完整地址（前端直接用）
//     */
//    @Transient
//    public String getProfileUrl() {
//        if (profilePath == null || profilePath.isBlank()) {
//            return null;
//        }
//        return "https://image.tmdb.org/t/p/w500" + profilePath;
//    }
}
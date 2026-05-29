package com.example.movie.entity;

import com.example.movie.enums.UserMovieStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_tv_season",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_tv_season_user_tv_season",
                columnNames = {"user_id", "tv_season_id"}
        ))
@Data
public class UserTvSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tv_season_id", nullable = false)
    private TvSeason tvSeason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserMovieStatus status;

    private Byte rating;

    @Column(name = "watched_date")
    private LocalDate watchedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

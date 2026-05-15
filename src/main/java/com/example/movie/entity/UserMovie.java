package com.example.movie.entity;

import com.example.movie.enums.UserMovieStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "user_movie",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_movie_user_movie", columnNames = {"user_id", "movie_id"}))
@Data
public class UserMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 电影
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    // 状态（重点）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserMovieStatus status;

    // 评分（1-5）
    @Column
    private Byte rating;

    // 看过日期
    @Column(name = "watched_date")
    private LocalDate watchedDate;

    // 创建时间
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 更新时间
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== 生命周期自动填充 =====
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
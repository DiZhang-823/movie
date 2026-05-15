package com.example.movie.dto;

import com.example.movie.enums.UserMovieStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserMovieResponse {

    private Long movieId;
    private UserMovieStatus status;
    private Byte rating;
    private LocalDate watchedDate;
    private LocalDateTime updatedAt;
}

package com.example.movie.dto;

import com.example.movie.enums.UserMovieStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserTvSeasonRequest {
    private UserMovieStatus status;
    private Byte rating;
    private LocalDate watchedDate;
}

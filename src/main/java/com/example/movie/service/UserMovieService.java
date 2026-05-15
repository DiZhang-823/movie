package com.example.movie.service;

import com.example.movie.entity.Movie;
import com.example.movie.entity.User;
import com.example.movie.entity.UserMovie;
import com.example.movie.enums.UserMovieStatus;
import com.example.movie.repository.UserMovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserMovieService {

    private final UserMovieRepository userMovieRepository;

    public UserMovieService(UserMovieRepository userMovieRepository) {
        this.userMovieRepository = userMovieRepository;
    }

    @Transactional
    public UserMovie saveUserMovie(User user, Movie movie, UserMovieStatus status, Byte rating, LocalDate watchedDate) {
        UserMovie userMovie = userMovieRepository.findByUserAndMovie(user, movie)
                .orElseGet(UserMovie::new);

        userMovie.setUser(user);
        userMovie.setMovie(movie);
        userMovie.setStatus(status);

        if (status == UserMovieStatus.WATCHED) {
            if (rating != null && (rating < 1 || rating > 5)) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }

            userMovie.setRating(rating);
            userMovie.setWatchedDate(watchedDate != null ? watchedDate : LocalDate.now());
        } else {
            userMovie.setRating(null);
            userMovie.setWatchedDate(null);
        }

        return userMovieRepository.save(userMovie);
    }

    @Transactional(readOnly = true)
    public UserMovie findUserMovie(User user, Movie movie) {
        return userMovieRepository.findByUserAndMovie(user, movie).orElse(null);
    }
}
package com.example.movie.repository;

import com.example.movie.entity.Movie;
import com.example.movie.entity.User;
import com.example.movie.entity.UserMovie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserMovieRepository extends JpaRepository<UserMovie, Long> {

    Optional<UserMovie> findByUserAndMovie(User user, Movie movie);
}

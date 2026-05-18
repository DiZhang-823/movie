package com.example.movie.repository;

import com.example.movie.entity.Movie;
import com.example.movie.entity.User;
import com.example.movie.entity.UserMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMovieRepository extends JpaRepository<UserMovie, Long> {

    Optional<UserMovie> findByUserAndMovie(User user, Movie movie);

    @Query("""
            SELECT um FROM UserMovie um
            JOIN FETCH um.movie
            WHERE um.user.id = :userId
            AND um.movie.id IN :movieIds
            """)
    List<UserMovie> findByUserIdAndMovieIds(
            @Param("userId") Long userId,
            @Param("movieIds") List<Long> movieIds);
}

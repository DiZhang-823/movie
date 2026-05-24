package com.example.movie.repository;

import com.example.movie.entity.Movie;
import com.example.movie.entity.MoviePerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MoviePersonRepository extends JpaRepository<MoviePerson, Long> {
    void deleteByMovie(Movie movie);

    @Query("""
        SELECT mp FROM MoviePerson mp
        JOIN FETCH mp.person
        WHERE mp.movie.id = :movieId
        ORDER BY mp.sortOrder ASC
    """)
    List<MoviePerson> findByMovieIdWithPersonOrderBySortOrderAsc(@Param("movieId") Long movieId);

    @Query("""
        SELECT mp FROM MoviePerson mp
        JOIN FETCH mp.movie
        WHERE mp.person.id = :personId
        ORDER BY
            CASE WHEN mp.movie.releaseDate IS NULL THEN 1 ELSE 0 END,
            mp.movie.releaseDate DESC,
            mp.movie.id DESC,
            mp.sortOrder ASC
    """)
    List<MoviePerson> findByPersonIdWithMovieOrderByReleaseDateDesc(@Param("personId") Long personId);

    List<MoviePerson> findByMovieIdOrderBySortOrderAsc(Long movieId);
}

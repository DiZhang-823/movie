package com.example.movie.repository;

import com.example.movie.entity.Movie;
import com.example.movie.entity.MoviePerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoviePersonRepository extends JpaRepository<MoviePerson, Long> {
    void deleteByMovie(Movie movie);
    List<MoviePerson> findByMovieIdOrderBySortOrderAsc(Long movieId);
}


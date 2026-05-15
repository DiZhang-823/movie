package com.example.movie.repository;

import com.example.movie.entity.Movie;
import com.example.movie.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Optional<Movie> findByTmdbId(Long tmdbId);

    @Query("""
        SELECT m FROM Movie m
        JOIN m.genres g
        WHERE g.id = :genreId
    """)
    Page<Movie> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);

    @Query("""
        SELECT m FROM Movie m
        JOIN m.genres g
        WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))
        AND g.id = :genreId
    """)
    Page<Movie> findByTitleAndGenre(
            @Param("title") String title,
            @Param("genreId") Long genreId,
            Pageable pageable
    );

}

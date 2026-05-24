package com.example.movie.repository;

import com.example.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Optional<Movie> findByTmdbId(Long tmdbId);

    @EntityGraph(attributePaths = {"genres", "companies", "countries"})
    @Query("SELECT m FROM Movie m WHERE m.id = :id")
    Optional<Movie> findDetailById(@Param("id") Long id);

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

    @Query("""
        SELECT m FROM Movie m
        WHERE m.collectionId = :collectionId
        ORDER BY
            CASE WHEN m.releaseDate IS NULL THEN 1 ELSE 0 END,
            m.releaseDate ASC,
            m.id ASC
    """)
    List<Movie> findByCollectionIdOrderByReleaseDateAsc(@Param("collectionId") Long collectionId);

}

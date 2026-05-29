package com.example.movie.repository;

import com.example.movie.entity.Genre;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByTmdbIdAndMediaType(Long tmdbId, String mediaType);

    List<Genre> findByMediaTypeOrderByNameAsc(String mediaType);
}

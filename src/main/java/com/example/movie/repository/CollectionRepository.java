package com.example.movie.repository;

import com.example.movie.entity.Collection;
import com.example.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Integer> {
    Optional<Collection> findByTmdbId(Long tmdbId);
}
package com.example.movie.repository;

import com.example.movie.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByTmdbId(Long tmdbId);
    Page<Person> findByNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCase(
            String name,
            String originalName,
            Pageable pageable
    );
}

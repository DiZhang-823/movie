package com.example.movie.repository;

import com.example.movie.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {
    Optional<Person> findByTmdbId(Long tmdbId);
}
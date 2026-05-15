package com.example.movie.repository;

import com.example.movie.entity.Company;
import com.example.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Integer> {
    Optional<Company> findByTmdbId(Long tmdbId);
}
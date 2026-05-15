package com.example.movie.repository;

import com.example.movie.entity.ImportProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportProgressRepository extends JpaRepository<ImportProgress, Long> {

    Optional<ImportProgress> findByTaskName(String taskName);
}

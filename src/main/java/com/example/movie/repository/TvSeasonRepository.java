package com.example.movie.repository;

import com.example.movie.entity.TvSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TvSeasonRepository extends JpaRepository<TvSeason, Long>, JpaSpecificationExecutor<TvSeason> {
    Optional<TvSeason> findByTmdbTvIdAndSeasonNumber(Long tmdbTvId, Integer seasonNumber);
    List<TvSeason> findByTmdbTvIdOrderBySeasonNumberAsc(Long tmdbTvId);

    @EntityGraph(attributePaths = {"genres", "countries"})
    @Query("SELECT ts FROM TvSeason ts WHERE ts.id = :id")
    Optional<TvSeason> findDetailById(@Param("id") Long id);
}

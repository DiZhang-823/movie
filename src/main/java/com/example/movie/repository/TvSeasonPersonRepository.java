package com.example.movie.repository;

import com.example.movie.entity.TvSeason;
import com.example.movie.entity.TvSeasonPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TvSeasonPersonRepository extends JpaRepository<TvSeasonPerson, Long> {
    List<TvSeasonPerson> findByTvSeasonOrderBySortOrderAsc(TvSeason tvSeason);

    @Query("""
        SELECT tsp FROM TvSeasonPerson tsp
        JOIN FETCH tsp.tvSeason
        WHERE tsp.person.id = :personId
        ORDER BY
            CASE WHEN tsp.tvSeason.airDate IS NULL THEN 1 ELSE 0 END,
            tsp.tvSeason.airDate DESC,
            tsp.tvSeason.id DESC,
            tsp.sortOrder ASC
    """)
    List<TvSeasonPerson> findByPersonIdWithTvSeasonOrderByAirDateDesc(@Param("personId") Long personId);

    void deleteByTvSeason(TvSeason tvSeason);
}

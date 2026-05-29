package com.example.movie.repository;

import com.example.movie.entity.TvSeason;
import com.example.movie.entity.TvSeasonPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TvSeasonPersonRepository extends JpaRepository<TvSeasonPerson, Long> {
    List<TvSeasonPerson> findByTvSeasonOrderBySortOrderAsc(TvSeason tvSeason);

    void deleteByTvSeason(TvSeason tvSeason);
}

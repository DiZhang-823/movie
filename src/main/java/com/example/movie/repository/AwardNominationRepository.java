package com.example.movie.repository;

import com.example.movie.entity.AwardNomination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AwardNominationRepository extends JpaRepository<AwardNomination, Long> {
    List<AwardNomination> findByAwardCategoryIdOrderBySortOrderAscIdAsc(Long awardCategoryId);
    List<AwardNomination> findByMovieIdOrderByAwardCategoryAwardEditionAwardYearDesc(Long movieId);
    List<AwardNomination> findByTvSeasonIdOrderByAwardCategoryAwardEditionAwardYearDesc(Long tvSeasonId);
    List<AwardNomination> findByPersonIdOrderByAwardCategoryAwardEditionAwardYearDesc(Long personId);
}

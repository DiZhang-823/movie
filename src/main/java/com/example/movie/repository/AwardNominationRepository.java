package com.example.movie.repository;

import com.example.movie.entity.AwardNomination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AwardNominationRepository extends JpaRepository<AwardNomination, Long> {
    List<AwardNomination> findByAwardCategoryIdOrderBySortOrderAscIdAsc(Long awardCategoryId);
    List<AwardNomination> findByAwardCategoryIdAndSortOrderGreaterThanOrderBySortOrderAscIdAsc(
            Long awardCategoryId,
            Integer sortOrder
    );
    List<AwardNomination> findByMovieIdOrderByAwardCategoryAwardEditionAwardYearDesc(Long movieId);
    List<AwardNomination> findByTvSeasonIdOrderByAwardCategoryAwardEditionAwardYearDesc(Long tvSeasonId);
    List<AwardNomination> findByPersonIdOrderByAwardCategoryAwardEditionAwardYearDesc(Long personId);

    @Modifying
    @Query("DELETE FROM AwardNomination an WHERE an.awardCategory.awardEdition.awardBody.id = :awardBodyId")
    void deleteByAwardBodyId(@Param("awardBodyId") Long awardBodyId);

    @Modifying
    @Query("DELETE FROM AwardNomination an WHERE an.awardCategory.awardEdition.id = :awardEditionId")
    void deleteByAwardEditionId(@Param("awardEditionId") Long awardEditionId);

    @Modifying
    @Query("DELETE FROM AwardNomination an WHERE an.awardCategory.id = :awardCategoryId")
    void deleteByAwardCategoryId(@Param("awardCategoryId") Long awardCategoryId);
}

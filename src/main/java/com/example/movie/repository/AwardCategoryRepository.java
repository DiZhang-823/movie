package com.example.movie.repository;

import com.example.movie.entity.AwardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AwardCategoryRepository extends JpaRepository<AwardCategory, Long> {
    Optional<AwardCategory> findByAwardEditionIdAndName(Long awardEditionId, String name);
    List<AwardCategory> findByAwardEditionIdOrderBySortOrderAscIdAsc(Long awardEditionId);
    List<AwardCategory> findByAwardEditionIdAndSortOrderGreaterThanEqualOrderBySortOrderDesc(
            Long awardEditionId,
            Integer sortOrder
    );
    List<AwardCategory> findByAwardEditionIdAndSortOrderGreaterThanOrderBySortOrderAscIdAsc(
            Long awardEditionId,
            Integer sortOrder
    );

    @Modifying
    @Query("DELETE FROM AwardCategory ac WHERE ac.awardEdition.awardBody.id = :awardBodyId")
    void deleteByAwardBodyId(@Param("awardBodyId") Long awardBodyId);

    @Modifying
    @Query("DELETE FROM AwardCategory ac WHERE ac.awardEdition.id = :awardEditionId")
    void deleteByAwardEditionId(@Param("awardEditionId") Long awardEditionId);
}

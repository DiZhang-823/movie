package com.example.movie.repository;

import com.example.movie.entity.AwardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AwardCategoryRepository extends JpaRepository<AwardCategory, Long> {
    Optional<AwardCategory> findByAwardEditionIdAndName(Long awardEditionId, String name);
    List<AwardCategory> findByAwardEditionIdOrderBySortOrderAscIdAsc(Long awardEditionId);
    List<AwardCategory> findByAwardEditionIdAndSortOrderGreaterThanEqualOrderBySortOrderDesc(
            Long awardEditionId,
            Integer sortOrder
    );
}

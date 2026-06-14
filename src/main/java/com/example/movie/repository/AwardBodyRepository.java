package com.example.movie.repository;

import com.example.movie.entity.AwardBody;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AwardBodyRepository extends JpaRepository<AwardBody, Long> {
    Optional<AwardBody> findByName(String name);
    List<AwardBody> findAllByOrderBySortOrderAscIdAsc();
    List<AwardBody> findBySortOrderGreaterThanEqualOrderBySortOrderDesc(Integer sortOrder);
}

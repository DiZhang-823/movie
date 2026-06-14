package com.example.movie.repository;

import com.example.movie.entity.AwardEdition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AwardEditionRepository extends JpaRepository<AwardEdition, Long> {
    Optional<AwardEdition> findByAwardBodyIdAndEditionNumber(Long awardBodyId, Integer editionNumber);
    List<AwardEdition> findByAwardBodyIdOrderByEditionNumberDesc(Long awardBodyId);
    List<AwardEdition> findAllByOrderByAwardYearDescEditionNumberDescIdDesc();
}

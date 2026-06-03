package com.example.movie.repository;

import com.example.movie.entity.TvSeason;
import com.example.movie.entity.User;
import com.example.movie.entity.UserTvSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTvSeasonRepository extends JpaRepository<UserTvSeason, Long> {
    Optional<UserTvSeason> findByUserAndTvSeason(User user, TvSeason tvSeason);

    void deleteByTvSeason(TvSeason tvSeason);

    @Query("""
            SELECT uts FROM UserTvSeason uts
            JOIN FETCH uts.tvSeason
            WHERE uts.user.id = :userId
            AND uts.tvSeason.id IN :tvSeasonIds
            """)
    List<UserTvSeason> findByUserIdAndTvSeasonIds(
            @Param("userId") Long userId,
            @Param("tvSeasonIds") List<Long> tvSeasonIds);
}

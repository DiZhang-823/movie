package com.example.movie.service;

import com.example.movie.entity.TvSeason;
import com.example.movie.entity.User;
import com.example.movie.entity.UserTvSeason;
import com.example.movie.enums.UserMovieStatus;
import com.example.movie.repository.UserTvSeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserTvSeasonService {

    private final UserTvSeasonRepository userTvSeasonRepository;

    public UserTvSeasonService(UserTvSeasonRepository userTvSeasonRepository) {
        this.userTvSeasonRepository = userTvSeasonRepository;
    }

    @Transactional
    public UserTvSeason saveUserTvSeason(
            User user,
            TvSeason tvSeason,
            UserMovieStatus status,
            Byte rating,
            LocalDate watchedDate) {
        UserTvSeason userTvSeason = userTvSeasonRepository.findByUserAndTvSeason(user, tvSeason)
                .orElseGet(UserTvSeason::new);

        userTvSeason.setUser(user);
        userTvSeason.setTvSeason(tvSeason);
        userTvSeason.setStatus(status);

        if (status == UserMovieStatus.WATCHED) {
            if (rating != null && (rating < 1 || rating > 5)) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }

            userTvSeason.setRating(rating);
            userTvSeason.setWatchedDate(watchedDate != null ? watchedDate : LocalDate.now());
        } else {
            userTvSeason.setRating(null);
            userTvSeason.setWatchedDate(null);
        }

        return userTvSeasonRepository.save(userTvSeason);
    }

    @Transactional(readOnly = true)
    public UserTvSeason findUserTvSeason(User user, TvSeason tvSeason) {
        return userTvSeasonRepository.findByUserAndTvSeason(user, tvSeason).orElse(null);
    }
}

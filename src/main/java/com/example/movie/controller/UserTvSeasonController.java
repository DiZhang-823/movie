package com.example.movie.controller;

import com.example.movie.dto.UserTvSeasonRequest;
import com.example.movie.dto.UserTvSeasonResponse;
import com.example.movie.entity.TvSeason;
import com.example.movie.entity.User;
import com.example.movie.entity.UserTvSeason;
import com.example.movie.repository.TvSeasonRepository;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.UserTvSeasonService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-tv-seasons")
@Tag(name = "User-TV-Season API", description = "")
public class UserTvSeasonController {

    private final UserTvSeasonService userTvSeasonService;
    private final UserRepository userRepository;
    private final TvSeasonRepository tvSeasonRepository;

    public UserTvSeasonController(
            UserTvSeasonService userTvSeasonService,
            UserRepository userRepository,
            TvSeasonRepository tvSeasonRepository
    ) {
        this.userTvSeasonService = userTvSeasonService;
        this.userRepository = userRepository;
        this.tvSeasonRepository = tvSeasonRepository;
    }

    @PostMapping("/{tvSeasonId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> saveUserTvSeason(
            @PathVariable Long tvSeasonId,
            @RequestBody UserTvSeasonRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TvSeason tvSeason = tvSeasonRepository.findById(tvSeasonId)
                .orElseThrow(() -> new RuntimeException("TV season not found"));

        userTvSeasonService.saveUserTvSeason(
                user,
                tvSeason,
                request.getStatus(),
                request.getRating(),
                request.getWatchedDate()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{tvSeasonId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserTvSeasonResponse> getUserTvSeason(
            @PathVariable Long tvSeasonId,
            Authentication authentication
    ) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TvSeason tvSeason = tvSeasonRepository.findById(tvSeasonId)
                .orElseThrow(() -> new RuntimeException("TV season not found"));

        UserTvSeason userTvSeason = userTvSeasonService.findUserTvSeason(user, tvSeason);
        if (userTvSeason == null) {
            return ResponseEntity.noContent().build();
        }

        UserTvSeasonResponse response = new UserTvSeasonResponse();
        response.setTvSeasonId(tvSeason.getId());
        response.setStatus(userTvSeason.getStatus());
        response.setRating(userTvSeason.getRating());
        response.setWatchedDate(userTvSeason.getWatchedDate());
        response.setUpdatedAt(userTvSeason.getUpdatedAt());

        return ResponseEntity.ok(response);
    }
}

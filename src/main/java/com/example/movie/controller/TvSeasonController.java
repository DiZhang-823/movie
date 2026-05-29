package com.example.movie.controller;

import com.example.movie.dto.*;
import com.example.movie.entity.User;
import com.example.movie.enums.UserMovieStatusFilter;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.TvSeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tv-seasons")
@Tag(name = "TV Season API", description = "Operations for managing TV seasons")
public class TvSeasonController {

    private final TvSeasonService tvSeasonService;
    private final UserRepository userRepository;

    public TvSeasonController(TvSeasonService tvSeasonService, UserRepository userRepository) {
        this.tvSeasonService = tvSeasonService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "getTvSeasons: Get TV seasons with pagination and filters")
    @GetMapping
    public ResponseEntity<PageResponse<TvSeasonResponse>> getTvSeasons(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) UserMovieStatusFilter userTvSeasonStatus,
            @RequestParam(required = false) Integer userRatingMin,
            @RequestParam(required = false) Integer userRatingMax,
            @RequestParam(required = false) Integer tvSeasonRatingMin,
            @RequestParam(required = false) Integer tvSeasonRatingMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "ID") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            org.springframework.security.core.Authentication authentication
    ) {
        Long currentUserId = null;
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());

        if (userTvSeasonStatus != null && !authenticated) {
            return ResponseEntity.status(401).build();
        }

        if (authenticated) {
            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            currentUserId = user.getId();
        }

        TvSeasonQuery tvSeasonQuery = new TvSeasonQuery();
        tvSeasonQuery.setTitle(title);
        tvSeasonQuery.setGenreId(genreId);
        tvSeasonQuery.setYear(year);
        tvSeasonQuery.setRegionCode(regionCode);
        tvSeasonQuery.setUserTvSeasonStatus(userTvSeasonStatus);
        tvSeasonQuery.setUserRatingMin(userRatingMin);
        tvSeasonQuery.setUserRatingMax(userRatingMax);
        tvSeasonQuery.setTvSeasonRatingMin(tvSeasonRatingMin);
        tvSeasonQuery.setTvSeasonRatingMax(tvSeasonRatingMax);

        return ResponseEntity.ok(tvSeasonService.getTvSeasons(
                tvSeasonQuery,
                currentUserId,
                page,
                size,
                sortBy,
                direction
        ));
    }

    @Operation(summary = "getTvSeasonById: Get TV season by id")
    @GetMapping("/{id}")
    public ResponseEntity<TvSeasonDetailResponse> getTvSeasonById(@PathVariable Long id) {
        return ResponseEntity.ok(tvSeasonService.getTvSeasonById(id));
    }
}

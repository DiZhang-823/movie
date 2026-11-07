package com.example.movie.controller;

import com.example.movie.dto.MovieDetailResponse;
import com.example.movie.dto.TvSeasonDetailResponse;
import com.example.movie.dto.UpdateTitleRequest;
import com.example.movie.dto.UpdateTvSeasonTitleRequest;
import com.example.movie.service.MovieService;
import com.example.movie.service.TvSeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Content API", description = "Admin operations for content cleanup")
public class AdminContentController {

    private final MovieService movieService;
    private final TvSeasonService tvSeasonService;

    public AdminContentController(MovieService movieService, TvSeasonService tvSeasonService) {
        this.movieService = movieService;
        this.tvSeasonService = tvSeasonService;
    }

    @Operation(summary = "deleteMovie: Permanently delete a movie and related records")
    @DeleteMapping("/movies/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "updateMovieTitle: Update a movie title")
    @PutMapping("/movies/{id}/title")
    public ResponseEntity<MovieDetailResponse> updateMovieTitle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTitleRequest request
    ) {
        return ResponseEntity.ok(movieService.updateMovieTitle(id, request.getTitle()));
    }

    @Operation(summary = "deleteTvSeason: Permanently delete a TV season and related records")
    @DeleteMapping("/tv-seasons/{id}")
    public ResponseEntity<Void> deleteTvSeason(@PathVariable Long id) {
        tvSeasonService.deleteTvSeason(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "updateTvSeasonTitle: Update a TV season display title")
    @PutMapping("/tv-seasons/{id}/title")
    public ResponseEntity<TvSeasonDetailResponse> updateTvSeasonTitle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTvSeasonTitleRequest request
    ) {
        return ResponseEntity.ok(tvSeasonService.updateTvSeasonTitle(id, request.getTitle(), request.getShowName()));
    }
}

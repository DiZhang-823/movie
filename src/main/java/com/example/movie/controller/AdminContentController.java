package com.example.movie.controller;

import com.example.movie.service.MovieService;
import com.example.movie.service.TvSeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(summary = "deleteTvSeason: Permanently delete a TV season and related records")
    @DeleteMapping("/tv-seasons/{id}")
    public ResponseEntity<Void> deleteTvSeason(@PathVariable Long id) {
        tvSeasonService.deleteTvSeason(id);
        return ResponseEntity.noContent().build();
    }
}

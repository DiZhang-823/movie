package com.example.movie.controller;

import com.example.movie.dto.*;
import com.example.movie.entity.User;
import com.example.movie.enums.UserMovieStatusFilter;
import com.example.movie.exception.ResourceNotFoundException;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.MovieService;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import com.example.movie.tmdb.dto.TmdbPersonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movie API", description = "Operations for managing movies")
public class MovieController {

    private final MovieService movieService;
    private final UserRepository userRepository;

    public MovieController(MovieService movieService, UserRepository userRepository) {
        this.movieService = movieService;
        this.userRepository = userRepository;
    }

    // 直接看 TMDB 返回结果，不保存数据库
    @GetMapping("/tmdb/get/{movieId}")
    public TmdbMovieResponse getMovieByIdFromTmdb(@PathVariable Long movieId) {
        return movieService.fetchMovieByIdFromTmdb(movieId);
    }

    // 从 TMDB 拉取并保存到本地数据库
    @PostMapping("/tmdb/sync/{movieId}")
    public ResponseEntity<TmdbMovieResponse> syncMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(movieService.syncMovie(movieId));
    }

    @Operation(summary = "getMovies: Get movies with pagination and optional title search")
    @GetMapping
    public ResponseEntity<PageResponse<MovieResponse>> getMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) UserMovieStatusFilter userMovieStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "ID") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            org.springframework.security.core.Authentication authentication

    ) {
        Long currentUserId = null;
        if (userMovieStatus != null) {
            if (authentication == null || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).build();
            }

            User user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            currentUserId = user.getId();
        }

        MovieQuery movieQuery = new MovieQuery();
        movieQuery.setTitle(title);
        movieQuery.setGenreId(genreId);
        movieQuery.setYear(year);
        movieQuery.setRegionCode(regionCode);
        movieQuery.setUserMovieStatus(userMovieStatus);
        return ResponseEntity.ok(movieService.getMovies(movieQuery, currentUserId, page, size, sortBy, direction));
    }

//    @Operation(summary = "addMovie: Add a new movie")
//    @PostMapping
//    public ResponseEntity<MovieResponse> addMovie(@Valid @RequestBody MovieRequest request) {
//        MovieResponse savedMovie = movieService.addMovie(request);
//        return ResponseEntity.status(201).body(savedMovie);
//    }

    @Operation(summary = "getMovieById: Get movie by id")
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieById(@PathVariable Long id) {
        MovieDetailResponse movie = movieService.getMovieById(id);
        if (movie == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(movie);
    }

//    @Operation(summary = "deleteMovie: Delete a movie by id")
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
//        movieService.deleteMovie(id);
//        return ResponseEntity.noContent().build();
//    }

//    @Operation(summary = "updateMovie: Update a movie by id")
//    @PutMapping("/{id}")
//    public ResponseEntity<MovieResponse> updateMovie(@Valid @RequestBody MovieRequest request, @PathVariable Long id) {
//        MovieResponse updatedMovie = movieService.updateMovie(id, request);
//        if (updatedMovie == null) {
//            return ResponseEntity.notFound().build();
//        }
//        return ResponseEntity.ok(updatedMovie);
//    }
}

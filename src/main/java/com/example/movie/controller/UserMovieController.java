package com.example.movie.controller;

import com.example.movie.dto.UserMovieRequest;
import com.example.movie.dto.UserMovieResponse;
import com.example.movie.entity.Movie;
import com.example.movie.entity.User;
import com.example.movie.entity.UserMovie;
import com.example.movie.repository.MovieRepository;
import com.example.movie.repository.UserRepository;
import com.example.movie.service.UserMovieService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-movies")
@Tag(name = "User-Movie API", description = "")
public class UserMovieController {

    private final UserMovieService userMovieService;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public UserMovieController(
            UserMovieService userMovieService,
            UserRepository userRepository,
            MovieRepository movieRepository
    ) {
        this.userMovieService = userMovieService;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @PostMapping("/{movieId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> saveUserMovie(
            @PathVariable Long movieId,
            @RequestBody UserMovieRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        userMovieService.saveUserMovie(
                user,
                movie,
                request.getStatus(),
                request.getRating(),
                request.getWatchedDate()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{movieId}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserMovieResponse> getUserMovie(
            @PathVariable Long movieId,
            Authentication authentication
    ) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        UserMovie userMovie = userMovieService.findUserMovie(user, movie);

        if (userMovie == null) {
            return ResponseEntity.noContent().build();
        }

        UserMovieResponse response = new UserMovieResponse();
        response.setMovieId(movie.getId());
        response.setStatus(userMovie.getStatus());
        response.setRating(userMovie.getRating());
        response.setWatchedDate(userMovie.getWatchedDate());
        response.setUpdatedAt(userMovie.getUpdatedAt());

        return ResponseEntity.ok(response);
    }
}
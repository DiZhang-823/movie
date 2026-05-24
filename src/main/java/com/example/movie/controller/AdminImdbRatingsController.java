package com.example.movie.controller;

import com.example.movie.dto.ImdbRatingsSyncResponse;
import com.example.movie.service.ImdbRatingsSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/imdb-ratings")
public class AdminImdbRatingsController {

    private final ImdbRatingsSyncService imdbRatingsSyncService;

    public AdminImdbRatingsController(ImdbRatingsSyncService imdbRatingsSyncService) {
        this.imdbRatingsSyncService = imdbRatingsSyncService;
    }

    @PostMapping("/sync")
    public ImdbRatingsSyncResponse sync() {
        return imdbRatingsSyncService.syncRatings();
    }
}

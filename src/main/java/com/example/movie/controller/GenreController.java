package com.example.movie.controller;

import com.example.movie.entity.Genre;
import com.example.movie.service.GenreService;
import com.example.movie.tmdb.dto.TmdbGenreDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@Tag(name = "Genres API", description = "Operations for managing movie genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    // 直接看 TMDB 返回结果，不保存数据库
    @GetMapping("/tmdb")

    public List<TmdbGenreDto> getGenresFromTmdb() {
        return genreService.fetchMovieGenresFromTmdb();
    }

    // 从 TMDB 拉取并保存到本地数据库
    @PostMapping("/sync")
    public List<Genre> syncGenres() {
        return genreService.syncMovieGenres();
    }

    // 查询本地数据库中的 genre
    @GetMapping
    @Operation(summary = "getLocalGenres: Get movie genres from local database.")
    public List<Genre> getLocalGenres(@RequestParam(required = false) String mediaType) {
        return genreService.getLocalGenresByMediaType(mediaType);
    }
}

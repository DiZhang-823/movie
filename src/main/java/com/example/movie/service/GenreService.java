package com.example.movie.service;

import com.example.movie.entity.Genre;
import com.example.movie.repository.GenreRepository;
import com.example.movie.tmdb.TmdbClient;
import com.example.movie.tmdb.dto.TmdbGenreDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GenreService {

    private final TmdbClient tmdbClient;
    private final GenreRepository genreRepository;

    public GenreService(TmdbClient tmdbClient, GenreRepository genreRepository) {
        this.tmdbClient = tmdbClient;
        this.genreRepository = genreRepository;
    }

    public List<TmdbGenreDto> fetchMovieGenresFromTmdb() {
        return tmdbClient.getMovieGenres();
    }

    @Transactional
    public List<Genre> syncMovieGenres() {
        List<TmdbGenreDto> tmdbGenres = fetchMovieGenresFromTmdb();
        List<Genre> savedGenres = new ArrayList<>();

        for (TmdbGenreDto dto : tmdbGenres) {
            Genre genre = genreRepository.findByTmdbId(dto.getId().longValue()).orElseGet(Genre::new);

            genre.setTmdbId(dto.getId().longValue());
            genre.setName(dto.getName());

            savedGenres.add(genreRepository.save(genre));
        }

        return savedGenres;
    }

    public List<Genre> getAllLocalGenres() {
        return genreRepository.findAll();
    }
}
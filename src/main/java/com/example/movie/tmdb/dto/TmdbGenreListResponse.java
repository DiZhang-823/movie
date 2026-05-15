package com.example.movie.tmdb.dto;

import java.util.List;

public class TmdbGenreListResponse {

    private List<TmdbGenreDto> genres;

    public List<TmdbGenreDto> getGenres() {
        return genres;
    }

    public void setGenres(List<TmdbGenreDto> genres) {
        this.genres = genres;
    }
}
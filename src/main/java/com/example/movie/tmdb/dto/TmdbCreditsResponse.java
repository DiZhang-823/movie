package com.example.movie.tmdb.dto;

import lombok.Data;
import java.util.List;

@Data
public class TmdbCreditsResponse {

    private Long id;

    private List<Cast> cast;

    private List<Crew> crew;
}
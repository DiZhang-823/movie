package com.example.movie.tmdb;

import com.example.movie.tmdb.config.TmdbProperties;
//import com.example.movie.tmdb.dto.TmdbMovieDetailResponse;
//import com.example.movie.tmdb.dto.TmdbSearchMovieResponse;
import com.example.movie.tmdb.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class TmdbClient {

    private final RestClient restClient;
    private final TmdbProperties tmdbProperties;

    public TmdbClient(TmdbProperties tmdbProperties) {
        this.tmdbProperties = tmdbProperties;
        this.restClient = RestClient.builder()
                .baseUrl(tmdbProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbProperties.getBearerTokenV4())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    public List<TmdbGenreDto> getMovieGenres() {
        TmdbGenreListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/movie/list")
                        .queryParam("language", tmdbProperties.getLanguage())
                        .build()
                )
                .retrieve()
                .body(TmdbGenreListResponse.class);

        if (response == null || response.getGenres() == null) {
            return List.of();
        }

        return response.getGenres();
    }

    /**
     * 获取人物详情
     */
    public TmdbPersonResponse getPersonById(Long personId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{person_id}")
                        .queryParam("language", "zh-CN")
                        .build(personId)
                )
                .retrieve()
                .body(TmdbPersonResponse.class);
    }

    public TmdbMovieResponse getMovieById(Long movieId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movie_id}")
                        .queryParam("language", "zh-CN")
                        .build(movieId)
                )
                .retrieve()
                .body(TmdbMovieResponse.class);
    }

    public TmdbCreditsResponse getCreditsById(Long movieId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movie_id}/credits")
                        .queryParam("language", "zh-CN")
                        .build(movieId)
                )
                .retrieve()
                .body(TmdbCreditsResponse.class);
    }


//    public TmdbMovieDetailResponse getMovieDetail(Long movieId) {
//        String uri = UriComponentsBuilder
//                .fromPath("/movie/{movieId}")
//                .queryParam("language", tmdbProperties.getLanguage())
//                .queryParam("append_to_response", "credits")
//                .buildAndExpand(movieId)
//                .toUriString();
//
//        return restClient.get()
//                .uri(uri)
//                .retrieve()
//                .body(TmdbMovieDetailResponse.class);
//    }
//
//    public TmdbMovieDetailResponse getMovieDetailWithExtras(Long movieId) {
//        String uri = UriComponentsBuilder
//                .fromPath("/movie/{movieId}")
//                .queryParam("language", tmdbProperties.getLanguage())
//                .queryParam("append_to_response", "credits,videos,images,external_ids")
//                .buildAndExpand(movieId)
//                .toUriString();
//
//        return restClient.get()
//                .uri(uri)
//                .retrieve()
//                .body(TmdbMovieDetailResponse.class);
//    }
}
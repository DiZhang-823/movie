package com.example.movie.tmdb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {

    private String baseUrl;
    private String bearerTokenV3;
    private String bearerTokenV4;
    private String language;

}
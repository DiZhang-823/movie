package com.example.movie.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Cast {

    private Long id;

    private String name;

    @JsonProperty("original_name")
    private String originalName;

    private Byte gender;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    @JsonProperty("profile_path")
    private String profilePath;

    private BigDecimal popularity;

    // 🎬 关键字段
    private String character;

    private Integer order;

    // 一般不用，但建议保留（防止以后用）
    @JsonProperty("cast_id")
    private Integer castId;

    @JsonProperty("credit_id")
    private String creditId;

    private Boolean adult;
}
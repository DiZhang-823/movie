package com.example.movie.enums;

import lombok.Getter;

@Getter
public enum TvSeasonSortField {
    ID("id"),
    AIR_DATE("airDate"),
    VOTE_COUNT("voteCount"),
    VOTE_AVERAGE("voteAverage"),
    CREATED_AT("createdAt");

    private final String field;

    TvSeasonSortField(String field) {
        this.field = field;
    }
}

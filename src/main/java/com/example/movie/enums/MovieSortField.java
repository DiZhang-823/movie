package com.example.movie.enums;

import lombok.Getter;

@Getter
public enum MovieSortField {
    ID("id"),
    RELEASE_DATE("releaseDate"),
    VOTE_COUNT("voteCount"),
    VOTE_AVERAGE("voteAverage"),
    CREATED_AT("createdAt");

    private final String field;

    MovieSortField(String field) {
        this.field = field;
    }

    public static MovieSortField from(String field) {
        for (MovieSortField f : values()) {
            if (f.field.equalsIgnoreCase(field)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Invalid sort field: " + field);
    }
}


package com.example.movie.specification;

import com.example.movie.entity.Country;
import com.example.movie.entity.Movie;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;

public class MovieSpecifications {

    public static Specification<Movie> titleContains(String title) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                );
    }

    public static Specification<Movie> hasGenre(Long genreId) {
        return (root, query, cb) -> {
            assert query != null;
            query.distinct(true);
            return cb.equal(
                    root.join("genres", JoinType.INNER).get("id"),
                    genreId
            );
        };
    }

    public static Specification<Movie> hasReleaseYear(Integer year) {
        return (root, query, cb) -> {
            if (year == null) {
                return cb.conjunction();
            }
            return cb.equal(cb.function("year", Integer.class, root.get("releaseDate")), year);
        };
    }

    public static Specification<Movie> hasRegion(String regionCode) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Movie, Country> countryJoin = root.join("countries");

            if ("OTHER".equals(regionCode)) {
                return cb.not(countryJoin.get("code").in(
                        "CN", "HK", "TW", "US", "GB", "JP", "KR",
                        "FR", "DE", "IT", "ES", "IN", "RU", "BR", "TH"
                ));
            }

            return cb.equal(countryJoin.get("code"), regionCode);
        };
    }
}
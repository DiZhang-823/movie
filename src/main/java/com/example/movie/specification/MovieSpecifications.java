package com.example.movie.specification;

import com.example.movie.entity.Country;
import com.example.movie.entity.Movie;
import com.example.movie.entity.UserMovie;
import com.example.movie.enums.UserMovieStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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

    public static Specification<Movie> hasUserMovieStatus(Long userId, UserMovieStatus status) {
        return (root, query, cb) -> {
            assert query != null;

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserMovie> userMovieRoot = subquery.from(UserMovie.class);

            subquery.select(userMovieRoot.get("movie").get("id"))
                    .where(
                            cb.equal(userMovieRoot.get("user").get("id"), userId),
                            cb.equal(userMovieRoot.get("status"), status)
                    );

            return root.get("id").in(subquery);
        };
    }

    public static Specification<Movie> hasNoUserMovieMark(Long userId) {
        return (root, query, cb) -> {
            assert query != null;

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserMovie> userMovieRoot = subquery.from(UserMovie.class);

            subquery.select(userMovieRoot.get("movie").get("id"))
                    .where(cb.equal(userMovieRoot.get("user").get("id"), userId));

            return cb.not(root.get("id").in(subquery));
        };
    }
}

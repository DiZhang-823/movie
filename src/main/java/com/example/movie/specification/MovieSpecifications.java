package com.example.movie.specification;

import com.example.movie.entity.Country;
import com.example.movie.entity.Genre;
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

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Movie> movieRoot = subquery.from(Movie.class);
            Join<Movie, Genre> genreJoin = movieRoot.join("genres", JoinType.INNER);

            subquery.select(movieRoot.get("id"))
                    .where(
                            cb.equal(movieRoot.get("id"), root.get("id")),
                            cb.equal(genreJoin.get("id"), genreId)
                    );

            return cb.exists(subquery);
        };
    }

    public static Specification<Movie> hasUserMovieStatusOrderByUserTime(Long userId, UserMovieStatus status) {
        return (root, query, cb) -> {
            assert query != null;

            Root<UserMovie> userMovieRoot = query.from(UserMovie.class);

            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                if (status == UserMovieStatus.WATCHED) {
                    query.orderBy(
                            cb.desc(userMovieRoot.get("watchedDate")),
                            cb.desc(root.get("releaseDate")),
                            cb.desc(root.get("id"))
                    );
                } else {
                    query.orderBy(
                            cb.desc(userMovieRoot.get("updatedAt")),
                            cb.desc(root.get("id"))
                    );
                }
            }

            return cb.and(
                    cb.equal(userMovieRoot.get("movie").get("id"), root.get("id")),
                    cb.equal(userMovieRoot.get("user").get("id"), userId),
                    cb.equal(userMovieRoot.get("status"), status)
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
            assert query != null;

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Movie> movieRoot = subquery.from(Movie.class);
            Join<Movie, Country> countryJoin = movieRoot.join("countries", JoinType.INNER);

            if ("OTHER".equals(regionCode)) {
                subquery.select(movieRoot.get("id"))
                        .where(
                                cb.equal(movieRoot.get("id"), root.get("id")),
                                cb.not(countryJoin.get("code").in(
                                        "CN", "HK", "TW", "US", "GB", "JP", "KR",
                                        "FR", "DE", "IT", "ES", "IN", "RU", "BR", "TH"
                                ))
                        );
                return cb.exists(subquery);
            }

            subquery.select(movieRoot.get("id"))
                    .where(
                            cb.equal(movieRoot.get("id"), root.get("id")),
                            cb.equal(countryJoin.get("code"), regionCode)
                    );

            return cb.exists(subquery);
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

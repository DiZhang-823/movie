package com.example.movie.specification;

import com.example.movie.entity.*;
import com.example.movie.enums.UserMovieStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TvSeasonSpecifications {

    public static Specification<TvSeason> titleContains(String title) {
        return (root, query, cb) -> {
            String pattern = "%" + title.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("originalTitle")), pattern),
                    cb.like(cb.lower(root.get("showName")), pattern),
                    cb.like(cb.lower(root.get("originalShowName")), pattern)
            );
        };
    }

    public static Specification<TvSeason> hasGenre(Long genreId) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<TvSeason> tvSeasonRoot = subquery.from(TvSeason.class);
            Join<TvSeason, Genre> genreJoin = tvSeasonRoot.join("genres", JoinType.INNER);

            subquery.select(tvSeasonRoot.get("id"))
                    .where(
                            cb.equal(tvSeasonRoot.get("id"), root.get("id")),
                            cb.equal(genreJoin.get("id"), genreId)
                    );

            return cb.exists(subquery);
        };
    }

    public static Specification<TvSeason> hasYear(Integer year) {
        return (root, query, cb) -> year == null ? cb.conjunction() : cb.equal(root.get("year"), year);
    }

    public static Specification<TvSeason> hasRegion(String regionCode) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<TvSeason> tvSeasonRoot = subquery.from(TvSeason.class);
            Join<TvSeason, Country> countryJoin = tvSeasonRoot.join("countries", JoinType.INNER);

            if ("OTHER".equals(regionCode)) {
                subquery.select(tvSeasonRoot.get("id"))
                        .where(
                                cb.equal(tvSeasonRoot.get("id"), root.get("id")),
                                cb.not(countryJoin.get("code").in(
                                        "CN", "HK", "TW", "US", "GB", "JP", "KR",
                                        "FR", "DE", "IT", "ES", "IN", "RU", "BR", "TH"
                                ))
                        );
                return cb.exists(subquery);
            }

            subquery.select(tvSeasonRoot.get("id"))
                    .where(
                            cb.equal(tvSeasonRoot.get("id"), root.get("id")),
                            cb.equal(countryJoin.get("code"), regionCode)
                    );

            return cb.exists(subquery);
        };
    }

    public static Specification<TvSeason> hasDisplayVoteAverageBetween(Integer minRating, Integer maxRating) {
        return (root, query, cb) -> {
            Expression<BigDecimal> displayVoteAverage = cb.coalesce(
                    root.get("imdbVoteAverage"),
                    root.get("voteAverage")
            );

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(
                    cb.isNotNull(root.get("imdbVoteAverage")),
                    cb.isNotNull(root.get("voteAverage"))
            ));
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(displayVoteAverage, BigDecimal.valueOf(minRating)));
            }
            if (maxRating != null) {
                predicates.add(cb.lessThanOrEqualTo(displayVoteAverage, BigDecimal.valueOf(maxRating)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<TvSeason> orderByDisplayVoteAverage(Sort.Direction direction) {
        return (root, query, cb) -> {
            assert query != null;
            if (!isCountQuery(query.getResultType())) {
                Expression<BigDecimal> displayVoteAverage = cb.coalesce(
                        root.get("imdbVoteAverage"),
                        root.get("voteAverage")
                );
                query.orderBy(
                        toOrder(cb, displayVoteAverage, direction),
                        toOrder(cb, root.get("id"), direction)
                );
            }
            return cb.conjunction();
        };
    }

    public static Specification<TvSeason> orderByDisplayVoteCount(Sort.Direction direction) {
        return (root, query, cb) -> {
            assert query != null;
            if (!isCountQuery(query.getResultType())) {
                Expression<Integer> displayVoteCount = cb.coalesce(
                        root.get("imdbVoteCount"),
                        root.get("voteCount")
                );
                query.orderBy(
                        toOrder(cb, displayVoteCount, direction),
                        toOrder(cb, root.get("id"), direction)
                );
            }
            return cb.conjunction();
        };
    }

    public static Specification<TvSeason> hasUserTvSeasonStatusOrderByUserTime(
            Long userId,
            UserMovieStatus status,
            Integer minRating,
            Integer maxRating) {
        return (root, query, cb) -> {
            assert query != null;
            Root<UserTvSeason> userTvSeasonRoot = query.from(UserTvSeason.class);

            if (!isCountQuery(query.getResultType())) {
                if (status == UserMovieStatus.WATCHED) {
                    query.orderBy(
                            cb.desc(userTvSeasonRoot.get("watchedDate")),
                            cb.desc(root.get("airDate")),
                            cb.desc(root.get("id"))
                    );
                } else {
                    query.orderBy(
                            cb.desc(userTvSeasonRoot.get("updatedAt")),
                            cb.desc(root.get("id"))
                    );
                }
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(userTvSeasonRoot.get("tvSeason").get("id"), root.get("id")));
            predicates.add(cb.equal(userTvSeasonRoot.get("user").get("id"), userId));
            predicates.add(cb.equal(userTvSeasonRoot.get("status"), status));
            addUserRatingPredicates(predicates, userTvSeasonRoot, status, minRating, maxRating, cb);

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<TvSeason> hasUserTvSeasonStatus(
            Long userId,
            UserMovieStatus status,
            Integer minRating,
            Integer maxRating) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserTvSeason> userTvSeasonRoot = subquery.from(UserTvSeason.class);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(userTvSeasonRoot.get("user").get("id"), userId));
            predicates.add(cb.equal(userTvSeasonRoot.get("status"), status));
            addUserRatingPredicates(predicates, userTvSeasonRoot, status, minRating, maxRating, cb);

            subquery.select(userTvSeasonRoot.get("tvSeason").get("id"))
                    .where(cb.and(predicates.toArray(Predicate[]::new)));

            return root.get("id").in(subquery);
        };
    }

    public static Specification<TvSeason> hasNoUserTvSeasonMark(Long userId) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserTvSeason> userTvSeasonRoot = subquery.from(UserTvSeason.class);

            subquery.select(userTvSeasonRoot.get("tvSeason").get("id"))
                    .where(cb.equal(userTvSeasonRoot.get("user").get("id"), userId));

            return cb.not(root.get("id").in(subquery));
        };
    }

    public static Specification<TvSeason> doesNotHaveUserTvSeasonStatus(Long userId, UserMovieStatus status) {
        return (root, query, cb) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserTvSeason> userTvSeasonRoot = subquery.from(UserTvSeason.class);

            subquery.select(userTvSeasonRoot.get("tvSeason").get("id"))
                    .where(
                            cb.equal(userTvSeasonRoot.get("user").get("id"), userId),
                            cb.equal(userTvSeasonRoot.get("status"), status)
                    );

            return cb.not(root.get("id").in(subquery));
        };
    }

    private static void addUserRatingPredicates(
            List<Predicate> predicates,
            Root<UserTvSeason> userTvSeasonRoot,
            UserMovieStatus status,
            Integer minRating,
            Integer maxRating,
            CriteriaBuilder cb) {
        if (status != UserMovieStatus.WATCHED) {
            return;
        }
        if (minRating != null) {
            predicates.add(cb.greaterThanOrEqualTo(userTvSeasonRoot.get("rating").as(Integer.class), minRating));
        }
        if (maxRating != null) {
            predicates.add(cb.lessThanOrEqualTo(userTvSeasonRoot.get("rating").as(Integer.class), maxRating));
        }
    }

    private static boolean isCountQuery(Class<?> resultType) {
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }

    private static Order toOrder(CriteriaBuilder cb, Expression<?> expression, Sort.Direction direction) {
        return direction == Sort.Direction.ASC ? cb.asc(expression) : cb.desc(expression);
    }
}

package com.example.movie.service;

import com.example.movie.entity.*;
import com.example.movie.dto.*;
import com.example.movie.enums.TvSeasonSortField;
import com.example.movie.enums.UserMovieStatus;
import com.example.movie.enums.UserMovieStatusFilter;
import com.example.movie.exception.ResourceNotFoundException;
import com.example.movie.repository.*;
import com.example.movie.specification.TvSeasonSpecifications;
import com.example.movie.tmdb.TmdbClient;
import com.example.movie.tmdb.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TvSeasonService {

    private static final Set<String> IMPORTANT_JOBS = Set.of(
            "director",
            "writer",
            "producer"
    );

    private static final Map<String, String> COUNTRY_NAME_ZH_OVERRIDES = Map.ofEntries(
            Map.entry("AE", "阿联酋"),
            Map.entry("AR", "阿根廷"),
            Map.entry("AT", "奥地利"),
            Map.entry("AU", "澳大利亚"),
            Map.entry("BE", "比利时"),
            Map.entry("BR", "巴西"),
            Map.entry("CA", "加拿大"),
            Map.entry("CH", "瑞士"),
            Map.entry("CN", "中国大陆"),
            Map.entry("DE", "德国"),
            Map.entry("DK", "丹麦"),
            Map.entry("ES", "西班牙"),
            Map.entry("FI", "芬兰"),
            Map.entry("FR", "法国"),
            Map.entry("GB", "英国"),
            Map.entry("HK", "香港"),
            Map.entry("IN", "印度"),
            Map.entry("IT", "意大利"),
            Map.entry("JP", "日本"),
            Map.entry("KR", "韩国"),
            Map.entry("MX", "墨西哥"),
            Map.entry("NL", "荷兰"),
            Map.entry("NO", "挪威"),
            Map.entry("NZ", "新西兰"),
            Map.entry("SE", "瑞典"),
            Map.entry("SG", "新加坡"),
            Map.entry("TH", "泰国"),
            Map.entry("TR", "土耳其"),
            Map.entry("TW", "台湾"),
            Map.entry("US", "美国")
    );

    private final TmdbClient tmdbClient;
    private final TvSeasonRepository tvSeasonRepository;
    private final TvSeasonPersonRepository tvSeasonPersonRepository;
    private final GenreRepository genreRepository;
    private final CountryRepository countryRepository;
    private final PersonRepository personRepository;
    private final UserTvSeasonRepository userTvSeasonRepository;

    public TvSeasonService(
            TmdbClient tmdbClient,
            TvSeasonRepository tvSeasonRepository,
            TvSeasonPersonRepository tvSeasonPersonRepository,
            GenreRepository genreRepository,
            CountryRepository countryRepository,
            PersonRepository personRepository,
            UserTvSeasonRepository userTvSeasonRepository
    ) {
        this.tmdbClient = tmdbClient;
        this.tvSeasonRepository = tvSeasonRepository;
        this.tvSeasonPersonRepository = tvSeasonPersonRepository;
        this.genreRepository = genreRepository;
        this.countryRepository = countryRepository;
        this.personRepository = personRepository;
        this.userTvSeasonRepository = userTvSeasonRepository;
    }

    public PageResponse<TvSeasonResponse> getTvSeasons(
            TvSeasonQuery tvSeasonQuery,
            Long currentUserId,
            int page,
            int size,
            String sortBy,
            String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be less than 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        TvSeasonSortField requestedSortField = TvSeasonSortField.valueOf(sortBy);
        Sort.Direction requestedDirection = Sort.Direction.fromString(direction);
        boolean useUserTvSeasonDefaultSort = shouldUseUserTvSeasonDefaultSort(
                tvSeasonQuery,
                currentUserId,
                requestedSortField,
                requestedDirection
        );
        boolean useDisplayRatingSort = !useUserTvSeasonDefaultSort && shouldUseDisplayRatingSort(requestedSortField);

        Sort sort = useUserTvSeasonDefaultSort || useDisplayRatingSort
                ? Sort.unsorted()
                : Sort.by(requestedDirection, requestedSortField.getField());

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<TvSeason> spec = Specification.unrestricted();
        if (tvSeasonQuery.getTitle() != null && !tvSeasonQuery.getTitle().isBlank()) {
            spec = spec.and(TvSeasonSpecifications.titleContains(tvSeasonQuery.getTitle()));
        }
        if (tvSeasonQuery.getGenreId() != null) {
            spec = spec.and(TvSeasonSpecifications.hasGenre(tvSeasonQuery.getGenreId()));
        }
        if (tvSeasonQuery.getYear() != null) {
            spec = spec.and(TvSeasonSpecifications.hasYear(tvSeasonQuery.getYear()));
        }
        if (tvSeasonQuery.getRegionCode() != null && !tvSeasonQuery.getRegionCode().isBlank()) {
            spec = spec.and(TvSeasonSpecifications.hasRegion(tvSeasonQuery.getRegionCode()));
        }
        if (tvSeasonQuery.getTvSeasonRatingMin() != null || tvSeasonQuery.getTvSeasonRatingMax() != null) {
            spec = spec.and(TvSeasonSpecifications.hasDisplayVoteAverageBetween(
                    tvSeasonQuery.getTvSeasonRatingMin(),
                    tvSeasonQuery.getTvSeasonRatingMax()
            ));
        }
        if (tvSeasonQuery.getUserTvSeasonStatus() != null && currentUserId != null) {
            if (tvSeasonQuery.getUserTvSeasonStatus() == UserMovieStatusFilter.DEFAULT) {
                spec = spec.and(TvSeasonSpecifications.doesNotHaveUserTvSeasonStatus(
                        currentUserId,
                        UserMovieStatus.NOT_INTERESTED
                ));
            } else if (tvSeasonQuery.getUserTvSeasonStatus() == UserMovieStatusFilter.UNMARKED) {
                spec = spec.and(TvSeasonSpecifications.hasNoUserTvSeasonMark(currentUserId));
            } else {
                UserMovieStatus status = UserMovieStatus.valueOf(tvSeasonQuery.getUserTvSeasonStatus().name());
                spec = spec.and(useUserTvSeasonDefaultSort
                        ? TvSeasonSpecifications.hasUserTvSeasonStatusOrderByUserTime(
                                currentUserId,
                                status,
                                tvSeasonQuery.getUserRatingMin(),
                                tvSeasonQuery.getUserRatingMax()
                        )
                        : TvSeasonSpecifications.hasUserTvSeasonStatus(
                                currentUserId,
                                status,
                                tvSeasonQuery.getUserRatingMin(),
                                tvSeasonQuery.getUserRatingMax()
                        ));
            }
        }
        if (useDisplayRatingSort) {
            spec = spec.and(requestedSortField == TvSeasonSortField.VOTE_AVERAGE
                    ? TvSeasonSpecifications.orderByDisplayVoteAverage(requestedDirection)
                    : TvSeasonSpecifications.orderByDisplayVoteCount(requestedDirection));
        }

        Page<TvSeason> tvSeasonPage = tvSeasonRepository.findAll(spec, pageable);
        List<TvSeason> tvSeasons = tvSeasonPage.getContent();
        Map<Long, UserTvSeason> userTvSeasonsByTvSeasonId = getUserTvSeasonsByTvSeasonId(tvSeasons, currentUserId);

        List<TvSeasonResponse> content = tvSeasons.stream()
                .map(tvSeason -> convertToResponse(tvSeason, userTvSeasonsByTvSeasonId.get(tvSeason.getId())))
                .toList();

        return new PageResponse<>(
                content,
                tvSeasonPage.getNumber(),
                tvSeasonPage.getSize(),
                tvSeasonPage.getTotalElements(),
                tvSeasonPage.getTotalPages(),
                tvSeasonPage.isLast()
        );
    }

    public TvSeasonDetailResponse getTvSeasonById(Long id) {
        TvSeason tvSeason = tvSeasonRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TV season not found with id:" + id));
        return convertToDetailResponse(tvSeason);
    }

    @Transactional
    public void deleteTvSeason(Long id) {
        TvSeason tvSeason = tvSeasonRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TV season not found with id:" + id));

        userTvSeasonRepository.deleteByTvSeason(tvSeason);
        tvSeasonPersonRepository.deleteByTvSeason(tvSeason);
        tvSeason.getGenres().clear();
        tvSeason.getCountries().clear();
        tvSeasonRepository.delete(tvSeason);
    }

    @Transactional
    public TvSeason syncTvSeason(Long tvId, Integer seasonNumber) {
        TmdbTvShowResponse tvShow = tmdbClient.getTvShowById(tvId);
        TmdbTvSeasonResponse tmdbSeason = tmdbClient.getTvSeasonByNumber(tvId, seasonNumber);

        TvSeason tvSeason = tvSeasonRepository.findByTmdbTvIdAndSeasonNumber(tvId, seasonNumber)
                .orElseGet(TvSeason::new);

        tvSeason.setTmdbTvId(tvShow.getId());
        tvSeason.setTmdbSeasonId(tmdbSeason.getId());
        tvSeason.setSeasonNumber(tmdbSeason.getSeasonNumber());
        tvSeason.setSeasonCount(countOfficialSeasons(tvShow.getSeasons()));
        tvSeason.setShowName(tvShow.getName());
        tvSeason.setOriginalShowName(tvShow.getOriginalName());
        tvSeason.setTitle(buildSeasonTitle(tvShow.getName(), tmdbSeason.getName(), tmdbSeason.getSeasonNumber()));
        tvSeason.setOriginalTitle(buildOriginalSeasonTitle(tvShow.getOriginalName(), tmdbSeason.getSeasonNumber()));
        tvSeason.setOverview(firstText(tmdbSeason.getOverview(), tvShow.getOverview()));
        tvSeason.setPosterPath(firstText(tmdbSeason.getPosterPath(), tvShow.getPosterPath()));
        tvSeason.setAirDate(tmdbSeason.getAirDate());
        tvSeason.setYear(tmdbSeason.getAirDate() == null ? null : tmdbSeason.getAirDate().getYear());
        tvSeason.setEpisodeCount(tmdbSeason.getEpisodes() == null ? 0 : tmdbSeason.getEpisodes().size());
        tvSeason.setVoteAverage(tmdbSeason.getVoteAverage());
        tvSeason.setVoteCount(sumEpisodeVoteCount(tmdbSeason.getEpisodes()));
        tvSeason.setImdbParentId(tvShow.getExternalIds() == null ? null : tvShow.getExternalIds().getImdbId());
        tvSeason.setGenres(resolveTvGenres(tvShow.getGenres()));
        tvSeason.setCountries(resolveCountries(tvShow.getOriginCountry()));

        TvSeason savedSeason = tvSeasonRepository.save(tvSeason);

        syncCredits(savedSeason, tmdbClient.getTvSeasonCredits(tvId, seasonNumber));

        return savedSeason;
    }

    public List<TvSeason> syncTvShowSeasons(Long tvId) {
        TmdbTvShowResponse tvShow = tmdbClient.getTvShowById(tvId);
        if (tvShow == null || tvShow.getSeasons() == null || tvShow.getSeasons().isEmpty()) {
            return List.of();
        }

        return tvShow.getSeasons()
                .stream()
                .filter(season -> season.getSeasonNumber() != null && season.getSeasonNumber() > 0)
                .map(season -> syncTvSeason(tvId, season.getSeasonNumber()))
                .toList();
    }

    private void syncCredits(TvSeason tvSeason, TmdbCreditsResponse creditsResponse) {
        if (creditsResponse == null) {
            return;
        }

        tvSeasonPersonRepository.deleteByTvSeason(tvSeason);

        if (creditsResponse.getCast() != null) {
            for (Cast cast : creditsResponse.getCast()) {
                Person person = resolvePerson(cast);
                TvSeasonPerson tvSeasonPerson = new TvSeasonPerson();
                tvSeasonPerson.setTvSeason(tvSeason);
                tvSeasonPerson.setPerson(person);
                tvSeasonPerson.setJob("Actor");
                tvSeasonPerson.setCharacterName(cast.getCharacter());
                tvSeasonPerson.setSortOrder(cast.getOrder());
                tvSeasonPersonRepository.save(tvSeasonPerson);
            }
        }

        if (creditsResponse.getCrew() != null) {
            for (Crew crew : creditsResponse.getCrew()) {
                if (crew.getJob() == null || !IMPORTANT_JOBS.contains(crew.getJob().toLowerCase(Locale.ROOT))) {
                    continue;
                }

                Person person = resolvePerson(crew);
                TvSeasonPerson tvSeasonPerson = new TvSeasonPerson();
                tvSeasonPerson.setTvSeason(tvSeason);
                tvSeasonPerson.setPerson(person);
                tvSeasonPerson.setJob(crew.getJob());
                tvSeasonPersonRepository.save(tvSeasonPerson);
            }
        }
    }

    private boolean shouldUseUserTvSeasonDefaultSort(
            TvSeasonQuery tvSeasonQuery,
            Long currentUserId,
            TvSeasonSortField requestedSortField,
            Sort.Direction requestedDirection) {
        if (currentUserId == null || tvSeasonQuery.getUserTvSeasonStatus() == null) {
            return false;
        }
        if (tvSeasonQuery.getUserTvSeasonStatus() == UserMovieStatusFilter.DEFAULT
                || tvSeasonQuery.getUserTvSeasonStatus() == UserMovieStatusFilter.UNMARKED) {
            return false;
        }

        return (requestedSortField == TvSeasonSortField.CREATED_AT && requestedDirection == Sort.Direction.DESC)
                || (requestedSortField == TvSeasonSortField.ID && requestedDirection == Sort.Direction.ASC);
    }

    private boolean shouldUseDisplayRatingSort(TvSeasonSortField requestedSortField) {
        return requestedSortField == TvSeasonSortField.VOTE_AVERAGE
                || requestedSortField == TvSeasonSortField.VOTE_COUNT;
    }

    private Map<Long, UserTvSeason> getUserTvSeasonsByTvSeasonId(List<TvSeason> tvSeasons, Long currentUserId) {
        if (currentUserId == null || tvSeasons.isEmpty()) {
            return Map.of();
        }

        List<Long> tvSeasonIds = tvSeasons.stream().map(TvSeason::getId).toList();

        return userTvSeasonRepository.findByUserIdAndTvSeasonIds(currentUserId, tvSeasonIds)
                .stream()
                .collect(Collectors.toMap(userTvSeason -> userTvSeason.getTvSeason().getId(), Function.identity()));
    }

    private TvSeasonResponse convertToResponse(TvSeason tvSeason, UserTvSeason userTvSeason) {
        TvSeasonResponse response = new TvSeasonResponse();
        response.setId(tvSeason.getId());
        response.setTmdbTvId(tvSeason.getTmdbTvId());
        response.setTmdbSeasonId(tvSeason.getTmdbSeasonId());
        response.setSeasonNumber(tvSeason.getSeasonNumber());
        response.setSeasonCount(tvSeason.getSeasonCount());
        response.setTitle(tvSeason.getTitle());
        response.setOriginalTitle(tvSeason.getOriginalTitle());
        response.setShowName(tvSeason.getShowName());
        response.setOriginalShowName(tvSeason.getOriginalShowName());
        response.setAirDate(tvSeason.getAirDate());
        response.setYear(tvSeason.getYear());
        response.setEpisodeCount(tvSeason.getEpisodeCount());
        response.setVoteAverage(tvSeason.getVoteAverage());
        response.setVoteCount(tvSeason.getVoteCount());
        response.setImdbVoteAverage(tvSeason.getImdbVoteAverage());
        response.setImdbVoteCount(tvSeason.getImdbVoteCount());
        response.setImdbRatingUpdatedAt(tvSeason.getImdbRatingUpdatedAt());
        response.setPosterPath(tvSeason.getPosterPath());
        response.setCreatedAt(tvSeason.getCreatedAt());
        if (userTvSeason != null) {
            response.setUserTvSeasonStatus(userTvSeason.getStatus());
            response.setUserRating(userTvSeason.getRating());
            response.setUserWatchedDate(userTvSeason.getWatchedDate());
            response.setUserTvSeasonUpdatedAt(userTvSeason.getUpdatedAt());
        }
        return response;
    }

    private TvSeasonDetailResponse convertToDetailResponse(TvSeason tvSeason) {
        TvSeasonDetailResponse response = new TvSeasonDetailResponse();
        response.setId(tvSeason.getId());
        response.setTmdbTvId(tvSeason.getTmdbTvId());
        response.setTmdbSeasonId(tvSeason.getTmdbSeasonId());
        response.setSeasonNumber(tvSeason.getSeasonNumber());
        response.setSeasonCount(tvSeason.getSeasonCount());
        response.setTitle(tvSeason.getTitle());
        response.setOriginalTitle(tvSeason.getOriginalTitle());
        response.setShowName(tvSeason.getShowName());
        response.setOriginalShowName(tvSeason.getOriginalShowName());
        response.setOverview(tvSeason.getOverview());
        response.setPosterPath(tvSeason.getPosterPath());
        response.setAirDate(tvSeason.getAirDate());
        response.setYear(tvSeason.getYear());
        response.setEpisodeCount(tvSeason.getEpisodeCount());
        response.setVoteAverage(tvSeason.getVoteAverage());
        response.setVoteCount(tvSeason.getVoteCount());
        response.setImdbVoteAverage(tvSeason.getImdbVoteAverage());
        response.setImdbVoteCount(tvSeason.getImdbVoteCount());
        response.setImdbRatingUpdatedAt(tvSeason.getImdbRatingUpdatedAt());
        response.setImdbParentId(tvSeason.getImdbParentId());
        response.setGenres(tvSeason.getGenres().stream().map(Genre::getName).toList());
        response.setCountries(tvSeason.getCountries().stream().map(this::getCountryDisplayName).toList());

        List<TvSeasonPerson> tvSeasonPersons = tvSeasonPersonRepository.findByTvSeasonOrderBySortOrderAsc(tvSeason);

        response.setProducers(tvSeasonPersons.stream()
                .filter(person -> "Producer".equalsIgnoreCase(person.getJob()))
                .map(this::convertToPersonItemResponse)
                .toList());
        response.setDirectors(tvSeasonPersons.stream()
                .filter(person -> "Director".equalsIgnoreCase(person.getJob()))
                .map(this::convertToPersonItemResponse)
                .toList());
        response.setWriters(tvSeasonPersons.stream()
                .filter(person -> "Writer".equalsIgnoreCase(person.getJob()))
                .map(this::convertToPersonItemResponse)
                .toList());
        response.setCast(tvSeasonPersons.stream()
                .filter(person -> "Actor".equalsIgnoreCase(person.getJob()))
                .map(this::convertToPersonItemResponse)
                .toList());
        response.setCollectionSeasons(tvSeasonRepository.findByTmdbTvIdOrderBySeasonNumberAsc(tvSeason.getTmdbTvId())
                .stream()
                .map(this::convertToCollectionItemResponse)
                .toList());

        return response;
    }

    private TvSeasonCollectionItemResponse convertToCollectionItemResponse(TvSeason tvSeason) {
        TvSeasonCollectionItemResponse item = new TvSeasonCollectionItemResponse();
        item.setId(tvSeason.getId());
        item.setSeasonNumber(tvSeason.getSeasonNumber());
        item.setSeasonName(getSeasonName(tvSeason));
        item.setAirDate(tvSeason.getAirDate());
        item.setVoteAverage(tvSeason.getVoteAverage());
        item.setImdbVoteAverage(tvSeason.getImdbVoteAverage());
        item.setPosterPath(tvSeason.getPosterPath());
        return item;
    }

    private String getSeasonName(TvSeason tvSeason) {
        String title = tvSeason.getTitle();
        String showName = tvSeason.getShowName();
        String titlePrefix = showName + " ";
        if (hasText(title) && hasText(showName) && title.startsWith(titlePrefix)) {
            return title.substring(titlePrefix.length());
        }
        return hasText(title) ? title : "第" + tvSeason.getSeasonNumber() + "季";
    }

    private PersonItemResponse convertToPersonItemResponse(TvSeasonPerson tvSeasonPerson) {
        PersonItemResponse item = new PersonItemResponse();
        item.setPersonId(tvSeasonPerson.getPerson().getId());
        item.setName(tvSeasonPerson.getPerson().getName());
        item.setOriginalName(tvSeasonPerson.getPerson().getOriginalName());
        item.setProfilePath(tvSeasonPerson.getPerson().getProfilePath());
        item.setCharacter(tvSeasonPerson.getCharacterName());
        item.setJob(tvSeasonPerson.getJob());
        return item;
    }

    private Person resolvePerson(Cast cast) {
        Person person = personRepository.findByTmdbId(cast.getId()).orElseGet(Person::new);
        boolean isNewPerson = person.getId() == null;
        person.setTmdbId(cast.getId());
        person.setGender(cast.getGender());
        person.setKnownForDepartment(cast.getKnownForDepartment());
        person.setName(cast.getName());
        person.setOriginalName(cast.getOriginalName());
        person.setPopularity(cast.getPopularity());
        person.setProfilePath(cast.getProfilePath());
        if (isNewPerson) {
            fillPersonDetail(person, cast.getId());
        }
        return personRepository.save(person);
    }

    private Person resolvePerson(Crew crew) {
        Person person = personRepository.findByTmdbId(crew.getId()).orElseGet(Person::new);
        boolean isNewPerson = person.getId() == null;
        person.setTmdbId(crew.getId());
        person.setGender(crew.getGender());
        person.setKnownForDepartment(crew.getKnownForDepartment());
        person.setName(crew.getName());
        person.setOriginalName(crew.getOriginalName());
        person.setPopularity(crew.getPopularity());
        person.setProfilePath(crew.getProfilePath());
        if (isNewPerson) {
            fillPersonDetail(person, crew.getId());
        }
        return personRepository.save(person);
    }

    private void fillPersonDetail(Person person, Long tmdbPersonId) {
        TmdbPersonResponse personResponse = tmdbClient.getPersonById(tmdbPersonId);
        if (personResponse == null) {
            return;
        }

        person.setBiography(personResponse.getBiography());
        person.setBirthday(personResponse.getBirthday());
        person.setDeathday(personResponse.getDeathday());
        person.setPlaceOfBirth(personResponse.getPlaceOfBirth());
    }

    private Set<Genre> resolveTvGenres(List<TmdbMovieResponse.Genre> genreResponses) {
        Set<Genre> genres = new HashSet<>();
        if (genreResponses == null || genreResponses.isEmpty()) {
            return genres;
        }

        for (TmdbMovieResponse.Genre genreResponse : genreResponses) {
            Genre genre = genreRepository.findByTmdbIdAndMediaType(genreResponse.getId(), "TV")
                    .orElseGet(() -> {
                        Genre newGenre = new Genre();
                        newGenre.setTmdbId(genreResponse.getId());
                        newGenre.setMediaType("TV");
                        newGenre.setName(genreResponse.getName());
                        return genreRepository.save(newGenre);
                    });
            genres.add(genre);
        }
        return genres;
    }

    private Integer sumEpisodeVoteCount(List<TmdbTvSeasonResponse.Episode> episodes) {
        if (episodes == null || episodes.isEmpty()) {
            return null;
        }

        int voteCount = episodes.stream()
                .map(TmdbTvSeasonResponse.Episode::getVoteCount)
                .filter(count -> count != null && count > 0)
                .mapToInt(Integer::intValue)
                .sum();

        return voteCount == 0 ? null : voteCount;
    }

    private Set<Country> resolveCountries(List<String> countryCodes) {
        Set<Country> countries = new HashSet<>();
        if (countryCodes == null || countryCodes.isEmpty()) {
            return countries;
        }

        for (String countryCode : countryCodes) {
            if (!hasText(countryCode)) {
                continue;
            }

            String normalizedCode = countryCode.trim().toUpperCase(Locale.ROOT);
            Country country = countryRepository.findByCode(normalizedCode)
                    .orElseGet(() -> {
                        Country newCountry = new Country();
                        newCountry.setCode(normalizedCode);
                        newCountry.setName(resolveEnglishCountryName(normalizedCode));
                        newCountry.setChineseName(resolveChineseCountryName(normalizedCode));
                        return countryRepository.save(newCountry);
                    });
            if (!hasText(country.getChineseName())) {
                country.setChineseName(resolveChineseCountryName(normalizedCode));
            }
            countries.add(country);
        }
        return countries;
    }

    private String getCountryDisplayName(Country country) {
        if (hasText(country.getChineseName())) {
            return country.getChineseName();
        }
        String resolvedChineseName = resolveChineseCountryName(country.getCode());
        if (hasText(resolvedChineseName)) {
            return resolvedChineseName;
        }
        return country.getName();
    }

    private String buildSeasonTitle(String showName, String seasonName, Integer seasonNumber) {
        String resolvedSeasonName = hasText(seasonName) ? seasonName : "第" + seasonNumber + "季";
        if (!hasText(showName)) {
            return resolvedSeasonName;
        }
        return showName + " " + resolvedSeasonName;
    }

    private Integer countOfficialSeasons(List<TmdbTvShowResponse.Season> seasons) {
        if (seasons == null || seasons.isEmpty()) {
            return 0;
        }
        return (int) seasons.stream()
                .filter(season -> season.getSeasonNumber() != null && season.getSeasonNumber() > 0)
                .count();
    }

    private String buildOriginalSeasonTitle(String showName, Integer seasonNumber) {
        if (!hasText(showName)) {
            return "Season " + seasonNumber;
        }
        return showName + " Season " + seasonNumber;
    }

    private String firstText(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveEnglishCountryName(String countryCode) {
        try {
            Locale countryLocale = new Locale.Builder().setRegion(countryCode).build();
            String displayName = countryLocale.getDisplayCountry(Locale.ENGLISH);
            return displayName.equals(countryCode) ? countryCode : displayName;
        } catch (IllformedLocaleException ignored) {
            return countryCode;
        }
    }

    private String resolveChineseCountryName(String countryCode) {
        String overriddenName = COUNTRY_NAME_ZH_OVERRIDES.get(countryCode);
        if (overriddenName != null) {
            return overriddenName;
        }

        try {
            Locale countryLocale = new Locale.Builder().setRegion(countryCode).build();
            String displayName = countryLocale.getDisplayCountry(Locale.SIMPLIFIED_CHINESE);
            return displayName.equals(countryCode) ? null : displayName;
        } catch (IllformedLocaleException ignored) {
            return null;
        }
    }
}

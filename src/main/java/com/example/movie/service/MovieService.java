package com.example.movie.service;

import com.example.movie.dto.*;
import com.example.movie.entity.*;
import com.example.movie.enums.MovieSortField;
import com.example.movie.enums.UserMovieStatus;
import com.example.movie.enums.UserMovieStatusFilter;
import com.example.movie.exception.ResourceNotFoundException;
import com.example.movie.repository.*;
import com.example.movie.specification.MovieSpecifications;
import com.example.movie.tmdb.TmdbClient;
import com.example.movie.tmdb.dto.Cast;
import com.example.movie.tmdb.dto.TmdbCreditsResponse;
import com.example.movie.tmdb.dto.TmdbMovieResponse;
import com.example.movie.tmdb.dto.TmdbPersonResponse;
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
public class MovieService {

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
            Map.entry("CZ", "捷克"),
            Map.entry("DE", "德国"),
            Map.entry("DK", "丹麦"),
            Map.entry("ES", "西班牙"),
            Map.entry("FI", "芬兰"),
            Map.entry("FR", "法国"),
            Map.entry("GB", "英国"),
            Map.entry("GR", "希腊"),
            Map.entry("HK", "香港"),
            Map.entry("ID", "印度尼西亚"),
            Map.entry("IE", "爱尔兰"),
            Map.entry("IL", "以色列"),
            Map.entry("IN", "印度"),
            Map.entry("IR", "伊朗"),
            Map.entry("IT", "意大利"),
            Map.entry("JP", "日本"),
            Map.entry("KR", "韩国"),
            Map.entry("MX", "墨西哥"),
            Map.entry("MY", "马来西亚"),
            Map.entry("NL", "荷兰"),
            Map.entry("NO", "挪威"),
            Map.entry("NZ", "新西兰"),
            Map.entry("PH", "菲律宾"),
            Map.entry("PL", "波兰"),
            Map.entry("PT", "葡萄牙"),
            Map.entry("RU", "俄罗斯"),
            Map.entry("SE", "瑞典"),
            Map.entry("SG", "新加坡"),
            Map.entry("TH", "泰国"),
            Map.entry("TR", "土耳其"),
            Map.entry("TW", "台湾"),
            Map.entry("US", "美国"),
            Map.entry("VN", "越南"),
            Map.entry("ZA", "南非")
    );

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final CompanyRepository companyRepository;
    private final CountryRepository countryRepository;
    private final CollectionRepository collectionRepository;
    private final PersonService personService;
    private final MoviePersonRepository moviePersonRepository;
    private final UserMovieRepository userMovieRepository;

    public MovieService(TmdbClient tmdbClient, MovieRepository movieRepository,
                        GenreRepository genreRepository,
                        CompanyRepository companyRepository,
                        CountryRepository countryRepository,
                        CollectionRepository collectionRepository,
                        PersonService personService,
                        MoviePersonRepository moviePersonRepository,
                        UserMovieRepository userMovieRepository) {
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.companyRepository = companyRepository;
        this.countryRepository = countryRepository;
        this.collectionRepository = collectionRepository;
        this.personService = personService;
        this.moviePersonRepository = moviePersonRepository;
        this.userMovieRepository = userMovieRepository;
    }

    public TmdbMovieResponse fetchMovieByIdFromTmdb(Long movieId) {
        return tmdbClient.getMovieById(movieId);
    }

    public TmdbCreditsResponse fetchCreditsByIdFromTmdb(Long movieId) {
        return tmdbClient.getCreditsById(movieId);
    }

    @Transactional
    public TmdbMovieResponse syncMovie(Long movieId) {
        TmdbMovieResponse tmdbMovieResponse = fetchMovieByIdFromTmdb(movieId);

        Movie movie = movieRepository.findByTmdbId(tmdbMovieResponse.getId()).orElseGet(Movie::new);

        movie.setTmdbId(tmdbMovieResponse.getId());
        movie.setTitle(tmdbMovieResponse.getTitle());
        movie.setOriginalTitle(tmdbMovieResponse.getOriginalTitle());
        if (tmdbMovieResponse.getBelongsToCollection() != null) {
            movie.setCollectionId(tmdbMovieResponse.getBelongsToCollection().getId());
            resolveCollection(tmdbMovieResponse.getBelongsToCollection());
        }
        movie.setOverview(tmdbMovieResponse.getOverview());
        movie.setPopularity(tmdbMovieResponse.getPopularity());
        movie.setReleaseDate(tmdbMovieResponse.getReleaseDate());
        movie.setRevenue(tmdbMovieResponse.getRevenue());
        movie.setRuntime(tmdbMovieResponse.getRuntime());
        movie.setVoteAverage(tmdbMovieResponse.getVoteAverage());
        movie.setVoteCount(tmdbMovieResponse.getVoteCount());
        movie.setPosterPath(tmdbMovieResponse.getPosterPath());
        movie.setImdbId(tmdbMovieResponse.getImdbId());

        movie.setGenres(resolveGenres(tmdbMovieResponse.getGenres()));
        movie.setCompanies(resolveCompanies(tmdbMovieResponse.getProductionCompanies()));
        movie.setCountries(resolveCountries(tmdbMovieResponse.getProductionCountries()));

        movieRepository.save(movie);

        //同步credits
        TmdbCreditsResponse tmdbCreditsResponse = fetchCreditsByIdFromTmdb(movieId);
        personService.syncCredits(movie, tmdbCreditsResponse);

        return tmdbMovieResponse;
    }

    private Set<Genre> resolveGenres(List<TmdbMovieResponse.Genre> genreResponses) {
        Set<Genre> genres = new HashSet<>();

        if (genreResponses == null ||  genreResponses.isEmpty()) {
            return genres;
        }

        for (TmdbMovieResponse.Genre genreResponse : genreResponses) {
            Genre genre = genreRepository.findByTmdbId(genreResponse.getId())
                    .orElseGet(() -> {
                        Genre newGenre = new Genre();
                        newGenre.setTmdbId(genreResponse.getId());
                        newGenre.setName(genreResponse.getName());
                        return genreRepository.save(newGenre);
                    });
            genres.add(genre);
        }
        return genres;
    }

    private Set<Company> resolveCompanies(List<TmdbMovieResponse.ProductionCompany> companyResponses) {
        Set<Company> companies = new HashSet<>();

        if (companyResponses == null ||  companyResponses.isEmpty()) {
            return companies;
        }

        for (TmdbMovieResponse.ProductionCompany companyResponse : companyResponses) {
            Company company = companyRepository.findByTmdbId(companyResponse.getId())
                    .orElseGet(() -> {
                        Company newCompany = new Company();
                        newCompany.setTmdbId(companyResponse.getId());
                        newCompany.setName(companyResponse.getName());
                        newCompany.setLogoPath(companyResponse.getLogoPath());
                        return companyRepository.save(newCompany);
                    });
            companies.add(company);
        }
        return companies;
    }

    private Set<Country> resolveCountries(List<TmdbMovieResponse.ProductionCountry> countryResponses) {
        Set<Country> countries = new HashSet<>();

        if (countryResponses == null ||  countryResponses.isEmpty()) {
            return countries;
        }

        for (TmdbMovieResponse.ProductionCountry countryResponse : countryResponses) {
            Country country = countryRepository.findByCode(countryResponse.getIso31661())
                    .orElseGet(() -> {
                        Country newCountry = new Country();
                        newCountry.setCode(countryResponse.getIso31661());
                        newCountry.setName(countryResponse.getName());
                        newCountry.setChineseName(resolveChineseCountryName(countryResponse.getIso31661()));
                        return countryRepository.save(newCountry);
                    });
            if (!hasText(country.getChineseName())) {
                country.setChineseName(resolveChineseCountryName(country.getCode()));
            }
            countries.add(country);
        }
        return countries;
    }

    private void resolveCollection(TmdbMovieResponse.CollectionInfo collectionResponse) {
        if (collectionResponse != null) {
            collectionRepository.findByTmdbId(collectionResponse.getId())
                    .orElseGet(() -> {
                        Collection newCollection = new Collection();
                        newCollection.setTmdbId(collectionResponse.getId());
                        newCollection.setName(collectionResponse.getName());
                        return collectionRepository.save(newCollection);
                    });
        }
    }

//    public MovieResponse addMovie(MovieRequest request) {
//        Movie movie = new Movie();
//        movie.setDirector(request.getDirector());
//        movie.setReleaseYear(request.getYear());
//        movie.setTitle(request.getTitle());
//
//        Movie savedMovie = movieRepository.save(movie);
//        return convertToResponse(savedMovie);
//    }

    public PageResponse<MovieResponse> getMovies(
            MovieQuery movieQuery,
            Long currentUserId,
            int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be less than 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        MovieSortField requestedSortField = MovieSortField.valueOf(sortBy);
        Sort.Direction requestedDirection = Sort.Direction.fromString(direction);
        boolean useUserMovieDefaultSort = shouldUseUserMovieDefaultSort(
                movieQuery,
                currentUserId,
                requestedSortField,
                requestedDirection
        );

        Sort sort = useUserMovieDefaultSort
                ? Sort.unsorted()
                : Sort.by(requestedDirection, requestedSortField.getField());

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Movie> spec = Specification.unrestricted();
        if (movieQuery.getTitle() != null && !movieQuery.getTitle().isBlank()) {
            spec = spec.and(MovieSpecifications.titleContains(movieQuery.getTitle()));
        }
        if (movieQuery.getGenreId() != null) {
            spec = spec.and(MovieSpecifications.hasGenre(movieQuery.getGenreId()));
        }
        if (movieQuery.getYear() != null) {
            spec = spec.and(MovieSpecifications.hasReleaseYear(movieQuery.getYear()));
        }
        if (movieQuery.getRegionCode() != null && !movieQuery.getRegionCode().isBlank()) {
            spec = spec.and(MovieSpecifications.hasRegion(movieQuery.getRegionCode()));
        }
        if (movieQuery.getUserMovieStatus() != null && currentUserId != null) {
            if (movieQuery.getUserMovieStatus() == UserMovieStatusFilter.UNMARKED) {
                spec = spec.and(MovieSpecifications.hasNoUserMovieMark(currentUserId));
            } else {
                UserMovieStatus status = UserMovieStatus.valueOf(movieQuery.getUserMovieStatus().name());
                spec = spec.and(useUserMovieDefaultSort
                        ? MovieSpecifications.hasUserMovieStatusOrderByUserTime(
                                currentUserId,
                                status,
                                movieQuery.getUserRatingMin(),
                                movieQuery.getUserRatingMax()
                        )
                        : MovieSpecifications.hasUserMovieStatus(
                                currentUserId,
                                status,
                                movieQuery.getUserRatingMin(),
                                movieQuery.getUserRatingMax()
                        ));
            }
        }

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);
//        if (title != null && !title.isEmpty() && genreId != null) {
//            moviePage = movieRepository.findByTitleAndGenre(title, genreId, pageable);
//        } else if (title != null && !title.isEmpty()) {
//            moviePage = movieRepository.findByTitleContainingIgnoreCase(title, pageable);
//        } else if (genreId != null) {
//            moviePage = movieRepository.findByGenreId(genreId, pageable);
//        } else {
//            moviePage = movieRepository.findAll(pageable);
//        }

        List<Movie> movies = moviePage.getContent();
        Map<Long, UserMovie> userMoviesByMovieId = getUserMoviesByMovieId(movies, currentUserId);

        List<MovieResponse> content = movies
                .stream()
                .map(movie -> convertToResponse(movie, userMoviesByMovieId.get(movie.getId())))
                .toList();

        return  new PageResponse<>(
                content,
                moviePage.getNumber(),
                moviePage.getSize(),
                moviePage.getTotalElements(),
                moviePage.getTotalPages(),
                moviePage.isLast()
        );
    }

    public MovieDetailResponse getMovieById(Long id) {
        Movie movie = movieRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id:" + id));
        return this.convertToDetailResponse(movie);
    }

    private boolean shouldUseUserMovieDefaultSort(
            MovieQuery movieQuery,
            Long currentUserId,
            MovieSortField requestedSortField,
            Sort.Direction requestedDirection) {
        if (currentUserId == null || movieQuery.getUserMovieStatus() == null) {
            return false;
        }
        if (movieQuery.getUserMovieStatus() == UserMovieStatusFilter.UNMARKED) {
            return false;
        }

        return (requestedSortField == MovieSortField.CREATED_AT && requestedDirection == Sort.Direction.DESC)
                || (requestedSortField == MovieSortField.ID && requestedDirection == Sort.Direction.ASC);
    }

    private Map<Long, UserMovie> getUserMoviesByMovieId(
            List<Movie> movies,
            Long currentUserId) {
        if (currentUserId == null || movies.isEmpty()) {
            return Map.of();
        }

        List<Long> movieIds = movies.stream().map(Movie::getId).toList();

        return userMovieRepository.findByUserIdAndMovieIds(currentUserId, movieIds)
                .stream()
                .collect(Collectors.toMap(userMovie -> userMovie.getMovie().getId(), Function.identity()));
    }

//    public void deleteMovie(Long id) {
//        Movie movie = movieRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id:" + id));
//
//        movieRepository.delete(movie);
//    }

//    public MovieResponse updateMovie(Long id, MovieRequest request) {
//        Movie movie = movieRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id:" + id));
//
//        movie.setTitle(request.getTitle());
//        movie.setDirector(request.getDirector());
//        movie.setReleaseYear(request.getYear());
//
//        Movie updatedMovie = movieRepository.save(movie);
//        return convertToResponse(updatedMovie);
//    }

    private MovieResponse convertToResponse(Movie movie) {
        return convertToResponse(movie, null);
    }

    private MovieResponse convertToResponse(Movie movie, UserMovie userMovie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTmdbId(movie.getTmdbId());
        response.setTitle(movie.getTitle());
        response.setOriginalTitle(movie.getOriginalTitle());
        response.setPopularity(movie.getPopularity());
        response.setReleaseDate(movie.getReleaseDate());
        response.setVoteAverage(movie.getVoteAverage());
        response.setVoteCount(movie.getVoteCount());
        response.setPosterPath(movie.getPosterPath());
        response.setCreatedAt(movie.getCreatedAt());
        if (userMovie != null) {
            response.setUserMovieStatus(userMovie.getStatus());
            response.setUserRating(userMovie.getRating());
            response.setUserWatchedDate(userMovie.getWatchedDate());
            response.setUserMovieUpdatedAt(userMovie.getUpdatedAt());
        }
        return response;
    }

    private MovieDetailResponse convertToDetailResponse(Movie movie) {
        MovieDetailResponse response = new MovieDetailResponse();

        response.setId(movie.getId());
        response.setTmdbId(movie.getTmdbId());
        response.setTitle(movie.getTitle());
        response.setOriginalTitle(movie.getOriginalTitle());
        response.setOverview(movie.getOverview());
        response.setPopularity(movie.getPopularity());
        response.setReleaseDate(movie.getReleaseDate());
        response.setRevenue(movie.getRevenue());
        response.setRuntime(movie.getRuntime());
        response.setVoteAverage(movie.getVoteAverage());
        response.setVoteCount(movie.getVoteCount());
        response.setPosterPath(movie.getPosterPath());
        response.setImdbId(movie.getImdbId());

        response.setGenres(
                movie.getGenres().stream()
                        .map(Genre::getName)
                        .toList()
        );

        response.setCompanies(
                movie.getCompanies().stream()
                        .map(Company::getName)
                        .toList()
        );

        response.setCountries(
                movie.getCountries().stream()
                        .map(this::getCountryDisplayName)
                        .toList()
        );

        List<MoviePerson> moviePersons = moviePersonRepository.findByMovieIdWithPersonOrderBySortOrderAsc(movie.getId());

        response.setDirectors(
                moviePersons.stream()
                        .filter(mp -> "Director".equalsIgnoreCase(mp.getJob()))
                        .map(this::convertToPersonItemResponse)
                        .toList()
        );

        response.setCast(
                moviePersons.stream()
                        .filter(mp -> "Actor".equalsIgnoreCase(mp.getJob()))
                        .map(this::convertToPersonItemResponse)
                        .toList()
        );

        return response;
    }

    private PersonItemResponse convertToPersonItemResponse(MoviePerson moviePerson) {
        PersonItemResponse item = new PersonItemResponse();
        item.setPersonId(moviePerson.getPerson().getId());
        item.setName(moviePerson.getPerson().getName());
        item.setOriginalName(moviePerson.getPerson().getOriginalName());
        item.setProfilePath(moviePerson.getPerson().getProfilePath());
        item.setCharacter(moviePerson.getCharacterName());
        item.setJob(moviePerson.getJob());
        return item;
    }

    private String getCountryDisplayName(Country country) {
        if (hasText(country.getChineseName())) {
            return country.getChineseName();
        }

        return country.getName();
    }

    private String resolveChineseCountryName(String countryCode) {
        if (!hasText(countryCode)) {
            return null;
        }

        String normalizedCode = countryCode.trim().toUpperCase(Locale.ROOT);
        String overriddenName = COUNTRY_NAME_ZH_OVERRIDES.get(normalizedCode);
        if (overriddenName != null) {
            return overriddenName;
        }

        try {
            Locale countryLocale = new Locale.Builder().setRegion(normalizedCode).build();
            String displayName = countryLocale.getDisplayCountry(Locale.SIMPLIFIED_CHINESE);
            if (displayName.equals(normalizedCode)) {
                return null;
            }
            return displayName;
        } catch (IllformedLocaleException ignored) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

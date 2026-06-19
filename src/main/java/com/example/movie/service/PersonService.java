package com.example.movie.service;

import com.example.movie.dto.PersonDetailResponse;
import com.example.movie.dto.PersonItemResponse;
import com.example.movie.dto.PersonMovieCreditResponse;
import com.example.movie.entity.Movie;
import com.example.movie.entity.MoviePerson;
import com.example.movie.entity.Person;
import com.example.movie.entity.TvSeason;
import com.example.movie.entity.TvSeasonPerson;
import com.example.movie.exception.ResourceNotFoundException;
import com.example.movie.repository.MoviePersonRepository;
import com.example.movie.repository.PersonRepository;
import com.example.movie.repository.TvSeasonPersonRepository;
import com.example.movie.tmdb.TmdbClient;
import com.example.movie.tmdb.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PersonService {

    private final TmdbClient tmdbClient;
    private final PersonRepository personRepository;
    private final MoviePersonRepository moviePersonRepository;
    private final TvSeasonPersonRepository tvSeasonPersonRepository;
    private static final Set<String> IMPORTANT_JOBS = Set.of(
            "director",
            "writer",
            "producer"
    );


    public PersonService(TmdbClient tmdbClient, PersonRepository personRepository,
                         MoviePersonRepository moviePersonRepository,
                         TvSeasonPersonRepository tvSeasonPersonRepository) {
        this.tmdbClient = tmdbClient;
        this.personRepository = personRepository;
        this.moviePersonRepository = moviePersonRepository;
        this.tvSeasonPersonRepository = tvSeasonPersonRepository;
    }

    public TmdbPersonResponse fetchPersonByIdFromTmdb(Long personId) {
        return tmdbClient.getPersonById(personId);
    }

    public PersonDetailResponse getPersonById(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id:" + personId));
        List<MoviePerson> moviePersons = moviePersonRepository.findByPersonIdWithMovieOrderByReleaseDateDesc(personId);
        List<TvSeasonPerson> tvSeasonPersons = tvSeasonPersonRepository.findByPersonIdWithTvSeasonOrderByAirDateDesc(personId);

        PersonDetailResponse response = new PersonDetailResponse();
        response.setId(person.getId());
        response.setTmdbId(person.getTmdbId());
        response.setName(person.getName());
        response.setOriginalName(person.getOriginalName());
        response.setGender(person.getGender());
        response.setBirthday(person.getBirthday());
        response.setDeathday(person.getDeathday());
        response.setPlaceOfBirth(person.getPlaceOfBirth());
        response.setBiography(person.getBiography());
        response.setKnownForDepartment(person.getKnownForDepartment());
        response.setProfilePath(person.getProfilePath());
        response.setPopularity(person.getPopularity());

        List<PersonMovieCreditResponse> credits = new ArrayList<>();
        credits.addAll(convertToMovieCredits(moviePersons));
        credits.addAll(convertToTvSeasonCredits(tvSeasonPersons));
        credits.sort(Comparator
                .comparing(PersonMovieCreditResponse::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PersonMovieCreditResponse::getWorkId, Comparator.reverseOrder()));
        response.setMovieCreditCount(credits.size());
        response.setCredits(credits);

        return response;
    }

    public List<PersonItemResponse> searchPersons(String name, int size) {
        int resolvedSize = Math.max(1, Math.min(size, 20));
        String keyword = name == null ? "" : name.trim();
        List<Person> persons = keyword.isBlank()
                ? personRepository.findAll(PageRequest.of(0, resolvedSize, Sort.by(Sort.Direction.DESC, "popularity"))).getContent()
                : personRepository.findByNameContainingIgnoreCaseOrOriginalNameContainingIgnoreCase(
                        keyword,
                        keyword,
                        PageRequest.of(0, resolvedSize, Sort.by(Sort.Direction.DESC, "popularity"))
                ).getContent();

        return persons.stream()
                .map(this::convertToPersonItemResponse)
                .toList();
    }

    @Transactional
    public void syncCredits(Movie movie, TmdbCreditsResponse tmdbCreditsResponse) {
        if (tmdbCreditsResponse != null) {
            moviePersonRepository.deleteByMovie(movie);
            moviePersonRepository.flush();
            List<Cast> casts = tmdbCreditsResponse.getCast();
            syncCasts(movie, casts);
            List<Crew> crews = tmdbCreditsResponse.getCrew();
            syncCrews(movie, crews);
        }
    }

    //同步casts信息
    private void syncCasts(Movie movie, List<Cast> casts) {
        for  (Cast cast : casts) {
            Person person = personRepository.findByTmdbId(cast.getId()).orElseGet(Person::new);

            person.setGender(cast.getGender());
            person.setTmdbId(cast.getId());
            person.setKnownForDepartment(cast.getKnownForDepartment());
            person.setName(cast.getName());
            person.setOriginalName(cast.getOriginalName());
            person.setPopularity(cast.getPopularity());
            person.setProfilePath(cast.getProfilePath());
            if (person.getId() == null) {
                //新人，补充基础信息
                TmdbPersonResponse tmdbPersonResponse = fetchPersonByIdFromTmdb(cast.getId());
                person.setBiography(tmdbPersonResponse.getBiography());
                person.setBirthday(tmdbPersonResponse.getBirthday());
                person.setDeathday(tmdbPersonResponse.getDeathday());
                person.setPlaceOfBirth(tmdbPersonResponse.getPlaceOfBirth());
            }
            personRepository.save(person);

            //save movie_person
            MoviePerson moviePerson = new MoviePerson();
            moviePerson.setMovie(movie);
            moviePerson.setPerson(person);
            moviePerson.setJob("Actor");
            moviePerson.setCharacterName(cast.getCharacter());
            moviePerson.setSortOrder(cast.getOrder());
            moviePersonRepository.save(moviePerson);
        }
    }

    //同步crews信息
    private void syncCrews(Movie movie, List<Crew> crews) {
        for  (Crew crew : crews) {
            if (!IMPORTANT_JOBS.contains(crew.getJob().toLowerCase())) {
                continue;
            }

            Person person = personRepository.findByTmdbId(crew.getId()).orElseGet(Person::new);
            person.setGender(crew.getGender());
            person.setTmdbId(crew.getId());
            person.setKnownForDepartment(crew.getKnownForDepartment());
            person.setName(crew.getName());
            person.setOriginalName(crew.getOriginalName());
            person.setPopularity(crew.getPopularity());
            person.setProfilePath(crew.getProfilePath());
            if (person.getId() == null) {
                //新人，补充基础信息
                TmdbPersonResponse tmdbPersonResponse = fetchPersonByIdFromTmdb(crew.getId());
                person.setBiography(tmdbPersonResponse.getBiography());
                person.setBirthday(tmdbPersonResponse.getBirthday());
                person.setDeathday(tmdbPersonResponse.getDeathday());
                person.setPlaceOfBirth(tmdbPersonResponse.getPlaceOfBirth());
            }
            personRepository.save(person);

            //save movie_person
            MoviePerson moviePerson = new MoviePerson();
            moviePerson.setMovie(movie);
            moviePerson.setPerson(person);
            moviePerson.setJob(crew.getJob());
            moviePersonRepository.save(moviePerson);
        }
    }

    private List<PersonMovieCreditResponse> convertToMovieCredits(List<MoviePerson> moviePersons) {
        Map<Long, PersonMovieCreditResponse> creditsByMovieId = new LinkedHashMap<>();

        for (MoviePerson moviePerson : moviePersons) {
            Movie movie = moviePerson.getMovie();
            PersonMovieCreditResponse credit = creditsByMovieId.computeIfAbsent(movie.getId(), movieId -> {
            PersonMovieCreditResponse item = new PersonMovieCreditResponse();
                item.setWorkId(movie.getId());
                item.setMediaType("MOVIE");
                item.setMovieId(movie.getId());
                item.setTmdbId(movie.getTmdbId());
                item.setTitle(movie.getTitle());
                item.setOriginalTitle(movie.getOriginalTitle());
                item.setReleaseDate(movie.getReleaseDate());
                item.setVoteAverage(movie.getVoteAverage());
                item.setVoteCount(movie.getVoteCount());
                item.setImdbVoteAverage(movie.getImdbVoteAverage());
                item.setImdbVoteCount(movie.getImdbVoteCount());
                item.setImdbRatingUpdatedAt(movie.getImdbRatingUpdatedAt());
                item.setPosterPath(movie.getPosterPath());
                item.setJobs(new ArrayList<>());
                return item;
            });

            if (hasText(moviePerson.getJob())
                    && credit.getJobs().stream().noneMatch(job -> job.equalsIgnoreCase(moviePerson.getJob()))) {
                credit.getJobs().add(moviePerson.getJob());
            }
            if (!hasText(credit.getCharacter()) && hasText(moviePerson.getCharacterName())) {
                credit.setCharacter(moviePerson.getCharacterName());
            }
        }

        return new ArrayList<>(creditsByMovieId.values());
    }

    private List<PersonMovieCreditResponse> convertToTvSeasonCredits(List<TvSeasonPerson> tvSeasonPersons) {
        Map<Long, PersonMovieCreditResponse> creditsByTvSeasonId = new LinkedHashMap<>();

        for (TvSeasonPerson tvSeasonPerson : tvSeasonPersons) {
            TvSeason tvSeason = tvSeasonPerson.getTvSeason();
            PersonMovieCreditResponse credit = creditsByTvSeasonId.computeIfAbsent(tvSeason.getId(), tvSeasonId -> {
                PersonMovieCreditResponse item = new PersonMovieCreditResponse();
                item.setWorkId(tvSeason.getId());
                item.setMediaType("TV");
                item.setTvSeasonId(tvSeason.getId());
                item.setTmdbId(tvSeason.getTmdbTvId());
                item.setTitle(tvSeason.getTitle());
                item.setOriginalTitle(tvSeason.getOriginalTitle());
                item.setReleaseDate(tvSeason.getAirDate());
                item.setVoteAverage(tvSeason.getVoteAverage());
                item.setVoteCount(tvSeason.getVoteCount());
                item.setImdbVoteAverage(tvSeason.getImdbVoteAverage());
                item.setImdbVoteCount(tvSeason.getImdbVoteCount());
                item.setImdbRatingUpdatedAt(tvSeason.getImdbRatingUpdatedAt());
                item.setPosterPath(tvSeason.getPosterPath());
                item.setJobs(new ArrayList<>());
                return item;
            });

            if (hasText(tvSeasonPerson.getJob())
                    && credit.getJobs().stream().noneMatch(job -> job.equalsIgnoreCase(tvSeasonPerson.getJob()))) {
                credit.getJobs().add(tvSeasonPerson.getJob());
            }
            if (!hasText(credit.getCharacter()) && hasText(tvSeasonPerson.getCharacterName())) {
                credit.setCharacter(tvSeasonPerson.getCharacterName());
            }
        }

        return new ArrayList<>(creditsByTvSeasonId.values());
    }

    private PersonItemResponse convertToPersonItemResponse(Person person) {
        PersonItemResponse response = new PersonItemResponse();
        response.setPersonId(person.getId());
        response.setName(person.getName());
        response.setOriginalName(person.getOriginalName());
        response.setProfilePath(person.getProfilePath());
        return response;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

//    public List<Genre> getAllLocalGenres() {
//        return genreRepository.findAll();
//    }
}

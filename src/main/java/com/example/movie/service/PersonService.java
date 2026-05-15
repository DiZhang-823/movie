package com.example.movie.service;

import com.example.movie.entity.Genre;
import com.example.movie.entity.Movie;
import com.example.movie.entity.MoviePerson;
import com.example.movie.entity.Person;
import com.example.movie.repository.GenreRepository;
import com.example.movie.repository.MoviePersonRepository;
import com.example.movie.repository.PersonRepository;
import com.example.movie.tmdb.TmdbClient;
import com.example.movie.tmdb.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class PersonService {

    private final TmdbClient tmdbClient;
    private final PersonRepository personRepository;
    private final MoviePersonRepository moviePersonRepository;
    private static final Set<String> IMPORTANT_JOBS = Set.of(
            "director",
            "writer",
            "producer"
    );


    public PersonService(TmdbClient tmdbClient, PersonRepository personRepository,
                         MoviePersonRepository moviePersonRepository) {
        this.tmdbClient = tmdbClient;
        this.personRepository = personRepository;
        this.moviePersonRepository = moviePersonRepository;
    }

    public TmdbPersonResponse fetchPersonByIdFromTmdb(Long personId) {
        return tmdbClient.getPersonById(personId);
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

//    public List<Genre> getAllLocalGenres() {
//        return genreRepository.findAll();
//    }
}
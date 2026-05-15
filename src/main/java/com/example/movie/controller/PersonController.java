package com.example.movie.controller;

import com.example.movie.entity.Genre;
import com.example.movie.service.GenreService;
import com.example.movie.service.PersonService;
import com.example.movie.tmdb.dto.TmdbGenreDto;
import com.example.movie.tmdb.dto.TmdbPersonResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@Tag(name = "Persons API", description = "Operations for managing persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    // 直接看 TMDB 返回结果，不保存数据库
    @GetMapping("/tmdb/get/{personId}")
    public TmdbPersonResponse getPersonByIdFromTmdb(@PathVariable Long personId) {
        return personService.fetchPersonByIdFromTmdb(personId);
    }

//    // 从 TMDB 拉取并保存到本地数据库
//    @PostMapping("/tmdb/sync/{personId}")
//    public TmdbPersonResponse syncPerson(@PathVariable Long personId) {
//        return personService.syncPerson(personId);
//    }



    // 查询本地数据库中的 genre
//    @GetMapping
//    public List<Genre> getLocalGenres() {
//        return genreService.getAllLocalGenres();
//    }
}
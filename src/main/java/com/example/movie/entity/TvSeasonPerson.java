package com.example.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tv_season_person")
public class TvSeasonPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tv_season_id", nullable = false)
    private TvSeason tvSeason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String job;

    @Column(name = "character_name")
    private String characterName;

    @Column(name = "sort_order")
    private Integer sortOrder;
}

package com.example.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AwardNominationResponse {
    private Long id;
    private Long awardCategoryId;
    private String awardCategoryName;
    private Long awardEditionId;
    private String awardEditionName;
    private Long awardBodyId;
    private String awardBodyName;
    private Long movieId;
    private String movieTitle;
    private String moviePosterPath;
    private Long tvSeasonId;
    private String tvSeasonTitle;
    private String tvSeasonShowName;
    private Integer tvSeasonNumber;
    private String tvSeasonPosterPath;
    private Long personId;
    private String personName;
    private String personProfilePath;
    private Boolean winner;
    private String note;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

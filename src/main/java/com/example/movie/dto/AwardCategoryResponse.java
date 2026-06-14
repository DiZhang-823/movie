package com.example.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AwardCategoryResponse {
    private Long id;
    private Long awardEditionId;
    private String awardEditionName;
    private Long awardBodyId;
    private String awardBodyName;
    private String name;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.movie.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MovieRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotBlank(message = "Director cannot be blank")
    private String director;

    @Min(value = 1800, message = "Year must be no earlier than 1800")
    @Max(value = 2100, message = "Year must be no later than 2100")
    private Integer year;

}
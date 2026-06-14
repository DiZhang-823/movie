package com.example.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "award_edition", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_award_edition_body_edition_number",
                columnNames = {"award_body_id", "edition_number"}
        )
})
public class AwardEdition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "award_body_id", nullable = false)
    private AwardBody awardBody;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "edition_number", nullable = false)
    private Integer editionNumber;

    @Column(name = "award_year")
    private Integer awardYear;

    @Column(name = "ceremony_date")
    private LocalDate ceremonyDate;

    @Lob
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}

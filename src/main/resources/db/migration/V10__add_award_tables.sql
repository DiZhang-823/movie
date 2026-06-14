CREATE TABLE award_body (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    original_name VARCHAR(150) NULL,
    sort_order INT NULL,
    description LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_body_name (name),
    KEY idx_award_body_sort_order (sort_order)
);

CREATE TABLE award_edition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    award_body_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    edition_number INT NOT NULL,
    award_year INT NULL,
    ceremony_date DATE NULL,
    description LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_edition_body_edition_number (award_body_id, edition_number),
    KEY idx_award_edition_award_year (award_year),
    KEY idx_award_edition_ceremony_date (ceremony_date),
    CONSTRAINT fk_award_edition_award_body
        FOREIGN KEY (award_body_id) REFERENCES award_body (id)
        ON DELETE CASCADE
);

CREATE TABLE award_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    award_edition_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    sort_order INT NULL,
    description LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_award_category_edition_name (award_edition_id, name),
    KEY idx_award_category_sort_order (sort_order),
    CONSTRAINT fk_award_category_award_edition
        FOREIGN KEY (award_edition_id) REFERENCES award_edition (id)
        ON DELETE CASCADE
);

CREATE TABLE award_nomination (
    id BIGINT NOT NULL AUTO_INCREMENT,
    award_category_id BIGINT NOT NULL,
    movie_id BIGINT NULL,
    tv_season_id BIGINT NULL,
    person_id BIGINT NULL,
    is_winner BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(255) NULL,
    sort_order INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_award_nomination_category_id (award_category_id),
    KEY idx_award_nomination_movie_id (movie_id),
    KEY idx_award_nomination_tv_season_id (tv_season_id),
    KEY idx_award_nomination_person_id (person_id),
    KEY idx_award_nomination_winner (is_winner),
    CONSTRAINT fk_award_nomination_award_category
        FOREIGN KEY (award_category_id) REFERENCES award_category (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_award_nomination_movie
        FOREIGN KEY (movie_id) REFERENCES movie (id),
    CONSTRAINT fk_award_nomination_tv_season
        FOREIGN KEY (tv_season_id) REFERENCES tv_season (id),
    CONSTRAINT fk_award_nomination_person
        FOREIGN KEY (person_id) REFERENCES person (id),
    CONSTRAINT chk_award_nomination_one_work
        CHECK (movie_id IS NULL OR tv_season_id IS NULL),
    CONSTRAINT chk_award_nomination_has_target
        CHECK (movie_id IS NOT NULL OR tv_season_id IS NOT NULL OR person_id IS NOT NULL)
);

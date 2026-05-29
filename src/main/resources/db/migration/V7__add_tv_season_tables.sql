ALTER TABLE genre
    ADD COLUMN media_type VARCHAR(20) NOT NULL DEFAULT 'MOVIE' AFTER tmdb_id;

SET @genre_tmdb_unique_index := (
    SELECT INDEX_NAME
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'genre'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING COUNT(*) = 1
       AND SUM(CASE WHEN COLUMN_NAME = 'tmdb_id' THEN 1 ELSE 0 END) = 1
    LIMIT 1
);

SET @drop_genre_tmdb_unique_sql := IF(
    @genre_tmdb_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE genre DROP INDEX `', REPLACE(@genre_tmdb_unique_index, '`', '``'), '`')
);

PREPARE drop_genre_tmdb_unique_statement FROM @drop_genre_tmdb_unique_sql;
EXECUTE drop_genre_tmdb_unique_statement;
DEALLOCATE PREPARE drop_genre_tmdb_unique_statement;

ALTER TABLE genre
    ADD UNIQUE KEY uk_genre_tmdb_id_media_type (tmdb_id, media_type);

CREATE TABLE tv_season (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tmdb_tv_id BIGINT NOT NULL,
    tmdb_season_id BIGINT NULL,
    season_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255) NULL,
    show_name VARCHAR(255) NOT NULL,
    original_show_name VARCHAR(255) NULL,
    overview LONGTEXT NULL,
    poster_path VARCHAR(255) NULL,
    air_date DATE NULL,
    year INT NULL,
    episode_count INT NULL,
    vote_average DECIMAL(10, 4) NULL,
    vote_count INT NULL,
    imdb_vote_average DECIMAL(10, 4) NULL,
    imdb_vote_count INT NULL,
    imdb_rating_updated_at DATETIME(6) NULL,
    imdb_parent_id VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tv_season_tmdb_tv_id_season_number (tmdb_tv_id, season_number),
    KEY idx_tv_season_tmdb_season_id (tmdb_season_id),
    KEY idx_tv_season_imdb_parent_season (imdb_parent_id, season_number),
    KEY idx_tv_season_year (year),
    KEY idx_tv_season_air_date (air_date)
);

CREATE TABLE tv_episode (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tv_season_id BIGINT NOT NULL,
    tmdb_episode_id BIGINT NULL,
    imdb_id VARCHAR(20) NULL,
    season_number INT NOT NULL,
    episode_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    overview LONGTEXT NULL,
    still_path VARCHAR(255) NULL,
    air_date DATE NULL,
    runtime INT NULL,
    imdb_vote_average DECIMAL(10, 4) NULL,
    imdb_vote_count INT NULL,
    imdb_rating_updated_at DATETIME(6) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tv_episode_season_episode_number (tv_season_id, episode_number),
    UNIQUE KEY uk_tv_episode_tmdb_episode_id (tmdb_episode_id),
    KEY idx_tv_episode_imdb_id (imdb_id),
    CONSTRAINT fk_tv_episode_tv_season
        FOREIGN KEY (tv_season_id) REFERENCES tv_season (id)
        ON DELETE CASCADE
);

CREATE TABLE tv_season_person (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tv_season_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    job VARCHAR(255) NOT NULL,
    character_name VARCHAR(255) NULL,
    sort_order INT NULL,
    PRIMARY KEY (id),
    KEY idx_tv_season_person_tv_season_id (tv_season_id),
    KEY idx_tv_season_person_person_id (person_id),
    KEY idx_tv_season_person_job (job),
    CONSTRAINT fk_tv_season_person_tv_season
        FOREIGN KEY (tv_season_id) REFERENCES tv_season (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tv_season_person_person
        FOREIGN KEY (person_id) REFERENCES person (id)
);

CREATE TABLE tv_season_genre (
    tv_season_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (tv_season_id, genre_id),
    KEY idx_tv_season_genre_genre_id (genre_id),
    CONSTRAINT fk_tv_season_genre_tv_season
        FOREIGN KEY (tv_season_id) REFERENCES tv_season (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tv_season_genre_genre
        FOREIGN KEY (genre_id) REFERENCES genre (id)
);

CREATE TABLE tv_season_country (
    tv_season_id BIGINT NOT NULL,
    country_id BIGINT NOT NULL,
    PRIMARY KEY (tv_season_id, country_id),
    KEY idx_tv_season_country_country_id (country_id),
    CONSTRAINT fk_tv_season_country_tv_season
        FOREIGN KEY (tv_season_id) REFERENCES tv_season (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tv_season_country_country
        FOREIGN KEY (country_id) REFERENCES country (id)
);

CREATE TABLE user_tv_season (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tv_season_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    rating TINYINT NULL,
    watched_date DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tv_season_user_tv_season (user_id, tv_season_id),
    KEY idx_user_tv_season_tv_season_id (tv_season_id),
    CONSTRAINT fk_user_tv_season_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_tv_season_tv_season
        FOREIGN KEY (tv_season_id) REFERENCES tv_season (id)
        ON DELETE CASCADE
);

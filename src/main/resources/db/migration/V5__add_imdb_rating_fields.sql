ALTER TABLE movie
    ADD COLUMN imdb_vote_average DECIMAL(10, 4) NULL,
    ADD COLUMN imdb_vote_count INT NULL,
    ADD COLUMN imdb_rating_updated_at DATETIME(6) NULL;

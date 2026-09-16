CREATE TABLE sources (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    author          VARCHAR(255) NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    date_started    DATE         NOT NULL,
    date_finished   DATE,
    status          VARCHAR(20)  NOT NULL,
    rating          SMALLINT,
    general_notes   TEXT,
    CONSTRAINT chk_sources_type CHECK (type IN ('BOOK', 'ARTICLE', 'PAPER')),
    CONSTRAINT chk_sources_status CHECK (status IN ('READING', 'FINISHED', 'ABANDONED')),
    CONSTRAINT chk_sources_rating CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5))
);

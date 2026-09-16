CREATE TABLE notes (
    id              BIGSERIAL PRIMARY KEY,
    source_id       BIGINT       NOT NULL REFERENCES sources (id) ON DELETE CASCADE,
    content         TEXT         NOT NULL,
    location_ref    VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notes_source_id ON notes (source_id);

CREATE TABLE tags (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL
);

-- Case-insensitive uniqueness: "Stoicism" and "stoicism" must not
-- become two different tags. Enforced here, not just in application
-- code, so the invariant holds even if a future code path forgets.
CREATE UNIQUE INDEX uq_tags_name_lower ON tags (lower(name));

CREATE TABLE note_tags (
    note_id BIGINT NOT NULL REFERENCES notes (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, tag_id)
);

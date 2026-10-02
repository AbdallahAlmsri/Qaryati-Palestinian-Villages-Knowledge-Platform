CREATE TABLE population_records (
                                    id              BIGSERIAL PRIMARY KEY,
                                    village_id      BIGINT       NOT NULL REFERENCES villages(id),
                                    year            INTEGER      NOT NULL,
                                    population      INTEGER      NOT NULL,
                                    source          VARCHAR(255) NOT NULL,
                                    source_url      VARCHAR(500),
                                    notes           TEXT,
                                    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                                    created_by      BIGINT       NOT NULL REFERENCES users(id),
                                    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                                    reviewed_by     BIGINT       REFERENCES users(id),
                                    reviewed_at     TIMESTAMP,

                                    CONSTRAINT chk_population_non_negative CHECK (population >= 0),
                                    CONSTRAINT chk_year_range CHECK (year BETWEEN 1800 AND 2100),
    CONSTRAINT chk_status CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
    CONSTRAINT uq_village_year_source UNIQUE (village_id, year, source)
);

CREATE INDEX idx_population_village_year ON population_records (village_id, year);
CREATE TABLE governorates (
                              id          BIGSERIAL PRIMARY KEY,
                              name        VARCHAR(100) NOT NULL UNIQUE,
                              created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE villages (
                          id              BIGSERIAL PRIMARY KEY,
                          governorate_id  BIGINT NOT NULL REFERENCES governorates(id),
                          latitude        DECIMAL(9,6),
                          longitude       DECIMAL(9,6),
                          location_description TEXT,
                          elevation_m     INTEGER,
                          land_area_km2   DECIMAL(10,2),
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_villages_governorate_id ON villages(governorate_id);

CREATE TABLE village_names (
                               id          BIGSERIAL PRIMARY KEY,
                               village_id  BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
                               name        VARCHAR(200) NOT NULL,
                               name_type   VARCHAR(30) NOT NULL,
                               language    VARCHAR(20),
                               valid_from  INTEGER,
                               valid_to    INTEGER,
                               source      TEXT,
                               created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_village_names_village_id ON village_names(village_id);
CREATE INDEX idx_village_names_name_type ON village_names(name_type);
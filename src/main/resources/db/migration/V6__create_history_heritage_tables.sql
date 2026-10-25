CREATE TABLE historical_events (
    id BIGSERIAL PRIMARY KEY,
    village_id BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    event_date DATE,
    source TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_historical_event_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_historical_events_village_id
ON historical_events(village_id);

CREATE TABLE landmarks (
    id BIGSERIAL PRIMARY KEY,
    village_id BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    landmark_type VARCHAR(100),
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    source TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_landmark_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_landmarks_village_id
ON landmarks(village_id);

CREATE TABLE village_attributes (
    id BIGSERIAL PRIMARY KEY,
    village_id BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    attribute_name VARCHAR(150) NOT NULL,
    attribute_value TEXT NOT NULL,
    source TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_village_attribute_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_village_attributes_village_id
ON village_attributes(village_id);

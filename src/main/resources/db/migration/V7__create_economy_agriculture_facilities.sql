CREATE TABLE village_agriculture (
    id BIGSERIAL PRIMARY KEY,
    village_id BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    crop_name VARCHAR(150) NOT NULL,
    description TEXT,
    source TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_agriculture_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_agriculture_village_id
ON village_agriculture(village_id);

CREATE TABLE village_economy (
    id BIGSERIAL PRIMARY KEY,
    village_id BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    activity_name VARCHAR(150) NOT NULL,
    description TEXT,
    source TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_economy_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_economy_village_id
ON village_economy(village_id);

CREATE TABLE village_facilities (
    id BIGSERIAL PRIMARY KEY,
    village_id BIGINT NOT NULL REFERENCES villages(id) ON DELETE CASCADE,
    facility_name VARCHAR(150) NOT NULL,
    facility_type VARCHAR(100) NOT NULL,
    description TEXT,
    source TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_facilities_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_facilities_village_id
ON village_facilities(village_id);

-- T4: the remaining Palestinian governorates, municipalities, and the link from a village to its municipality.

INSERT INTO governorates (name) VALUES
    ('Jenin'),
    ('Tubas'),
    ('Tulkarm'),
    ('Qalqilya'),
    ('Salfit'),
    ('Ramallah and Al-Bireh'),
    ('Jericho'),
    ('Jerusalem'),
    ('Bethlehem'),
    ('Hebron'),
    ('North Gaza'),
    ('Gaza'),
    ('Deir al-Balah'),
    ('Khan Yunis'),
    ('Rafah')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE municipalities (
    id              BIGSERIAL PRIMARY KEY,
    governorate_id  BIGINT NOT NULL REFERENCES governorates(id),
    name            VARCHAR(150) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_municipalities_governorate_name UNIQUE (governorate_id, name)
);

CREATE INDEX idx_municipalities_governorate_id ON municipalities(governorate_id);

-- The main city municipality of each governorate. Smaller municipalities are added as data is collected.
INSERT INTO municipalities (governorate_id, name)
SELECT g.id, m.name
FROM (VALUES
    ('Jenin', 'Jenin'),
    ('Tubas', 'Tubas'),
    ('Tulkarm', 'Tulkarm'),
    ('Nablus', 'Nablus'),
    ('Qalqilya', 'Qalqilya'),
    ('Salfit', 'Salfit'),
    ('Ramallah and Al-Bireh', 'Ramallah'),
    ('Ramallah and Al-Bireh', 'Al-Bireh'),
    ('Jericho', 'Jericho'),
    ('Bethlehem', 'Bethlehem'),
    ('Hebron', 'Hebron'),
    ('North Gaza', 'Jabalia'),
    ('Gaza', 'Gaza'),
    ('Deir al-Balah', 'Deir al-Balah'),
    ('Khan Yunis', 'Khan Yunis'),
    ('Rafah', 'Rafah')
) AS m(governorate, name)
JOIN governorates g ON g.name = m.governorate;

-- Optional: a village may not belong to any municipality (for example one governed by a village council).
ALTER TABLE villages
    ADD COLUMN municipality_id BIGINT REFERENCES municipalities(id);

CREATE INDEX idx_villages_municipality_id ON villages(municipality_id);

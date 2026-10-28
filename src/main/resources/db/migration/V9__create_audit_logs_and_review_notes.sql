CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_actor_id
    ON audit_logs(actor_id);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at);

ALTER TABLE population_records
    ADD COLUMN review_notes TEXT;

ALTER TABLE historical_events
    ADD COLUMN review_notes TEXT;

ALTER TABLE landmarks
    ADD COLUMN review_notes TEXT;

ALTER TABLE village_attributes
    ADD COLUMN review_notes TEXT;

ALTER TABLE village_names
    ADD COLUMN review_notes TEXT;
ALTER TABLE active_session
    ADD COLUMN public_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
    ADD COLUMN last_activity_at DATETIME(6) NULL,
    ADD COLUMN core_card_id BIGINT NULL,
    ADD COLUMN can_act VARCHAR(20) NULL,
    ADD COLUMN needs_time BOOLEAN NULL,
    ADD COLUMN recommended_route VARCHAR(20) NULL,
    ADD COLUMN chosen_route VARCHAR(20) NULL,
    ADD COLUMN cards_generated_by VARCHAR(4) NULL,
    ADD COLUMN safety_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD INDEX idx_active_session_activity (last_activity_at),
    MODIFY COLUMN raw_text LONGTEXT CHARACTER SET utf8mb4 NULL;

UPDATE active_session SET last_activity_at = started_at;

ALTER TABLE active_session
    MODIFY COLUMN last_activity_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE session_summary
    ADD COLUMN reflection_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
    ADD COLUMN active_public_id BIGINT NULL UNIQUE;

CREATE TABLE thought_card (
    card_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    seq INT NOT NULL,
    content LONGTEXT CHARACTER SET utf8mb4 NOT NULL,
    category VARCHAR(16) NOT NULL,
    CONSTRAINT uq_thought_card_sequence UNIQUE (session_id, seq),
    CONSTRAINT chk_thought_card_category CHECK (category IN ('FACT', 'ASSUMPTION', 'UNCLASSIFIED')),
    CONSTRAINT fk_thought_card_session FOREIGN KEY (session_id)
        REFERENCES active_session(session_id) ON DELETE CASCADE
);

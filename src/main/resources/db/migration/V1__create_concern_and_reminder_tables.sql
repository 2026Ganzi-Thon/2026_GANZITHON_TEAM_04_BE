CREATE TABLE app_user (
    user_id VARCHAR(36) NOT NULL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL,
    step_count INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_app_user_steps CHECK (step_count >= 0)
);

CREATE TABLE active_session (
    session_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    situation_type VARCHAR(20) NULL,
    weight_before INT NULL,
    raw_text TEXT NULL,
    started_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_active_session_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT chk_active_session_weight CHECK (weight_before IS NULL OR weight_before BETWEEN 1 AND 5),
    INDEX idx_active_session_user (user_id)
);

CREATE TABLE session_summary (
    session_id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    situation_type VARCHAR(20) NOT NULL,
    weight_before INT NOT NULL,
    recommended_route VARCHAR(20) NOT NULL,
    chosen_route VARCHAR(20) NOT NULL,
    weight_reduced BOOLEAN NOT NULL,
    CONSTRAINT fk_session_summary_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
    CONSTRAINT chk_session_summary_weight CHECK (weight_before BETWEEN 1 AND 5),
    INDEX idx_session_summary_user_completed (user_id, completed_at)
);

CREATE TABLE deferred_record (
    deferred_record_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_deferred_record_summary FOREIGN KEY (session_id)
        REFERENCES session_summary(session_id) ON DELETE CASCADE,
    INDEX idx_deferred_record_created (created_at, deferred_record_id)
);

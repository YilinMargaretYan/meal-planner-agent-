-- Apply this migration to existing diet_db databases before deploying the preference-memory feature.
CREATE TABLE IF NOT EXISTS diet_user_preference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    slot_name VARCHAR(64) NOT NULL,
    option_value VARCHAR(64) NOT NULL,
    preference_type VARCHAR(16) NOT NULL,
    confidence DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_preference (user_id, slot_name, option_value, preference_type),
    KEY idx_user_preference_active (user_id, expires_at, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

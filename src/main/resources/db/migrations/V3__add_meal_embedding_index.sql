-- MySQL-backed semantic index. meal_item remains the source of truth.
CREATE TABLE IF NOT EXISTS diet_meal_embedding (
    meal_id BIGINT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    owner_user_id BIGINT NULL,
    content_hash CHAR(64) NOT NULL,
    embedding_model VARCHAR(64) NOT NULL,
    embedding_json MEDIUMTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (meal_id),
    INDEX idx_meal_embedding_scope (source_type, owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

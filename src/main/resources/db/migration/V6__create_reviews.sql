-- ==============================================================================
-- V6__create_reviews.sql: Đánh giá sau giao dịch & điểm uy tín
--  - users.reputation_score = NULL nghĩa là chưa có đánh giá nào
-- ==============================================================================

ALTER TABLE users
    MODIFY reputation_score DECIMAL(3, 2) NULL DEFAULT NULL,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0 AFTER reputation_score;

UPDATE users SET reputation_score = NULL WHERE review_count = 0;

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exchange_request_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reviews_request_reviewer UNIQUE (exchange_request_id, reviewer_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_reviews_request FOREIGN KEY (exchange_request_id) REFERENCES exchange_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_reviews_reviewee (reviewee_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

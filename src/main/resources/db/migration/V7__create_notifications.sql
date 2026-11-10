-- ==============================================================================
-- V7__create_notifications.sql: Thông báo trong ứng dụng & tùy chọn nhận email
-- ==============================================================================

ALTER TABLE users
    ADD COLUMN email_notifications BOOLEAN NOT NULL DEFAULT TRUE AFTER is_active;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    type ENUM('EXCHANGE_REQUESTED', 'EXCHANGE_ACCEPTED', 'EXCHANGE_REJECTED', 'EXCHANGE_CANCELLED',
              'EXCHANGE_COMPLETED', 'REVIEW_RECEIVED') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    -- Who caused it (NULL for automatic events) and what it is about, for links in the app
    actor_id BIGINT NULL,
    item_id BIGINT NULL,
    exchange_request_id BIGINT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_request FOREIGN KEY (exchange_request_id) REFERENCES exchange_requests (id) ON DELETE SET NULL,
    INDEX idx_notifications_recipient_created (recipient_id, created_at),
    INDEX idx_notifications_recipient_unread (recipient_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

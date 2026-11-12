-- ==============================================================================
-- V12__review_disputes_and_support.sql: Trả lời đánh giá, báo cáo đánh giá, ticket hỗ trợ với ban quản trị
-- ==============================================================================

-- 1. Người bị đánh giá trả lời công khai; kiểm duyệt viên có thể ẩn đánh giá sai sự thật
ALTER TABLE reviews
    ADD COLUMN reply VARCHAR(1000) NULL AFTER comment,
    ADD COLUMN replied_at TIMESTAMP NULL AFTER reply,
    ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE AFTER replied_at,
    ADD INDEX idx_reviews_reviewee_hidden (reviewee_id, hidden, created_at);

-- 2. Báo cáo một đánh giá; hành động mới: ẩn đánh giá
ALTER TABLE reports
    MODIFY target_type ENUM('ITEM', 'USER', 'REVIEW') NOT NULL,
    ADD COLUMN target_review_id BIGINT NULL AFTER target_item_id,
    ADD CONSTRAINT fk_reports_review FOREIGN KEY (target_review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    MODIFY action ENUM('NONE', 'REMOVE_ITEM', 'BAN_USER', 'HIDE_REVIEW') NULL;

ALTER TABLE notifications
    MODIFY type ENUM('EXCHANGE_REQUESTED', 'EXCHANGE_ACCEPTED', 'EXCHANGE_REJECTED', 'EXCHANGE_CANCELLED',
                     'EXCHANGE_COMPLETED', 'REVIEW_RECEIVED', 'REPORT_RESOLVED', 'ITEM_REMOVED',
                     'WANTED_MATCH', 'REVIEW_REPLIED', 'REVIEW_HIDDEN', 'SUPPORT_REPLY') NOT NULL,
    ADD COLUMN support_ticket_id BIGINT NULL AFTER exchange_request_id;

-- 3. Ticket hỗ trợ: trao đổi giữa một người dùng và ban quản trị (kiểm duyệt viên / admin)
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    status ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    -- Ai mở ticket: người dùng tự mở, hoặc ban quản trị chủ động liên hệ để làm rõ
    opened_by_staff BOOLEAN NOT NULL DEFAULT FALSE,
    related_report_id BIGINT NULL,
    last_message_at TIMESTAMP NULL,
    user_last_read_message_id BIGINT NULL,
    staff_last_read_message_id BIGINT NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_support_tickets_report FOREIGN KEY (related_report_id) REFERENCES reports (id) ON DELETE SET NULL,
    INDEX idx_support_tickets_status (status, last_message_at),
    INDEX idx_support_tickets_user (user_id, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    -- TRUE khi người gửi là kiểm duyệt viên / admin (hiển thị là "Ban quản trị")
    from_staff BOOLEAN NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_support_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_support_messages_ticket (ticket_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_support_ticket FOREIGN KEY (support_ticket_id)
        REFERENCES support_tickets (id) ON DELETE SET NULL;

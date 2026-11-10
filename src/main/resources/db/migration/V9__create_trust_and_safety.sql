-- ==============================================================================
-- V9__create_trust_and_safety.sql: Báo cáo vi phạm, chặn người dùng, khóa tài khoản, nhật ký quản trị
-- ==============================================================================

-- Tin bị kiểm duyệt viên gỡ: chủ tin không thể tự hiển thị lại
ALTER TABLE items
    MODIFY status ENUM('AVAILABLE', 'IN_TRANSACTION', 'COMPLETED', 'HIDDEN', 'REMOVED') NOT NULL DEFAULT 'AVAILABLE';

-- Khóa tài khoản: is_active = FALSE kèm thời điểm và lý do
ALTER TABLE users
    ADD COLUMN banned_at TIMESTAMP NULL AFTER is_active,
    ADD COLUMN ban_reason VARCHAR(500) NULL AFTER banned_at;

ALTER TABLE notifications
    MODIFY type ENUM('EXCHANGE_REQUESTED', 'EXCHANGE_ACCEPTED', 'EXCHANGE_REJECTED', 'EXCHANGE_CANCELLED',
                     'EXCHANGE_COMPLETED', 'REVIEW_RECEIVED', 'REPORT_RESOLVED', 'ITEM_REMOVED') NOT NULL;

CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type ENUM('ITEM', 'USER') NOT NULL,
    target_item_id BIGINT NULL,
    target_user_id BIGINT NOT NULL,
    reason ENUM('SCAM', 'PROHIBITED_ITEM', 'INAPPROPRIATE', 'SPAM', 'HARASSMENT', 'OTHER') NOT NULL,
    details VARCHAR(1000) NULL,
    status ENUM('OPEN', 'RESOLVED', 'DISMISSED') NOT NULL DEFAULT 'OPEN',
    action ENUM('NONE', 'REMOVE_ITEM', 'BAN_USER') NULL,
    resolution_note VARCHAR(1000) NULL,
    handled_by BIGINT NULL,
    handled_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_item FOREIGN KEY (target_item_id) REFERENCES items (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_user FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_handled_by FOREIGN KEY (handled_by) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_reports_status_created (status, created_at),
    INDEX idx_reports_target_user (target_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_blocks (
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_id, blocked_id),
    CONSTRAINT fk_user_blocks_blocker FOREIGN KEY (blocker_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_blocks_blocked FOREIGN KEY (blocked_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_blocks_blocked (blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nhật ký mọi thao tác kiểm duyệt / quản trị (không xóa khi người thực hiện bị xóa)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NULL,
    details VARCHAR(1000) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_audit_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

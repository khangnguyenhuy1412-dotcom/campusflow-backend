-- ==============================================================================
-- V13__create_item_comments.sql: Bình luận công khai dưới tin đăng (hỏi đáp), trả lời 1 tầng
-- ==============================================================================

CREATE TABLE IF NOT EXISTS item_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    -- NULL: bình luận gốc; có giá trị: trả lời cho một bình luận gốc (chỉ 1 tầng)
    parent_id BIGINT NULL,
    content VARCHAR(1000) NOT NULL,
    -- Xóa mềm bởi người viết hoặc chủ tin: giữ chỗ để mạch hội thoại không bị đứt
    deleted_at TIMESTAMP NULL,
    -- Ẩn bởi kiểm duyệt viên
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_comments_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_comments_parent FOREIGN KEY (parent_id) REFERENCES item_comments (id) ON DELETE CASCADE,
    INDEX idx_item_comments_item_parent (item_id, parent_id, created_at),
    INDEX idx_item_comments_parent (parent_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Báo cáo bình luận; hành động mới: ẩn bình luận
ALTER TABLE reports
    MODIFY target_type ENUM('ITEM', 'USER', 'REVIEW', 'COMMENT') NOT NULL,
    ADD COLUMN target_comment_id BIGINT NULL AFTER target_review_id,
    ADD CONSTRAINT fk_reports_comment FOREIGN KEY (target_comment_id) REFERENCES item_comments (id) ON DELETE CASCADE,
    MODIFY action ENUM('NONE', 'REMOVE_ITEM', 'BAN_USER', 'HIDE_REVIEW', 'HIDE_COMMENT') NULL;

ALTER TABLE notifications
    MODIFY type ENUM('EXCHANGE_REQUESTED', 'EXCHANGE_ACCEPTED', 'EXCHANGE_REJECTED', 'EXCHANGE_CANCELLED',
                     'EXCHANGE_COMPLETED', 'REVIEW_RECEIVED', 'REPORT_RESOLVED', 'ITEM_REMOVED',
                     'WANTED_MATCH', 'REVIEW_REPLIED', 'REVIEW_HIDDEN', 'SUPPORT_REPLY',
                     'ITEM_COMMENTED', 'COMMENT_REPLIED') NOT NULL;

-- ==============================================================================
-- V8__create_chat.sql: Nhắn tin giữa người mua và người đăng tin
--  - Mỗi cặp (món đồ, người mua) có đúng một cuộc trò chuyện
--  - *_last_read_message_id: con trỏ "đã đọc đến tin nhắn nào" để đếm tin chưa đọc
-- ==============================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    last_message_at TIMESTAMP NULL,
    last_message_preview VARCHAR(200) NULL,
    buyer_last_read_message_id BIGINT NULL,
    seller_last_read_message_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conversations_item_buyer UNIQUE (item_id, buyer_id),
    CONSTRAINT fk_conversations_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_buyer FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_seller FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_conversations_buyer (buyer_id, last_message_at),
    INDEX idx_conversations_seller (seller_id, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_messages_conversation (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

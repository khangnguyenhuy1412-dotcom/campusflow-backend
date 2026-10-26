-- ==============================================================================
-- V5__extend_items_and_images.sql: Bổ sung cho tin đăng & hình ảnh
--  - items.version, exchange_requests.version: optimistic locking
--    (tránh 2 yêu cầu được chấp nhận cùng lúc, hoặc vừa chấp nhận vừa từ chối)
--  - item_images: ảnh thu nhỏ + thứ tự hiển thị
--  - Index cho tìm kiếm / lọc
-- ==============================================================================

ALTER TABLE items
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD INDEX idx_items_status_created (status, created_at),
    ADD INDEX idx_items_category_status (category_id, status),
    ADD INDEX idx_items_user_status (user_id, status);

ALTER TABLE item_images
    ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER image_url,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER is_primary;

ALTER TABLE exchange_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD INDEX idx_requests_item_status (item_id, status),
    ADD INDEX idx_requests_requester_status (requester_id, status);

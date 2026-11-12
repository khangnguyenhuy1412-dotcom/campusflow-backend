-- ==============================================================================
-- V11__soft_delete_items.sql: Xóa mềm tin đăng
--  Tin bị chủ xóa chuyển sang DELETED thay vì xóa khỏi CSDL, để giữ lịch sử giao dịch,
--  đánh giá và thông báo. Tin DELETED không hiển thị ở bất kỳ đâu.
-- ==============================================================================

ALTER TABLE items
    MODIFY status ENUM('AVAILABLE', 'IN_TRANSACTION', 'COMPLETED', 'HIDDEN', 'REMOVED', 'DELETED')
        NOT NULL DEFAULT 'AVAILABLE';

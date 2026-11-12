-- ==============================================================================
-- V10__create_student_features.sql: Trường đại học theo tên miền email, "Cần tìm", xác nhận giao nhận bằng QR
-- ==============================================================================

-- 1. Trường đại học: nhận diện từ tên miền email (khớp hậu tố dài nhất, vd. student.hcmus.edu.vn -> hcmus.edu.vn)
CREATE TABLE IF NOT EXISTS universities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    short_name VARCHAR(30) NOT NULL,
    email_domain VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO universities (name, short_name, email_domain) VALUES
('Trường Đại học Khoa học Tự nhiên, ĐHQG-HCM', 'HCMUS', 'hcmus.edu.vn'),
('Trường Đại học Bách khoa, ĐHQG-HCM', 'HCMUT', 'hcmut.edu.vn'),
('Trường Đại học Công nghệ Thông tin, ĐHQG-HCM', 'UIT', 'uit.edu.vn'),
('Trường Đại học Quốc tế, ĐHQG-HCM', 'HCMIU', 'hcmiu.edu.vn'),
('Trường Đại học Kinh tế - Luật, ĐHQG-HCM', 'UEL', 'uel.edu.vn'),
('Trường Đại học Khoa học Xã hội và Nhân văn, ĐHQG-HCM', 'HCMUSSH', 'hcmussh.edu.vn'),
('Trường Đại học Kinh tế TP.HCM', 'UEH', 'ueh.edu.vn'),
('Trường Đại học Sư phạm Kỹ thuật TP.HCM', 'HCMUTE', 'hcmute.edu.vn'),
('Trường Đại học Sư phạm TP.HCM', 'HCMUE', 'hcmue.edu.vn'),
('Trường Đại học Tôn Đức Thắng', 'TDTU', 'tdtu.edu.vn'),
('Trường Đại học Công nghiệp TP.HCM', 'IUH', 'iuh.edu.vn'),
('Trường Đại học Nông Lâm TP.HCM', 'NLU', 'hcmuaf.edu.vn'),
('Trường Đại học Văn Lang', 'VLU', 'vlu.edu.vn'),
('Trường Đại học Công nghệ TP.HCM', 'HUTECH', 'hutech.edu.vn'),
('Trường Đại học FPT', 'FPTU', 'fpt.edu.vn'),
('Đại học Bách khoa Hà Nội', 'HUST', 'hust.edu.vn'),
('Đại học Quốc gia Hà Nội', 'VNU', 'vnu.edu.vn'),
('Trường Đại học Kinh tế Quốc dân', 'NEU', 'neu.edu.vn'),
('Trường Đại học Ngoại thương', 'FTU', 'ftu.edu.vn'),
('Học viện Công nghệ Bưu chính Viễn thông', 'PTIT', 'ptit.edu.vn'),
('Trường Đại học Cần Thơ', 'CTU', 'ctu.edu.vn');

ALTER TABLE users
    ADD COLUMN university_id BIGINT NULL AFTER university,
    ADD CONSTRAINT fk_users_university FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE SET NULL;

-- Gán trường cho người dùng đã có (khớp chính xác hoặc là tên miền con)
UPDATE users u
JOIN universities un
  ON SUBSTRING_INDEX(u.email, '@', -1) = un.email_domain
  OR SUBSTRING_INDEX(u.email, '@', -1) LIKE CONCAT('%.', un.email_domain)
SET u.university_id = un.id, u.university = un.name;

-- 2. "Cần tìm": sinh viên đăng món cần, hệ thống báo khi có tin phù hợp
CREATE TABLE IF NOT EXISTS wanted_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    title VARCHAR(150) NOT NULL,
    keywords VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    max_price DECIMAL(12, 2) NULL,
    same_university_only BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('ACTIVE', 'FULFILLED', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wanted_posts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wanted_posts_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    INDEX idx_wanted_posts_status_expires (status, expires_at),
    INDEX idx_wanted_posts_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Mỗi cặp (tin cần tìm, món đồ) chỉ được thông báo một lần
CREATE TABLE IF NOT EXISTS wanted_matches (
    wanted_post_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (wanted_post_id, item_id),
    CONSTRAINT fk_wanted_matches_post FOREIGN KEY (wanted_post_id) REFERENCES wanted_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_wanted_matches_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE notifications
    MODIFY type ENUM('EXCHANGE_REQUESTED', 'EXCHANGE_ACCEPTED', 'EXCHANGE_REJECTED', 'EXCHANGE_CANCELLED',
                     'EXCHANGE_COMPLETED', 'REVIEW_RECEIVED', 'REPORT_RESOLVED', 'ITEM_REMOVED',
                     'WANTED_MATCH') NOT NULL;

-- 3. Xác nhận giao nhận tại buổi gặp: người cho đưa mã/QR, người nhận quét để hoàn tất
ALTER TABLE exchange_requests
    ADD COLUMN handover_code_hash CHAR(64) NULL,
    ADD COLUMN handover_code_expires_at TIMESTAMP NULL,
    ADD COLUMN handover_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN handover_verified BOOLEAN NOT NULL DEFAULT FALSE;

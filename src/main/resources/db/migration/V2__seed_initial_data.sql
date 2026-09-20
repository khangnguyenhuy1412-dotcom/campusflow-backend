-- ==============================================================================
-- V2__seed_initial_data.sql: Khởi tạo dữ liệu mặc định (Roles, Categories)
-- ==============================================================================

-- 1. Thêm các vai trò người dùng mặc định
INSERT INTO roles (name) VALUES 
('ROLE_STUDENT'),
('ROLE_ADMIN'),
('ROLE_MODERATOR')
ON DUPLICATE KEY UPDATE name=name;

-- 2. Thêm các danh mục đồ dùng phổ biến cho sinh viên
INSERT INTO categories (name, slug, description, icon_url) VALUES
('Giáo trình & Sách học tập', 'giao-trinh-sach', 'Sách chuyên ngành, giáo trình đại học, tài liệu ôn thi', 'book-open'),
('Đồ gia dụng & Phòng trọ', 'do-gia-dung-phong-tro', 'Bếp điện, nồi cơm, ấm siêu tốc, quạt, bàn ghế xếp', 'home'),
('Thiết bị điện tử', 'thiet-bi-dien-tu', 'Laptop, máy tính cầm tay Casio, chuột, bàn phím, tai nghe', 'laptop'),
('Thời trang & Phụ kiện', 'thoi-trang-phu-kien', 'Quần áo, balo, giày dép, mũ bảo hiểm sinh viên', 'shirt'),
('Phương tiện di chuyển', 'phuong-tien-di-chuyen', 'Xe đạp, xe điện, phụ kiện xe máy', 'bike'),
('Khác', 'khac', 'Các vật dụng học tập và đời sống sinh viên khác', 'more-horizontal')
ON DUPLICATE KEY UPDATE name=name;

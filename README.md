# CampusFlow Backend 🎓📦

> Nền tảng trao đổi và thanh lý đồ dùng học tập, vật dụng đời sống dành cho sinh viên.

---

## 📌 Giới thiệu dự án

**CampusFlow** được thiết kế nhằm giải quyết bài toán lãng phí đồ dùng trong môi trường đại học:
- Giáo trình, sách tham khảo, tài liệu môn học.
- Đồ dùng phòng trọ, điện gia dụng, nội thất nhỏ.
- Thiết bị điện tử, đồ dùng học tập.

Hệ thống hỗ trợ 3 hình thức: **Cho tặng (0đ)**, **Trao đổi ngang giá (Trade)** và **Thanh lý giá sinh viên**.

---

## 🛠 Tech Stack

- **Ngôn ngữ & Framework**: Java 21, Spring Boot 4.x
- **Bảo mật**: Spring Security 6, JWT (JSON Web Token)
- **Cơ sở dữ liệu**: MySQL 8.x
- **ORM & Data Access**: Spring Data JPA / Hibernate
- **Containerization**: Docker & Docker Compose
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Build Tool**: Maven

---

## 🌿 Quy ước Git & Nhánh (Branching Strategy)

Dự án áp dụng mô hình Git Workflow rút gọn:

- `main`: Chứa mã nguồn ổn định, đã kiểm thử và sẵn sàng triển khai (Production).
- `develop`: Nhánh tích hợp chính dành cho việc phát triển phiên bản tiếp theo.
- `feature/<tên-tính-năng>`: Nhánh làm tính năng mới (tạo từ `develop`).
  - *Ví dụ*: `feature/auth-jwt`, `feature/item-listing`, `feature/user-profile`.
- `fix/<tên-lỗi>`: Nhánh sửa lỗi trong quá trình phát triển (tạo từ `develop`).
- `hotfix/<tên-lỗi>`: Sửa lỗi khẩn cấp trực tiếp từ `main`.

### 📝 Quy ước Commit (Conventional Commits)

Cú pháp: `<type>(<scope>): <mô tả ngắn>`

- `feat`: Tính năng mới (VD: `feat(auth): add student email verification`)
- `fix`: Sửa lỗi (VD: `fix(item): fix image upload max size validation`)
- `docs`: Cập nhật tài liệu / README (VD: `docs: update setup guide`)
- `refactor`: Tái cấu trúc mã nguồn không làm thay đổi tính năng
- `chore`: Cập nhật cấu hình, build tool, dependencies (VD: `chore: add docker-compose`)

---

## 🚀 Khởi chạy dự án cục bộ (Local Development)

### 1. Yêu cầu hệ thống
- JDK 21 trở lên
- Docker Desktop & Docker Compose
- Maven (hoặc dùng `./mvnw`)

### 2. Khởi động Cơ sở dữ liệu (Docker)
```bash
docker compose up -d
```

### 3. Khởi chạy Backend
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / MacOS
./mvnw spring-boot:run
```

---

## 👥 Đội ngũ phát triển
- Dự án CampusFlow Team


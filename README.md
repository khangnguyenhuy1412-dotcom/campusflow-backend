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
- **Bảo mật**: Spring Security 7, JWT (OAuth2 Resource Server, HS256) + Refresh Token
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

## 🧱 Quy ước mã nguồn (Code Conventions)

**Cấu trúc theo tính năng** — mỗi tính năng là một package `com.campusflow.<feature>` gồm:
`controller/` → `service/` → `repository/` → `entity/`, cùng `dto/` cho request/response.
Dùng chung: `common/` (lỗi, phân trang, `@CurrentUserId`), `security/`, `storage/`.

| Quy tắc | Lý do |
|---|---|
| Controller chỉ nhận/trả **DTO (Java record)**, không bao giờ trả Entity | Tránh lộ dữ liệu (mật khẩu, email) và mass-assignment |
| Validate input bằng Bean Validation (`@Valid`, `@NotBlank`, `@Size`...) | Chặn dữ liệu sai ngay tại biên |
| Quy tắc nghiệp vụ & **kiểm tra quyền sở hữu** nằm trong Service | Tránh IDOR (sửa/xem dữ liệu của người khác) |
| Lỗi nghiệp vụ ném `ApiException` → JSON [ProblemDetail (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457) | Mọi lỗi có cùng định dạng |
| Thay đổi CSDL **chỉ** qua migration Flyway mới (`V<n>__mo_ta.sql`), không sửa migration đã chạy | Đồng bộ schema giữa các máy |
| Bảng có cạnh tranh ghi (items, exchange_requests) dùng `@Version` | Optimistic locking, tránh ghi đè lẫn nhau |
| Danh sách luôn phân trang (`PageQuery`, tối đa 50) và sort theo whitelist | Tránh truy vấn quá nặng |
| Mỗi service có unit test cho quy tắc nghiệp vụ | Đảm bảo không hồi quy |

## 🔌 Tổng quan API (Phase 1)

| Nhóm | Endpoint chính | Quyền |
|---|---|---|
| Auth | `POST /api/auth/register · login · refresh · logout · verify-email · resend-verification · forgot-password · reset-password` | Công khai |
| Danh mục | `GET /api/categories` · `POST/PUT/DELETE /api/admin/categories` | Công khai · ADMIN |
| Tin đăng | `GET /api/items` (tìm kiếm, lọc, sắp xếp) · `GET /api/items/{id}` | Công khai |
| | `POST /api/items` · `PUT/DELETE /api/items/{id}` · `POST /{id}/hide · /{id}/show` | Đã xác thực email |
| Hình ảnh | `POST /api/items/{id}/images` (multipart, tối đa 5) · `DELETE /{imageId}` · `PUT /{imageId}/primary` | Chủ tin đăng |
| Trao đổi | `POST /api/exchange-requests` · `GET /sent · /received · /{id}` · `POST /{id}/accept · reject · cancel · complete` | Đã xác thực email |
| Đánh giá | `POST /api/exchange-requests/{id}/reviews` | Người tham gia giao dịch đã hoàn tất |
| Người dùng | `GET/PUT /api/users/me` · `POST /me/avatar` · `PUT /me/password` · `GET /me/items` | Đăng nhập |
| | `GET /api/users/{id}` · `/{id}/items` · `/{id}/reviews` | Công khai (không có email/SĐT) |

Luồng trao đổi: `PENDING → ACCEPTED → COMPLETED` (hoặc `REJECTED` / `CANCELLED`). Email & SĐT của bên kia chỉ hiện khi yêu cầu đã được chấp nhận.

### 📖 Tài liệu API (Swagger UI)
Khi chạy cục bộ: **http://localhost:8080/swagger-ui.html** (OpenAPI JSON: `/v3/api-docs`).
Gọi `POST /api/auth/login` → copy `accessToken` → bấm **Authorize** và dán token để thử các API cần đăng nhập.
Tắt ở môi trường production bằng `API_DOCS_ENABLED=false` nếu không muốn công khai.

### 🛡️ Giới hạn tần suất (Rate Limiting)
Vượt giới hạn → `429 Too Many Requests` kèm header `Retry-After` (giây). Cấu hình trong `RateLimitPolicy`.

| Áp dụng cho | Giới hạn | Theo |
|---|---|---|
| Mọi request `/api/**` | 300 / phút | IP |
| Đăng nhập | 20 / 15 phút · 10 / 15 phút | IP · email |
| Đăng ký | 10 / giờ | IP |
| Xác thực mã (verify email, reset password) | 20 / 15 phút | IP |
| Gửi email (gửi lại mã, quên mật khẩu) | 10 / 15 phút | IP |
| Đổi mật khẩu | 5 / 15 phút | Người dùng |
| Upload ảnh | 30 / 10 phút | Người dùng |
| Tạo tin đăng / yêu cầu trao đổi / đánh giá | 30 / giờ (dùng chung) | Người dùng |

> Khi chạy sau reverse proxy (Nginx, Render, Railway...), đặt `FORWARD_HEADERS_STRATEGY=native` để lấy đúng IP người dùng.
> Giới hạn lưu trong bộ nhớ của từng instance; khi chạy nhiều instance cần chuyển sang Redis.

---

## 🚀 Khởi chạy dự án cục bộ (Local Development)

### 1. Yêu cầu hệ thống
- JDK 21 trở lên
- Docker Desktop & Docker Compose
- Maven (hoặc dùng `./mvnw`)

### 2. Cấu hình biến môi trường
```bash
cp .env.example .env
```
Bắt buộc đặt `JWT_SECRET` (tối thiểu 32 ký tự), ví dụ: `openssl rand -base64 48`. File `.env` được Spring Boot tự động nạp khi chạy cục bộ.

### 3. Khởi động Cơ sở dữ liệu & Mail (Docker)
```bash
docker compose up -d
```
- MySQL: cổng `DB_PORT` (mặc định 3306), phpMyAdmin: http://localhost:8088
- Mailpit (hộp thư giả lập): http://localhost:8025 — mọi email (mã xác thực sinh viên) đều hiện ở đây, không gửi ra ngoài.

### 4. Khởi chạy Backend
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / MacOS
./mvnw spring-boot:run
```

---

## 👥 Đội ngũ phát triển
- Dự án CampusFlow Team


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
| Người dùng | `GET/PUT /api/users/me` · `POST /me/avatar` · `PUT /me/password` · `PUT /me/notification-settings` · `GET /me/items` | Đăng nhập |
| | `GET /api/users/{id}` · `/{id}/items` · `/{id}/reviews` | Công khai (không có email/SĐT) |
| Thông báo | `GET /api/notifications` · `GET /unread-count` · `POST /{id}/read` · `POST /read-all` · `DELETE /{id}` | Đăng nhập |
| Nhắn tin | `POST /api/conversations` · `GET /api/conversations` · `/unread-count` · `/{id}` · `GET/POST /{id}/messages` · `POST /{id}/read` | Đăng nhập (gửi: đã xác thực email) |
| Trường đại học | `GET /api/universities` · `POST/PUT/DELETE /api/admin/universities` | Công khai · ADMIN |
| Cần tìm | `GET /api/wanted-posts` · `/{id}` · `POST /api/wanted-posts` · `PUT /{id}` · `POST /{id}/close` · `GET /{id}/matches` · `GET /api/users/me/wanted-posts` | Công khai (ghi: đã xác thực email) |
| Giao nhận QR | `POST /api/exchange-requests/{id}/handover-code` (người cho) · `POST /{id}/confirm-handover` (người nhận) | Người tham gia giao dịch |
| Đánh giá | `POST /api/exchange-requests/{id}/reviews` · `PUT /api/reviews/{id}/reply` (người bị đánh giá trả lời, 1 lần) | Người tham gia giao dịch |
| Hỗ trợ (ticket) | `POST/GET /api/support/tickets` · `/{id}` · `GET/POST /{id}/messages` · `POST /{id}/read · close` | Đăng nhập |
| Hỗ trợ (ban quản trị) | `GET/POST /api/moderation/support-tickets` · `/{id}/messages` · `POST /{id}/read · close` · `POST /api/moderation/reviews/{id}/hide · restore` | MODERATOR, ADMIN |
| Báo cáo & chặn | `POST /api/reports` · `POST/DELETE /api/blocks/{userId}` · `GET /api/blocks` | Đăng nhập (báo cáo: đã xác thực email) |
| Kiểm duyệt | `GET /api/moderation/reports` · `POST /reports/{id}/resolve · dismiss` · `POST /items/{id}/remove · restore` · `POST /users/{id}/ban` | MODERATOR, ADMIN |
| Quản trị | `GET /api/admin/stats` · `GET /api/admin/users` · `POST /users/{id}/ban · unban` · `PUT /users/{id}/moderator` · `GET /audit-logs` | ADMIN |

Luồng trao đổi: `PENDING → ACCEPTED → COMPLETED` (hoặc `REJECTED` / `CANCELLED`). Email & SĐT của bên kia chỉ hiện khi yêu cầu đã được chấp nhận.

### 🔔 Thông báo
Mỗi thay đổi trạng thái giao dịch phát ra một domain event (`ExchangeEvent`, `ReviewCreatedEvent`); `NotificationEventListener`
tạo thông báo **trong cùng transaction** (hành động bị rollback thì thông báo cũng không tồn tại).
Các loại quan trọng (yêu cầu mới, được chấp nhận, bị hủy) được gửi thêm email sau khi commit; người dùng có thể tắt email.
Job dọn dẹp chạy lúc 03:00 mỗi đêm: xóa thông báo đã đọc quá 90 ngày, refresh token và mã xác thực đã hết hạn.

### 🎓 Tính năng dành riêng cho sinh viên
- **Trường đại học từ email**: khi đăng ký, trường được nhận diện theo tên miền email (khớp hậu tố dài nhất,
  vd. `student.hcmus.edu.vn` → HCMUS). Lọc tin cùng trường: `GET /api/items?sameUniversity=true` hoặc `?universityId=`.
  Admin thêm trường mới thì sinh viên đã đăng ký với tên miền đó được gán tự động.
- **Cần tìm**: đăng món mình cần (từ khóa, danh mục, giá tối đa, chỉ cùng trường). Khi có tin mới phù hợp, người đăng
  được thông báo (trong app + email), mỗi món chỉ một lần. So khớp không phân biệt dấu ("giai tich" khớp "Giải tích"),
  theo nguyên từ (số được giữ: "giải tích 2" không khớp "giải tích 1"). Tin tự đóng sau 30 ngày.
- **Xác nhận giao nhận bằng QR**: sau khi chấp nhận, người cho tạo mã 8 ký tự + ảnh QR (hết hạn 10 phút, chỉ lưu
  hash, tối đa 5 lần nhập sai). Người nhận quét QR/nhập mã → giao dịch hoàn tất với nhãn **"Đã xác nhận gặp mặt"**.

### 🛡️ An toàn & kiểm duyệt
- Người dùng **báo cáo** tin đăng/người dùng (mỗi người chỉ một báo cáo đang mở cho mỗi đối tượng) và **chặn** người khác
  (chặn hai chiều: không nhắn tin, không gửi yêu cầu trao đổi).
- Kiểm duyệt viên xử lý hàng đợi báo cáo: **gỡ tin** (trạng thái `REMOVED`, chủ tin không tự hiển thị lại được, các giao dịch
  liên quan bị hủy và mọi người được thông báo) hoặc **khóa tài khoản**. Xử lý một báo cáo sẽ đóng luôn các báo cáo trùng.
- **Khóa tài khoản có hiệu lực ngay**: access token còn hạn cũng bị từ chối (kiểm tra trạng thái tài khoản mỗi request,
  cache 60 giây và xóa cache khi khóa), refresh token bị thu hồi, WebSocket không kết nối được.
- **Tranh chấp đánh giá**: người bị đánh giá **trả lời công khai** một lần; ai cũng có thể **báo cáo đánh giá** sai sự thật
  (`targetType=REVIEW`). Kiểm duyệt viên có thể **ẩn đánh giá** (`HIDE_REVIEW`) — đánh giá bị ẩn không hiển thị và
  **không tính vào điểm uy tín** (điểm được tính lại), có thể khôi phục.
- **Ticket hỗ trợ**: người dùng trao đổi với ban quản trị (tối đa 3 ticket mở); ban quản trị cũng có thể **chủ động mở ticket**
  với một người dùng để làm rõ (vd. hỏi cả hai bên của một đánh giá bị báo cáo). Người dùng chỉ thấy tên
  "Ban quản trị CampusFlow", không lộ danh tính kiểm duyệt viên. Tin nhắn đẩy realtime qua `/user/queue/support`,
  người dùng nhận thông báo + email khi ban quản trị trả lời.
- Mọi thao tác kiểm duyệt / quản trị được ghi vào **nhật ký (audit log)**.
- Tài khoản ADMIN đầu tiên được cấp trực tiếp trong CSDL:
  `INSERT INTO user_roles (user_id, role_id) SELECT <id>, id FROM roles WHERE name = 'ROLE_ADMIN';`

### 💬 Nhắn tin thời gian thực (WebSocket)
- Lệnh (mở cuộc trò chuyện, gửi tin) đi qua **REST** để dùng chung validate, rate limit, phân quyền.
- **WebSocket (STOMP)** tại `/ws` chỉ dùng để **đẩy** dữ liệu mới tới client:
  - `/user/queue/messages` — tin nhắn mới · `/user/queue/notifications` — thông báo mới
- Gửi JWT trong frame CONNECT: header `Authorization: Bearer <accessToken>`. Chỉ được subscribe vào queue của chính mình;
  gửi (SEND) qua WebSocket bị từ chối.

```js
// Ví dụ client (thư viện @stomp/stompjs)
const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  onConnect: () => {
    client.subscribe('/user/queue/messages', m => showMessage(JSON.parse(m.body)));
    client.subscribe('/user/queue/notifications', m => showNotification(JSON.parse(m.body)));
  },
});
client.activate();
```

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

### 5. Lưu trữ ảnh
- Mặc định (`STORAGE_TYPE=local`): ảnh lưu trong thư mục `uploads/`, phục vụ tại `/uploads/**`.
- Production nên dùng `STORAGE_TYPE=cloudinary` + `CLOUDINARY_URL` (ảnh qua CDN, không mất khi deploy lại).
  Ảnh luôn được kiểm tra, resize và xóa metadata (GPS) trước khi lưu, dù dùng loại lưu trữ nào.

### 6. Triển khai
Xem **[DEPLOYMENT.md](DEPLOYMENT.md)**: biến môi trường, Docker Compose cho VPS, Render/Railway, tạo admin đầu tiên,
checklist an toàn. Health check: `GET /actuator/health`.

### 7. Chạy kiểm thử
```bash
./mvnw test        # Windows: .\mvnw.cmd test
```
- **Unit test**: kiểm tra quy tắc nghiệp vụ từng service (không cần CSDL).
- **Integration test** (`*IntegrationTest`): chạy toàn bộ ứng dụng với MySQL thật trong Docker (Testcontainers) —
  cần Docker Desktop đang chạy; nếu không có Docker các test này tự động được bỏ qua.
- **CI**: GitHub Actions (`.github/workflows/ci.yml`) chạy toàn bộ test cho mỗi push / pull request vào `main`, `develop`.

---

## 👥 Đội ngũ phát triển
- Dự án CampusFlow Team


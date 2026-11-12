# 🚀 Hướng dẫn triển khai CampusFlow Backend

Tài liệu này mô tả cách đưa backend lên môi trường thật (demo bảo vệ luận văn hoặc chạy cho người dùng).
Ứng dụng đóng gói sẵn bằng **Docker** (`Dockerfile`), chạy với profile `prod` và lấy toàn bộ cấu hình bí mật từ
**biến môi trường** — không có bí mật nào nằm trong mã nguồn.

---

## 1. Chọn nơi triển khai

| Lựa chọn | Phù hợp khi | Lưu ý |
|---|---|---|
| **VPS + Docker Compose** (DigitalOcean, Vultr, VPS Việt Nam...) | Muốn toàn quyền, chi phí cố định | Tự lo backup CSDL, HTTPS (Nginx/Caddy) |
| **Render / Railway / Fly.io** (PaaS) | Muốn nhanh, không quản trị server | Ổ đĩa không bền → **bắt buộc dùng Cloudinary** cho ảnh; MySQL dùng dịch vụ quản lý (Railway MySQL, Aiven...) |

> Ảnh: trên PaaS ổ đĩa bị xóa mỗi lần deploy, nên đặt `STORAGE_TYPE=cloudinary`. Trên VPS có thể giữ `local`
> (file nằm trong Docker volume `uploads`) hoặc vẫn dùng Cloudinary để có CDN.

---

## 2. Biến môi trường

Sao chép `.env.example` thành `.env` (VPS) hoặc khai báo trong trang cấu hình của PaaS.

### Bắt buộc
| Biến | Ý nghĩa |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Kết nối MySQL 8 |
| `JWT_SECRET` | Khóa ký JWT, **tối thiểu 32 ký tự**, tạo bằng `openssl rand -base64 48`. Đổi khóa = mọi người phải đăng nhập lại |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`, `MAIL_FROM` | SMTP thật để gửi mã xác thực (Gmail App Password, Brevo, Mailgun...) |
| `CORS_ALLOWED_ORIGINS` | Địa chỉ frontend, vd. `https://campusflow.vercel.app` |

### Nên đặt
| Biến | Giá trị gợi ý |
|---|---|
| `STORAGE_TYPE` / `CLOUDINARY_URL` | `cloudinary` / `cloudinary://API_KEY:API_SECRET@CLOUD_NAME` (Cloudinary Dashboard → API Keys) |
| `FORWARD_HEADERS_STRATEGY` | `native` (mặc định ở profile prod) khi chạy sau Nginx/PaaS — để rate limit lấy đúng IP người dùng. Đặt `none` nếu app nhận request trực tiếp |
| `API_DOCS_ENABLED` | `false` (mặc định ở prod). Đặt `true` tạm thời nếu muốn mở Swagger khi demo |
| `STUDENT_EMAIL_DOMAINS` | `edu.vn` |

---

## 3. Triển khai trên VPS (Docker Compose)

```bash
git clone https://github.com/<tài-khoản>/campusflow-backend.git && cd campusflow-backend
cp .env.example .env        # điền giá trị thật (xem mục 2), DB_PASSWORD và DB_ROOT_PASSWORD mạnh
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps   # app phải ở trạng thái "healthy"
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

- Flyway tự tạo/cập nhật bảng khi app khởi động; không cần chạy SQL thủ công.
- MySQL **không** mở cổng ra Internet; chỉ app (cổng `SERVER_PORT`, mặc định 8080).
- Đặt **Nginx hoặc Caddy** phía trước để có HTTPS (Caddy tự cấp chứng chỉ Let's Encrypt). WebSocket `/ws` cần
  chuyển tiếp header `Upgrade`/`Connection` (Caddy tự làm; với Nginx thêm `proxy_set_header Upgrade $http_upgrade;`
  và `proxy_set_header Connection "upgrade";`).
- Backup CSDL hằng ngày, ví dụ:
  `docker exec campusflow-prod-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" campusflow_db' > backup.sql`

Cập nhật phiên bản mới: `git pull && docker compose -f docker-compose.prod.yml up -d --build`.

---

## 4. Triển khai trên Render / Railway

1. Tạo **MySQL 8** (Railway MySQL plugin hoặc Aiven free tier) → lấy host, port, database, user, password.
2. Tạo **Web Service** từ repo GitHub, chọn **Docker** (dùng `Dockerfile` có sẵn).
3. Khai báo biến môi trường ở mục 2, kèm `STORAGE_TYPE=cloudinary` và `CLOUDINARY_URL`.
4. Health check path: `/actuator/health`.
5. Deploy. Lần đầu khởi động mất ~1 phút (Flyway chạy migration).

---

## 5. Sau khi triển khai

**Tạo tài khoản ADMIN đầu tiên** (chưa có API nào tạo admin, để tránh tự nâng quyền):
1. Đăng ký một tài khoản bằng email sinh viên như bình thường.
2. Chạy trên CSDL:
   ```sql
   INSERT INTO user_roles (user_id, role_id)
   SELECT u.id, r.id FROM users u, roles r
   WHERE u.email = 'email-cua-ban@student.xxx.edu.vn' AND r.name = 'ROLE_ADMIN';
   ```
3. Đăng nhập lại để token có quyền ADMIN. Từ đó admin có thể cấp quyền MODERATOR qua API.

**Checklist an toàn trước khi công bố**
- [ ] `JWT_SECRET` ngẫu nhiên, dài, không trùng môi trường dev
- [ ] Mật khẩu MySQL mạnh, MySQL không mở cổng ra ngoài
- [ ] HTTPS bật (Nginx/Caddy hoặc PaaS)
- [ ] `CORS_ALLOWED_ORIGINS` chỉ chứa địa chỉ frontend thật
- [ ] `API_DOCS_ENABLED=false` (hoặc chỉ bật khi demo)
- [ ] SMTP thật hoạt động (đăng ký thử và nhận được mã)
- [ ] `FORWARD_HEADERS_STRATEGY` đúng với hạ tầng (xem mục 2)
- [ ] Có lịch backup CSDL

---

## 6. Giới hạn hiện tại khi chạy nhiều instance

Ứng dụng được thiết kế cho **một instance** (đủ cho luận văn và quy mô một trường). Nếu chạy nhiều instance cần:
- **Rate limit**: chuyển bucket từ bộ nhớ sang Redis (Bucket4j hỗ trợ sẵn).
- **WebSocket**: thay simple broker bằng broker ngoài (RabbitMQ STOMP relay) để tin nhắn tới đúng instance.
- **Cache trạng thái tài khoản**: hiện TTL 60 giây; khóa tài khoản có hiệu lực ngay trên instance xử lý, các
  instance khác chậm tối đa 60 giây.
- **Ảnh**: dùng Cloudinary (không dùng ổ đĩa local).

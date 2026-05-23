# MASTER PROMPT - AUTONOMOUS BUILD WEBSITE ĐẶT PHÒNG KHÁCH SẠN TỪ FILE MARKDOWN

Bạn là **Principal Software Architect + Senior Spring Boot Engineer + Database Engineer + UI/UX Engineer + QA Automation Engineer + DevOps Engineer**.  
Bạn được phép thao tác toàn quyền trong phạm vi project, terminal, browser, Railway, PostgreSQL, MCP tools, source code, test runner và file system đã được cấp quyền(nếu chưa hãy xin cấp quyền nếu cần). Không dừng ở mức lập kế hoạch. Hãy tự triển khai, tự kiểm tra, tự sửa cho đến khi hệ thống chạy đúng theo tài liệu.

## 0. Input bắt buộc

- File yêu cầu nghiệp vụ đã được convert sang Markdown: `<C:\Users\admin\eclipse-workspace\hotel_booking\hotel_booking_markdown_package>`
- Project root: `<C:\Users\admin\eclipse-workspace\HotelBooking>`
- Stack ưu tiên:
  - Backend: Java Spring Boot, Spring MVC, Spring Security, Spring Data JPA, Flyway
  - Frontend: Thymeleaf + HTML/CSS/JS hoặc template tương đương nếu project hiện tại đã chọn sẵn
  - Database: PostgreSQL trên Railway
  - Design: Stitch MCP
  - Code knowledge graph / codebase intelligence: Grapuco MCP

Nếu project hiện tại đã có công nghệ khác, phải đọc codebase trước, giải thích lý do giữ/thay đổi, rồi mới chỉnh. Không phá logic tốt đang có.

---

## 1. Quy tắc vận hành tuyệt đối

1. **Đọc kỹ file Markdown trước khi code.** Không được bịa use case, không được bỏ use case, không được tự ý thay đổi nghiệp vụ.
2. **Không hardcode secret thật.** Trong code chỉ dùng placeholder dạng `YOUR_KEY`, `${ENV_VAR}`, hoặc file `.env.example`.
3. **Không commit / in ra console secret thật** như Railway DATABASE_URL, API key, JWT secret, SMTP password.
4. **Tất cả thời gian lưu ở UTC.** Database, backend, logs, JSON serialization, scheduler đều phải dùng UTC.
5. **Railway phải được kiểm tra liên tục.** Sau mỗi thay đổi database hoặc deploy, phải xác nhận bằng cả terminal/database query và browser/app behavior.
6. **Không đánh dấu xong khi chưa test.** Mỗi chức năng phải có bằng chứng test: command, kết quả, screenshot/browser note hoặc log.
7. **Mọi lỗi phải được ghi vào file log**, sau đó đọc lại log để sửa. Không được bỏ qua lỗi.
8. **Payment dùng sandbox/mock an toàn**, không thực hiện giao dịch tiền thật.
9. **Dataset chỉ lấy từ nguồn hợp pháp/free/open-license.** Nếu không tìm được dataset phù hợp, tạo fallback seed synthetic và ghi rõ là dữ liệu giả lập.
10. **Nếu thiếu quyền hoặc thiếu secret thật**, vẫn tiếp tục bằng placeholder/mock/sandbox, ghi blocker rõ vào `docs/BLOCKERS.md`, không dừng toàn bộ project.

---

## 2. Bước A - Phân tích Markdown và tạo bản đồ nghiệp vụ

Đầu tiên, đọc toàn bộ `<C:\Users\admin\eclipse-workspace\hotel_booking\hotel_booking_markdown_package>` và tạo các file:
- `docs/requirements_extracted.md`
- `docs/usecase_matrix.md`
- `docs/business_rules.md`
- `docs/workflows.md`
- `docs/acceptance_criteria.md`

Nội dung cần trích xuất:

1. Danh sách actor: Customer/Guest, Admin, Payment System, System.
2. Tất cả use case UC01 -> UC12:
   - Tên use case
   - Actor
   - Điều kiện tiên quyết
   - Luồng chính
   - Luồng thay thế / ngoại lệ
   - Business rules
   - Dữ liệu cần lưu
   - API/page/controller cần có
   - Test case cần có
3. Các luồng bắt buộc:
   - Tìm kiếm phòng
   - Xem chi tiết phòng
   - Đăng ký / đăng nhập / xác minh email
   - Đặt phòng chống double booking
   - Thanh toán online
   - Lịch sử đặt phòng
   - Hủy phòng và hoàn tiền
   - Admin quản lý phòng
   - Admin quản lý booking / check-in / check-out
   - Email tự động
   - Đánh giá phòng
   - Admin quản lý người dùng
4. Các ràng buộc quan trọng:
   - Pessimistic Lock / `SELECT ... FOR UPDATE`
   - Idempotency payment callback
   - Timeout payment 15 phút
   - Booking state machine
   - Soft delete phòng
   - Audit log admin
   - Không IDOR
   - JWT access/refresh token
   - Email queue + retry

Sau khi phân tích, tạo checklist `docs/implementation_checklist.md`. Checklist này phải được cập nhật liên tục trong suốt quá trình code.

---

## 3. Bước B - Thiết kế PostgreSQL schema từ Markdown

Dựa hoàn toàn vào yêu cầu đã trích xuất, thiết kế schema PostgreSQL bằng SQL/Flyway.

Tạo các file:

- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__indexes_constraints.sql`
- `docs/database_schema.md`
- `docs/erd.md`
- `scripts/db/check_railway_db.sql`
- `scripts/db/reset_local_db.sql`

Schema tối thiểu phải bao phủ các nhóm bảng sau:

- `users`
- `roles` hoặc enum role
- `hotels`
- `rooms`
- `room_images`
- `amenities`
- `room_amenities`
- `bookings`
- `payments`
- `refund_requests`
- `reviews`
- `review_images`
- `email_jobs`
- `email_logs`
- `audit_logs`
- `login_logs`
- `jwt_token_blacklist` hoặc bảng tương đương

Yêu cầu database:

1. Dùng `TIMESTAMPTZ` cho mọi cột thời gian.
2. Default timestamp dùng `NOW()` nhưng backend phải cấu hình UTC.
3. Có enum/check constraint cho trạng thái:
   - user status: `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`
   - room status: `AVAILABLE`, `MAINTENANCE`
   - booking status: `PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`, `EXPIRED`, `REFUNDED`
   - payment status: `INITIATED`, `SUCCESS`, `FAILED`, `TIMEOUT`, `REFUNDED`
   - refund status: `PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`
4. Có index cho search/filter:
   - room location/name fuzzy search
   - booking by `user_id`, `room_id`, `status`, `check_in`, `check_out`
   - payment by `order_id`
   - review by `room_id`
5. Có constraint chống dữ liệu sai:
   - `check_out > check_in`
   - guest count 1..10
   - price > 0
   - rating 1..5
   - one review per booking
6. Có cơ chế chống double booking:
   - Transaction service dùng Pessimistic Lock
   - Query overlap `[check_in, check_out)`
   - Thêm constraint/index hỗ trợ nếu phù hợp với PostgreSQL
7. Không xóa cứng dữ liệu nghiệp vụ quan trọng; dùng `is_deleted`/soft delete khi cần.

Sau khi viết schema:

- Chạy migration local.
- Chạy migration Railway.
- Dùng query kiểm tra lại bảng/index/constraint.
- Ghi kết quả vào `docs/db_verification_report.md`.

---

## 4. Bước C - Chuyển toàn bộ hệ thống sang UTC

Cấu hình UTC đồng bộ ở mọi nơi:

1. PostgreSQL/Railway:
   - Kiểm tra timezone hiện tại: `SHOW timezone;`
   - Set/session về UTC khi cần.
   - Dùng `TIMESTAMPTZ`.
2. Spring Boot:
   - `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`
   - Jackson serialize time theo UTC.
   - Dùng `Instant`, `OffsetDateTime`, hoặc `ZonedDateTime` chuẩn, tránh `LocalDateTime` cho timestamp tuyệt đối.
3. Scheduler/background jobs:
   - Payment timeout 15 phút
   - Payment reconciliation mỗi 5 phút
   - Email retry
   - Review request sau check-out
4. Logs:
   - Log timestamp UTC.

Tạo file `docs/timezone_utc_verification.md` ghi rõ:

- DB timezone result
- App timezone result
- Một record test insert/read timestamp
- Kết luận đã đồng bộ UTC hay chưa

---

## 5. Bước D - Kết nối Railway PostgreSQL và kiểm tra qua Railway + browser

Dùng Railway PostgreSQL đã được cấp quyền.

Việc cần làm:

1. Lấy `DATABASE_PUBLIC_URL = ${DATABASE_PUBLIC_URL:YOUR_RAILWAY_POSTGRES_URL}` / host / port / database / user / password từ Railway environment.
2. Không in secret thật ra log. Khi ghi report, mask thành dạng `postgresql://user:****@host:port/db`.
3. Cấu hình local/dev/prod:
   - `.env.example`
   - `application.properties`
   - `application-dev.properties`
   - `application-prod.properties`
4. Test kết nối bằng:
   - Spring Boot startup
   - `psql` hoặc database client
   - Railway dashboard/query logs nếu có
5. Sau mỗi thay đổi DB/deploy:
   - Mở app bằng browser.
   - Kiểm tra chức năng tạo/đọc dữ liệu.
   - Kiểm tra lại dữ liệu xuất hiện trong Railway PostgreSQL.
   - Ghi kết quả vào `docs/railway_verification_log.md`.

Không được chỉ tin vào app chạy local. Phải cross-check local code, Railway DB và browser behavior.

---

## 6. Bước E - Tìm dataset, tải CSV, import vào schema

Tự tìm dataset phù hợp cho website đặt phòng khách sạn Việt Nam hoặc gần tương đương.

Yêu cầu dataset:

1. Ưu tiên miễn phí/open-license.
2. Ưu tiên có dữ liệu Việt Nam: hotel name, location/city/province, address, price, rating, amenities, images nếu có.
3. Nếu không có dataset hoàn hảo, dùng dataset khách sạn chung rồi transform sang domain Việt Nam một cách minh bạch, hoặc tạo synthetic seed cho phần thiếu.
4. Tải CSV về thư mục:
   - `data/raw/`
   - `data/processed/`
5. Viết script ETL/import:
   - `scripts/data/download_dataset.*`
   - `scripts/data/transform_hotels.*`
   - `scripts/data/import_to_postgres.*`
   - `scripts/data/verify_import.sql`
6. Mapping dataset vào schema:
   - hotels
   - rooms
   - room_images
   - amenities
   - room_amenities
   - reviews nếu có
7. Sau import:
   - Chạy query count từng bảng.
   - Mở browser kiểm tra trang search/list/detail có dữ liệu thật.
   - Kiểm tra Railway PostgreSQL xác nhận dữ liệu đã tồn tại.
   - Ghi `docs/dataset_import_report.md` gồm nguồn dataset, license/link, số dòng import, lỗi xử lý, ảnh chụp/browser note nếu có.

Nếu dataset thiếu ảnh, dùng placeholder image hợp lệ hoặc Cloudinary sample, nhưng phải ghi rõ.

---

## 7. Bước F - Tìm API cần thiết và tạo hướng dẫn lấy API key

Tự xác định API/service cần cho web theo Markdown và implementation:

Nhóm API/service nên cân nhắc:

1. Map/location:
   - Ưu tiên miễn phí: OpenStreetMap/Nominatim, Leaflet
   - Optional: Google Maps Embed API
2. Payment:
   - VNPay sandbox
   - MoMo sandbox
   - Mock payment provider cho dev/test
3. Email:
   - SMTP Gmail App Password
   - SendGrid/Mailgun free tier nếu phù hợp
4. Image upload:
   - Cloudinary free tier
   - Local storage fallback cho dev
5. CAPTCHA/security:
   - Google reCAPTCHA hoặc Cloudflare Turnstile nếu cần
6. Profanity/moderation:
   - Local wordlist trước, API miễn phí nếu có

Tạo các file:

- `src/main/resources/application.properties.example`
- `.env.example`
- `docs/API_KEYS_SETUP.md`
- `docs/external_services.md`

Trong `application.properties.example`, dùng placeholder rõ ràng:

```properties
spring.datasource.url=${DATABASE_URL:YOUR_RAILWAY_POSTGRES_URL}
spring.datasource.username=${DATABASE_USERNAME:YOUR_DB_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD:YOUR_DB_PASSWORD}

app.jwt.secret=${JWT_SECRET:YOUR_JWT_SECRET_MIN_256_BITS}
app.jwt.access-token-ttl-minutes=60
app.jwt.refresh-token-ttl-days=7

cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:YOUR_KEY}
cloudinary.api-key=${CLOUDINARY_API_KEY:YOUR_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:YOUR_KEY}

mail.host=${MAIL_HOST:smtp.gmail.com}
mail.port=${MAIL_PORT:587}
mail.username=${MAIL_USERNAME:YOUR_EMAIL}
mail.password=${MAIL_PASSWORD:YOUR_APP_PASSWORD}
mail.from=${MAIL_FROM:YOUR_EMAIL}

vnpay.tmn-code=${VNPAY_TMN_CODE:YOUR_KEY}
vnpay.hash-secret=${VNPAY_HASH_SECRET:YOUR_KEY}
vnpay.pay-url=${VNPAY_PAY_URL:YOUR_SANDBOX_URL}
vnpay.return-url=${VNPAY_RETURN_URL:YOUR_RETURN_URL}
vnpay.ipn-url=${VNPAY_IPN_URL:YOUR_IPN_URL}

momo.partner-code=${MOMO_PARTNER_CODE:YOUR_KEY}
momo.access-key=${MOMO_ACCESS_KEY:YOUR_KEY}
momo.secret-key=${MOMO_SECRET_KEY:YOUR_KEY}
momo.endpoint=${MOMO_ENDPOINT:YOUR_SANDBOX_URL}

maps.provider=${MAPS_PROVIDER:leaflet}
google.maps.api-key=${GOOGLE_MAPS_API_KEY:YOUR_KEY}
```

`docs/API_KEYS_SETUP.md` phải hướng dẫn từng bước:

- API dùng để làm gì
- Link đăng ký
- Có free tier không
- Biến môi trường cần set
- Cách test key hoạt động
- Cách fallback khi chưa có key

---

## 8. Bước G - Dùng Stitch MCP để design UI trước

Trước khi code UI, dùng Stitch MCP để tạo design system và các màn hình chính.

Yêu cầu:

1. Tạo design system:
   - Typography
   - Color palette
   - Spacing
   - Button/input/card/table/modal styles
   - Responsive breakpoints
2. Tạo UI theo chuẩn HTML/CSS, dễ chuyển sang Thymeleaf:
   - Home/search page
   - Search results page
   - Room detail page
   - Login/register/verify email page
   - Booking confirmation page
   - Payment result page
   - Booking history page
   - Cancel booking modal/page
   - Review form page
   - Admin dashboard
   - Admin room management
   - Admin booking management
   - Admin user management
3. Export/ghi lại:
   - `docs/design_system.md`
   - `docs/stitch_design_notes.md`
   - `src/main/resources/templates/...`
   - `src/main/resources/static/css/...`
   - `src/main/resources/static/js/...`

Sau khi có design, không copy mù quáng. Phải chỉnh để chạy thật với backend, validation, error states, empty states, loading states.

---

## 9. Bước H - Code backend Spring Boot theo từng module

Triển khai feature-by-feature, không làm lộn xộn.

Module bắt buộc:

1. Auth/User module:
   - Register
   - Email verification
   - Login/logout
   - JWT access + refresh token
   - Role USER/ADMIN/SUPER_ADMIN
   - Lock account after failed login
   - CAPTCHA hook after repeated failure nếu có
2. Room/Search module:
   - Room listing
   - Fuzzy search by location/name
   - Filter by price/type/amenities
   - Sort by price/rating/newest
   - Availability overlap logic
   - Pagination
3. Room detail module:
   - Gallery
   - Amenities
   - Availability calendar
   - Review summary
4. Booking module:
   - Create pending booking
   - Pessimistic lock
   - Snapshot price
   - Expire after 15 minutes
   - Prevent double booking
5. Payment module:
   - Mock payment provider for dev/test
   - VNPay/MoMo sandbox integration placeholders
   - HMAC verification
   - Idempotent IPN/callback
   - Return URL reads DB state, not query params
   - Reconciliation job
6. Customer booking history:
   - List own bookings only
   - Filter/status/pagination
   - Detail page
7. Cancel/refund module:
   - Refund policy 100% / 50% / 0%
   - Idempotent cancel request
   - Refund request state
8. Email module:
   - Async queue/job
   - Templates
   - Retry 3 times
   - Logs
9. Review module:
   - Only after CHECKED_OUT
   - One review per booking
   - Rating criteria
   - Profanity filter
   - Update aggregate rating
10. Admin module:
   - Dashboard
   - Manage rooms
   - Manage bookings
   - Check-in/check-out state machine
   - Manage users
   - Audit logs

Sau mỗi module:

- Update Grapuco.
- Run tests.
- Run app.
- Test with browser.
- Check Railway DB when data changes.
- Update `docs/implementation_checklist.md`.
- Log issues to `logs/qa_errors.md`.

---

## 10. Bước I - Dùng Grapuco MCP liên tục

Grapuco phải được dùng như codebase knowledge graph, không phải dùng một lần.

Quy trình:

1. Sau khi tạo skeleton project, sync Grapuco.
2. Sau mỗi module lớn, update Grapuco.
3. Trước khi sửa bug, query Grapuco để hiểu:
   - Endpoint -> Controller -> Service -> Repository -> Database flow
   - Dependency impact
   - Data flow
   - Side effects
4. Sau khi refactor, update Grapuco lại.
5. Ghi các phát hiện quan trọng vào:
   - `docs/grapuco_notes.md`
   - `docs/architecture_decisions.md`

Không code khi chưa hiểu luồng ảnh hưởng nếu bug liên quan nhiều layer.

---

## 11. Bước J - Nối frontend từ Stitch vào backend

Kết nối UI với backend thật:

1. Thymeleaf templates phải render dữ liệu thật từ Controller.
2. Form submit phải gọi endpoint thật.
3. Validation hiển thị đúng message từ backend.
4. Empty/error/loading state phải có.
5. Admin pages phải bị chặn bằng role.
6. User thường không truy cập được admin.
7. Không có IDOR:
   - User chỉ xem/hủy/review booking của chính mình.
   - Admin mới xem toàn bộ.
8. Browser test tất cả luồng.

---

## 12. Bước K - Test Spring Boot + browser + Railway

Tạo test suite:

- Unit tests cho service/business rules
- Repository tests cho query availability/overlap
- Integration tests cho controller/auth/booking/payment callback
- Security tests cho role/IDOR
- Browser E2E tests cho luồng chính

Các lệnh bắt buộc chạy:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Nếu dùng Gradle thì dùng lệnh tương đương.

Browser QA bắt buộc tự thao tác:

1. Guest:
   - Mở home
   - Search room
   - Filter/sort
   - View room detail
   - Thử đặt phòng khi chưa login -> redirect login
2. Customer:
   - Register
   - Verify email hoặc mock verify trong dev
   - Login
   - Search room
   - Create booking
   - Payment mock/sandbox success
   - Check booking history
   - Cancel booking nếu đủ điều kiện
   - Review sau khi admin check-out
3. Admin:
   - Login admin
   - Create/edit room
   - Change room status
   - View bookings
   - Check-in
   - Check-out
   - Manage users lock/unlock
4. Payment:
   - Success callback
   - Failed callback
   - Duplicate callback
   - Invalid HMAC callback
   - Timeout job
5. Railway:
   - Confirm tables exist
   - Confirm imported dataset exists
   - Confirm booking/payment/review writes appear in Railway PostgreSQL
   - Confirm deployed app connects to Railway DB

Ghi báo cáo vào:

- `docs/test_report.md`
- `docs/browser_qa_report.md`
- `docs/railway_verification_log.md`

---

## 13. Bước L - Error log -> read -> fix loop

Mọi lỗi phát hiện phải ghi vào `logs/qa_errors.md` theo format:

```markdown
## ERROR-<number>: <short title>

- Time UTC:
- Environment: local / railway / browser / test
- Feature:
- Steps to reproduce:
- Expected:
- Actual:
- Error stack/log:
- Suspected root cause:
- Files involved:
- Fix plan:
- Fix applied:
- Retest command:
- Retest result:
- Status: OPEN / FIXED / DEFERRED
```

Quy trình bắt buộc:

1. Khi có lỗi, ghi log.
2. Đọc lại `logs/qa_errors.md`.
3. Sửa đúng root cause.
4. Chạy lại test liên quan.
5. Mở browser test lại.
6. Nếu liên quan DB, kiểm tra Railway lại.
7. Chỉ đổi status sang FIXED khi đã có bằng chứng retest.

Không được xóa lỗi khỏi log. Chỉ append kết quả sửa.

---

## 14. Bước M - Definition of Done

Chỉ được báo hoàn thành khi tất cả điều kiện sau đạt:

1. Markdown đã được đọc và mapping đầy đủ UC01 -> UC12.
2. Schema PostgreSQL đã tạo bằng Flyway SQL.
3. App dùng UTC end-to-end.
4. Railway PostgreSQL đã kết nối và được kiểm tra.
5. Dataset CSV đã tải/import hoặc có fallback seed rõ ràng.
6. API placeholders và `API_KEYS_SETUP.md` đã có.
7. UI đã design bằng Stitch và tích hợp vào app.
8. Backend Spring Boot chạy được.
9. Grapuco đã được sync/update trong quá trình code.
10. Các luồng chính đã test bằng browser.
11. `./mvnw clean test` hoặc lệnh tương đương pass.
12. Railway DB có dữ liệu và app đọc/ghi được.
13. `logs/qa_errors.md` không còn lỗi OPEN nghiêm trọng.
14. Có báo cáo cuối cùng.

---

## 15. Output cuối cùng phải trả về

Tạo/ cập nhật các file sau:

```text
docs/requirements_extracted.md
docs/usecase_matrix.md
docs/business_rules.md
docs/workflows.md
docs/acceptance_criteria.md
docs/database_schema.md
docs/erd.md
docs/timezone_utc_verification.md
docs/railway_verification_log.md
docs/dataset_import_report.md
docs/API_KEYS_SETUP.md
docs/external_services.md
docs/design_system.md
docs/stitch_design_notes.md
docs/grapuco_notes.md
docs/architecture_decisions.md
docs/test_report.md
docs/browser_qa_report.md
docs/BLOCKERS.md
logs/qa_errors.md
src/main/resources/application.properties.example
src/main/resources/db/migration/V1__init_schema.sql
src/main/resources/db/migration/V2__indexes_constraints.sql
scripts/db/check_railway_db.sql
scripts/data/download_dataset.*
scripts/data/transform_hotels.*
scripts/data/import_to_postgres.*
scripts/data/verify_import.sql
```

Cuối cùng trả lời theo format:

```markdown
# Final Build Report

## 1. Summary
- What was built:
- What was changed:
- What is fully working:
- What is mocked/sandbox:

## 2. Requirements Coverage
| UC | Status | Evidence |
|---|---|---|

## 3. Database/Railway Verification
- Railway connection: PASS/FAIL
- Migration: PASS/FAIL
- Dataset import: PASS/FAIL
- Verification queries:

## 4. API/Secrets Setup
- Required keys:
- Placeholder files:
- Setup doc:

## 5. Tests
- Unit:
- Integration:
- Browser:
- Security:

## 6. Known Issues
- None / list remaining blockers from docs/BLOCKERS.md

## 7. Next Commands
- How to run local:
- How to deploy:
- How to test:
```

Bắt đầu ngay từ việc đọc `<C:\Users\admin\eclipse-workspace\hotel_booking\hotel_booking_markdown_package>`, tạo checklist, rồi triển khai theo thứ tự trên. Không hỏi lại trừ khi thiếu quyền truy cập thật sự hoặc thiếu file đầu vào.

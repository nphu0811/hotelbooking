# Use Case Matrix

| UC | Name | Actor | Preconditions | Main Data | Controller/Page | Critical Tests |
|---|---|---|---|---|---|---|
| UC01 | Tìm kiếm phòng | Guest/Customer | Website accessible | hotels, rooms, bookings, amenities, reviews | `/`, `/rooms/search` | validation, overlap, filter, sort, pagination |
| UC02 | Xem chi tiết phòng | Guest/Customer | Room exists or direct URL | rooms, room_images, amenities, reviews | `/rooms/{id}` | 404, maintenance, reviews, availability |
| UC03 | Đăng ký / đăng nhập | Customer | Not logged in | users, roles, login_logs, token blacklist, email jobs | `/register`, `/login`, `/verify` | registration, login, lockout, verify |
| UC04 | Đặt phòng | Customer | Logged in, room available | bookings, payments, email jobs | `/bookings/create`, `/checkout/{id}` | pessimistic lock, expiry, unauthorized redirect |
| UC05 | Thanh toán online | Customer, Payment System | Pending booking | payments, bookings | `/payments/mock/*`, `/payments/ipn`, `/payments/return` | HMAC, idempotency, timeout, duplicate callback |
| UC06 | Lịch sử đặt phòng | Customer | Logged in | bookings, rooms, payments | `/account/bookings` | IDOR, empty state, filters |
| UC07 | Hủy đặt phòng | Customer | Own confirmed booking | bookings, refunds, emails | `/bookings/{id}/cancel` | refund policy, duplicate request, IDOR |
| UC08 | Quản lý phòng | Admin | Role ADMIN | hotels, rooms, room_images, amenities, audit_logs | `/admin/rooms` | validation, soft delete, audit |
| UC09 | Quản lý đặt phòng | Admin | Role ADMIN | bookings, users, rooms, audit_logs | `/admin/bookings` | state machine, check-in/out guards |
| UC10 | Email thông báo | System | Event trigger | email_jobs, email_logs | background worker/admin logs | retry, bounce, templates |
| UC11 | Đánh giá phòng | Customer | Own checked-out booking | reviews, review_images, rooms | `/bookings/{id}/review` | one review per booking, profanity, rating |
| UC12 | Quản lý người dùng | Admin/SUPER_ADMIN | Role ADMIN | users, roles, login_logs, token blacklist | `/admin/users` | lock/unlock, role elevation, audit |

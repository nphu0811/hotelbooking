# Requirements Extracted

Source: `hotel_booking_markdown_package/hotel_booking_report.md`

## Actors

- Guest/Customer: searches rooms, views details, registers/logs in, books, pays, views history, cancels, reviews.
- Admin: manages rooms, bookings, check-in/check-out, users, and audit-sensitive actions.
- Payment System: VNPay/MoMo or mock sandbox provider that sends return/IPN callbacks.
- System: runs async email jobs, payment timeout/reconciliation, review request scheduling, logging, and validation.

## Use Cases

### UC01 - Tìm Kiếm Phòng

- Actor: Guest/Customer.
- Goal: Search available rooms by location or hotel name, dates, and guest count.
- Preconditions: Customer can access website without login.
- Main flow: validate dates/guest count/stay length; fuzzy search by location/name; exclude rooms with overlapping `CONFIRMED` or `PENDING_PAYMENT` bookings; show image, price, amenities, rating, review count; filter/sort; paginate 20 rooms/page.
- Exceptions: invalid date, guest count outside 1..10, stay longer than 30 nights, no result, database error with one retry and error log.
- Data: room, hotel, amenities, room images, reviews, booking date ranges.
- Pages/controllers: home/search page, search results endpoint.
- Tests: validation boundaries, overlap exclusion, sort/filter/pagination, empty state.

### UC02 - Xem Chi Tiết Phòng

- Actor: Guest/Customer.
- Goal: View complete room information before booking.
- Preconditions: User comes from search or direct URL.
- Main flow: show gallery, description, amenities, capacity, area, cancellation policy, map, availability calendar, review list and review summary.
- Exceptions: room not found or soft-deleted returns 404; maintenance rooms show unavailable message.
- Data: room, hotel, room_images, amenities, reviews, bookings.
- Pages/controllers: room detail page.
- Tests: not found, maintenance state, review pagination, unavailable date marking.

### UC03 - Đăng Ký / Đăng Nhập

- Actor: Customer.
- Goal: Authenticate users for protected actions.
- Preconditions: Customer is not logged in.
- Main login flow: enter email/password; after 3 failures show CAPTCHA hook; validate active account; BCrypt password check; lock after 5 failures in 15 minutes; issue access token and refresh token; refresh token in HttpOnly cookie; write login log; redirect to `redirect_uri`.
- Main register flow: validate full name, email uniqueness, Vietnam phone format, strong password, confirm password; create `PENDING_VERIFICATION` account; generate email verification token valid 24 hours; send verification email; verification activates account and logs user in.
- Exceptions: generic login error, pending verification, locked account, expired verification link, weak password, duplicate pending account resends verification.
- Data: users, roles, login_logs, jwt_token_blacklist, email_jobs/logs.
- Pages/controllers: login, register, verify email, resend verification, logout, token refresh.
- Tests: validation, lockout, pending/active transitions, redirect, token/cookie behavior.

### UC04 - Đặt Phòng

- Actor: logged-in Customer.
- Goal: Create a booking safely without double booking.
- Preconditions: valid login, available room, payment system available.
- Main flow: user confirms dates and guests; system checks JWT; transaction locks room with `SELECT ... FOR UPDATE`; checks overlap `[check_in, check_out)` against `CONFIRMED` and `PENDING_PAYMENT`; creates `PENDING_PAYMENT` booking with `expires_at = now + 15 minutes`; snapshots price; user confirms payment; on payment success booking becomes `CONFIRMED`; confirmation email queued.
- Exceptions: overlap found, pending payment expired, payment failed, DB error after payment success triggers critical log and reconciliation.
- Data: bookings, payments, email_jobs/logs.
- Pages/controllers: booking create/confirm, checkout, payment result.
- Tests: overlap race protection, expiry, price snapshot, unauthorized redirect.

### UC05 - Thanh Toán Online

- Actor: Customer and Payment System.
- Goal: Process online payment safely.
- Preconditions: booking is `PENDING_PAYMENT` and not expired.
- Main flow: create order id, amount, return URL, IPN URL, signed payload; store `INITIATED` payment; redirect to provider/mock; provider sends IPN; verify HMAC; process idempotently; update payment `SUCCESS`/`FAILED`; return URL reads DB state only.
- Exceptions: missing IPN triggers 5-minute reconciliation; invalid HMAC rejected/logged; duplicate callback returns OK without reprocessing; 15-minute timeout marks payment `TIMEOUT` and booking `EXPIRED`.
- Data: payments, bookings, audit/log entries.
- Pages/controllers: payment start, IPN/callback, return page.
- Tests: success, failed, duplicate, invalid HMAC, timeout.

### UC06 - Xem Lịch Sử Đặt Phòng

- Actor: logged-in Customer.
- Goal: View own booking history.
- Preconditions: login required.
- Main flow: query by authenticated user id, sort newest first, paginate 10/page, show booking code, room, dates, total, status badge, filters and actions.
- Exceptions: empty state; expired JWT redirects with `redirect_uri`.
- Data: bookings, rooms, payments, reviews.
- Pages/controllers: customer booking history and detail.
- Tests: IDOR prevention, filters, empty state.

### UC07 - Hủy Đặt Phòng

- Actor: logged-in Customer.
- Goal: Cancel confirmed booking and request refund.
- Preconditions: booking belongs to user and is `CONFIRMED`.
- Main flow: verify ownership/state/check-in date; calculate refund rate: 100% at >=3 days, 50% at 1-2 days, 0% on check-in day; show confirmation; transaction changes booking to `CANCELLED`; create idempotent refund request by booking id; call/mock refund if amount > 0; queue email.
- Exceptions: not cancellable state, after check-in time, refund API failure, duplicate cancel request.
- Data: bookings, refund_requests, payments, email_jobs/logs.
- Pages/controllers: cancel modal/page, cancel endpoint.
- Tests: policy boundaries, duplicate idempotency, IDOR.

### UC08 - Quản Lý Phòng

- Actor: Admin.
- Goal: Manage rooms.
- Preconditions: role `ADMIN`.
- Main flow: list/filter rooms; create room; validate name/price/capacity/images; upload image or local placeholder; edit room; price changes affect future bookings only; status `AVAILABLE`/`MAINTENANCE`; soft delete if no active bookings; audit every change.
- Exceptions: duplicate room name in hotel, image upload failure, active booking prevents delete.
- Data: hotels, rooms, room_images, amenities, room_amenities, audit_logs.
- Pages/controllers: admin room management.
- Tests: role access, validation, soft delete, audit log.

### UC09 - Quản Lý Đặt Phòng

- Actor: Admin.
- Goal: Manage booking state and check-in/check-out.
- Preconditions: role `ADMIN`.
- Main flow: list/filter bookings; view detail; check-in `CONFIRMED` booking around check-in date; check-out `CHECKED_IN` booking; write actual timestamps; queue email and review request.
- Exceptions: early/late check-in/out warnings; DB update error is logged and does not change state.
- Data: bookings, users, rooms, email_jobs/logs, audit_logs.
- Pages/controllers: admin booking management.
- Tests: state machine transitions and invalid transitions.

### UC10 - Gửi Email Thông Báo

- Actor: System.
- Goal: Send event emails asynchronously.
- Preconditions: trigger from booking confirmed/cancelled, check-in, check-out, review request.
- Main flow: create email job; worker renders template; send via SMTP or provider fallback; log result; retry up to 3 times.
- Exceptions: SMTP/network timeout retry after 5 minutes; bounce marks email invalid and stops retry; complaint pauses sending.
- Data: email_jobs, email_logs, users, bookings.
- Pages/controllers: background job plus admin log visibility.
- Tests: job creation, retry limit, bounce behavior.

### UC11 - Đánh Giá Phòng Sau Check-out

- Actor: logged-in Customer.
- Goal: Review room after stay.
- Preconditions: booking belongs to user, `CHECKED_OUT`, no existing review.
- Main flow: review invitation after check-out; user submits rating 1..5, criteria scores, 50..2000 character content, up to 5 images; profanity filter; save review; update room aggregate rating.
- Exceptions: not checked out, duplicate review, profanity violation.
- Data: reviews, review_images, rooms, bookings.
- Pages/controllers: review form and submit endpoint.
- Tests: one review per booking, state/ownership checks, rating/content validation.

### UC12 - Quản Lý Người Dùng

- Actor: Admin/SUPER_ADMIN.
- Goal: Manage users, lock/unlock, assign roles.
- Preconditions: role `ADMIN`; role elevation requires `SUPER_ADMIN`.
- Main flow: list/search/filter users; view details, bookings, login history, reviews; lock user with reason; revoke JWT via blacklist; unlock and notify; SUPER_ADMIN can assign ADMIN; audit all changes.
- Exceptions: cannot lock SUPER_ADMIN; warn on users with confirmed bookings; normal admin cannot assign admin role.
- Data: users, roles, login_logs, jwt_token_blacklist, audit_logs, email_jobs/logs.
- Pages/controllers: admin user management.
- Tests: role checks, JWT blacklist, lock/unlock, audit log.

# HƯỚNG DẪN CẤU HÌNH ĐĂNG NHẬP GOOGLE & FACEBOOK (REAL)

Tài liệu này hướng dẫn cách lấy thông tin **Client ID** & **Client Secret** thật từ Google và Facebook, cũng như cấu hình chúng vào ứng dụng HotelBooking để có thể đăng nhập thực tế.

---

## 1. Cấu hình Google OAuth2

Để lấy thông tin từ Google, hãy làm theo các bước sau:

1. **Truy cập Google Cloud Console**:
   - Truy cập vào: [https://console.cloud.google.com](https://console.cloud.google.com) và đăng nhập bằng tài khoản Google của bạn.
   - Nhấp vào mục chọn dự án ở góc trên cùng bên trái và chọn **Dự án mới (New Project)**. Đặt tên cho dự án và nhấn **Tạo (Create)**.

2. **Cấu hình Màn hình đồng ý OAuth (OAuth Consent Screen)**:
   - Đi tới thanh menu bên trái, chọn **API và Dịch vụ (APIs & Services)** > **Màn hình đồng ý OAuth (OAuth consent screen)**.
   - Chọn loại người dùng là **External (Ngoài tổ chức)** và nhấp **Tạo (Create)**.
   - Điền các thông tin bắt buộc:
     - *Tên ứng dụng*: Ví dụ `HotelBooking`
     - *Email hỗ trợ người dùng*: Email của bạn
     - *Thông tin liên hệ của nhà phát triển*: Email của bạn
   - Nhấn **Lưu và tiếp tục (Save and Continue)** cho đến hết và quay lại trang tổng quan.

3. **Tạo thông tin xác thực (Credentials)**:
   - Đi tới tab **Thông tin xác thực (Credentials)** ở menu bên trái.
   - Nhấp vào **+ Tạo thông tin xác thực (+ Create Credentials)** > **ID ứng dụng khách OAuth (OAuth client ID)**.
   - Chọn loại ứng dụng là **Ứng dụng web (Web application)**.
   - Đặt tên cho Client ID này (ví dụ: `HotelBooking Web Client`).
   - Cấu hình phần **URI chuyển hướng được cấp phép (Authorized redirect URIs)**:
     - Ở môi trường Local: Thêm `http://localhost:8080/login/oauth2/code/google`
     - Ở môi trường Production (nếu có): Thêm `https://tên-miền-của-bạn.com/login/oauth2/code/google`
   - Nhấp vào **Tạo (Create)**.
   - Một cửa sổ pop-up sẽ hiển thị **ID ứng dụng khách (Client ID)** và **Mật khẩu ứng dụng khách (Client Secret)**. Hãy sao chép 2 giá trị này lại.

---

## 2. Cấu hình Facebook OAuth2

Để lấy thông tin từ Facebook, hãy làm theo các bước sau:

1. **Truy cập Facebook Developers**:
   - Truy cập vào: [https://developers.facebook.com](https://developers.facebook.com) và đăng nhập bằng tài khoản Facebook của bạn.
   - Nhấp vào **Ứng dụng của tôi (My Apps)** > Chọn **Tạo ứng dụng (Create App)**.
   - Chọn mục đích là **Cho phép mọi người đăng nhập bằng tài khoản Facebook của họ (Allow people to log in with their Facebook account)**.
   - Nhập tên hiển thị (ví dụ: `HotelBooking`) và nhấp **Tạo ứng dụng (Create App)**.

2. **Thiết lập Facebook Login**:
   - Trong bảng điều khiển ứng dụng (App Dashboard), tìm sản phẩm **Đăng nhập Facebook (Facebook Login)** và nhấp vào **Thiết lập (Set Up)**.
   - Chọn nền tảng là **Web (Trang web)**.
   - Nhập địa chỉ trang web của bạn (ví dụ local: `http://localhost:8080`) rồi lưu lại.
   - Ở cột bên trái dưới mục **Đăng nhập Facebook**, nhấp vào **Cài đặt (Settings)**.
   - Tìm trường **URI chuyển hướng OAuth hợp lệ (Valid OAuth Redirect URIs)**:
     - Ở môi trường Local: Thêm `http://localhost:8080/login/oauth2/code/facebook`
     - Ở môi trường Production (nếu có): Thêm `https://tên-miền-của-bạn.com/login/oauth2/code/facebook`
   - Nhấp vào **Lưu thay đổi (Save Changes)**.

3. **Lấy App ID và App Secret**:
   - Đi tới tab **Cài đặt (Settings)** > **Thông tin cơ bản (Basic)** ở cột menu bên trái.
   - Tại đây bạn sẽ thấy:
     - **ID ứng dụng (App ID)** (đây chính là *Client ID*)
     - **Khóa bí mật của ứng dụng (App Secret)** (nhấp vào *Hiển thị* và nhập mật khẩu Facebook của bạn để copy, đây chính là *Client Secret*).

---

## 3. Cấu hình vào dự án HotelBooking

Sau khi có đủ 4 thông tin trên, bạn cấu hình trực tiếp vào file môi trường của dự án để ứng dụng tự động nhận diện:

1. **Mở file `.env` ở thư mục gốc của dự án** (Nếu chưa có file `.env`, bạn hãy tạo mới hoặc đổi tên từ file `.env.example`).
2. **Điền các thông tin API thực tế vào**:

```env
# Cấu hình Google Client
GOOGLE_CLIENT_ID=nhập_client_id_google_của_bạn_vào_đây
GOOGLE_CLIENT_SECRET=nhập_client_secret_google_của_bạn_vào_đây

# Cấu hình Facebook Client
FACEBOOK_CLIENT_ID=nhập_app_id_facebook_của_bạn_vào_đây
FACEBOOK_CLIENT_SECRET=nhập_app_secret_facebook_của_bạn_vào_đây
```

3. **Chạy lại ứng dụng**:
   - Khi khởi động lại ứng dụng, Spring Boot sẽ tự động nạp các biến này từ file `.env` vào cấu hình OAuth2 Client.
   - Giờ đây, khi bạn bấm vào nút **Google** hoặc **Facebook** trên giao diện Đăng nhập/Đăng ký, hệ thống sẽ chuyển hướng bạn đến luồng đăng nhập thực tế của Google và Facebook để xác thực tài khoản thật!

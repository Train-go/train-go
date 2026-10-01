# Xác thực và phân quyền

Nguồn gốc: `SPEC.md` mục 41 và 45.3.

## Cơ chế

Spring Security với **session-based authentication**. Không dùng JWT, không
OAuth, không đăng nhập Google.

Hai role: `CUSTOMER` và `ADMIN`.

Phiên bản đầu hỗ trợ: đăng ký, đăng nhập, đăng xuất, session, phân quyền theo
role.

Không làm: quên mật khẩu, đặt lại mật khẩu, xác thực email, đăng nhập Google,
OAuth.

## Quyền truy cập

| Nhóm | Được dùng |
| --- | --- |
| Khách chưa đăng nhập | Landing, tìm chuyến, xem danh sách và chi tiết chuyến, đăng ký, đăng nhập |
| `CUSTOMER` | Toàn bộ luồng đặt vé, My Trips, vé điện tử, hủy booking của chính mình |
| `ADMIN` | Dashboard, quản lý ga, tuyến, tàu, chuyến, booking, danh sách người dùng |

## Quy tắc bắt buộc

- Mật khẩu phải được hash trước khi lưu.
- Backend phải kiểm tra role, không chỉ ẩn menu ở giao diện. Một Customer gõ tay
  `/admin/stations` phải bị chặn.
- Backend phải kiểm tra quyền sở hữu. Customer chỉ xem và hủy được booking của
  chính mình; gõ tay `/my-trips/<id của người khác>` phải bị chặn.

Hai lỗi phân quyền hay gặp nhất trong đồ án là chỉ ẩn nút trên giao diện, và quên
kiểm tra ownership khi truy cập theo id. Cả hai đều phải có test.

## Trạng thái hiện tại của code

`app/src/main/java/com/voyagego/traingo/common/config/SecurityConfig.java` mới chỉ
là bản nền: cho phép truy cập `/` cùng tài nguyên tĩnh, mọi đường dẫn khác yêu cầu
đăng nhập, dùng trang login mặc định của Spring Security.

Từ 01/10/2026 đã có bảng `users` và entity `auth/model/User` (email không trùng,
`passwordHash`, `role`). Seed data tạo sẵn tài khoản, mật khẩu lưu dạng BCrypt:

| Email | Mật khẩu | Role |
| --- | --- | --- |
| `admin@traingo.vn` | `Admin@123` | `ADMIN` |
| `an.nguyen@example.com`, `binh.tran@example.com`, `cuong.le@example.com` | `Customer@123` | `CUSTOMER` |

Chưa có `UserDetailsService`, chưa có trang đăng ký và đăng nhập riêng. Trang
login mặc định vẫn dùng tài khoản tạm `admin` / `admin` khai trong
`application.properties`, chưa đọc bảng `users`. Những phần này thuộc bước Auth
trong kế hoạch `docs/plans/active/`.

Khi làm Auth: đăng nhập bằng email, dùng `BCryptPasswordEncoder` (hash trong
seed là BCrypt thuần, không có tiền tố `{bcrypt}`), rồi xóa ba dòng
`spring.security.user.*` trong `application.properties`.

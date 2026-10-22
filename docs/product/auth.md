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

## Đăng ký

Chốt ngày 12/10/2026 (D4). Form đăng ký gồm họ tên, email, mật khẩu và nhập lại
mật khẩu:

- Mật khẩu ít nhất 8 ký tự. Hai ô mật khẩu phải giống nhau.
- Email không được trùng, không phân biệt chữ hoa chữ thường: đã có
  `an.nguyen@example.com` thì `An.Nguyen@Example.com` bị từ chối.
- Tài khoản tạo qua form luôn có role `CUSTOMER`.

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
kiểm tra ownership khi truy cập theo id. Cả hai đều phải được thử bằng tay: gõ thẳng
đường dẫn, không chỉ bấm theo menu (ADR 0007).

## Trạng thái hiện tại của code

Ngày 23/10/2026, phần server của #22 đã nối đăng nhập với bảng `users` qua
`UserRepository.findByEmailIgnoreCase` và `TrainGoUserDetailsService`. Mật khẩu
được đối chiếu bằng `BCryptPasswordEncoder`; đã bỏ tài khoản tạm
`spring.security.user.*`. `TrainGoUserDetails` giữ id, họ tên, email và role trong
session; `CurrentUserModelAdvice` cung cấp `currentUser` cho template.

`SecurityConfig` áp dụng:

- Public: `/`, `/trips/**`, `/api/trips/**`, `/login`, `/register`, `/error` và
  tài nguyên tĩnh. Quyền public không tạo sẵn trang/API còn thiếu; đường dẫn chưa
  triển khai vẫn có thể trả 404.
- Chỉ ADMIN: `/admin/**`, `/api/admin/**`.
- Chỉ CUSTOMER: `/booking/**`, `/my-trips/**`, `/tickets/**`.
- Đường dẫn còn lại yêu cầu đăng nhập. API bị chặn trả JSON
  `{message, fields: {}}`: GET chưa đăng nhập trả 401, thiếu quyền trả 403;
  trang HTML dùng redirect login hoặc 403 tương ứng. CSRF vẫn bật: POST thiếu
  token có thể bị 403 trước bước kiểm tra đăng nhập.
- CUSTOMER đăng nhập thành công quay lại GET đã lưu, giữ nguyên query; nếu không
  có thì về `/`. Request cache không lưu API hoặc POST. ADMIN luôn về
  `/admin/trains`, bỏ request đã lưu. POST `/logout` cần CSRF, thành công về `/`.

Từ 01/10/2026 đã có bảng `users` và entity `entity/User` (email không trùng,
`passwordHash`, `role`). Seed data tạo sẵn tài khoản, mật khẩu lưu dạng BCrypt:

| Email | Mật khẩu | Role |
| --- | --- | --- |
| `admin@traingo.vn` | `Admin@123` | `ADMIN` |
| `an.nguyen@example.com`, `binh.tran@example.com`, `cuong.le@example.com` | `Customer@123` | `CUSTOMER` |

Trang login hiện vẫn do Spring Security sinh, gửi trường `email` và `password`;
nhãn mặc định có thể vẫn là “Username”. Hash seed là BCrypt thuần, không có tiền
tố `{bcrypt}`. `admin` / `admin` không còn là tài khoản đăng nhập tạm.

#22 chưa hoàn thành toàn bộ: trang login riêng (lỗi tiếng Việt, giữ email,
ẩn/hiện mật khẩu, chuyển người đã đăng nhập), navbar khách và sidebar/admin chờ
khung dùng chung #20/#21. Header admin cũ còn tên hiển thị cố định và form logout;
chưa thể coi là tích hợp `currentUser` hoàn chỉnh. Đăng ký, phone và kiểm tra
ownership thuộc các ticket sau, chưa được triển khai bởi thay đổi này.

## Kịch bản thử bằng tay — phần server #22

1. Với MySQL Server và dữ liệu seed, đăng nhập bằng email khác hoa/thường của
   CUSTOMER và ADMIN; xác nhận đích redirect theo role.
2. Thử email không tồn tại và mật khẩu sai: cùng lỗi đăng nhập chung, không tiết
   lộ tài khoản tồn tại. Lỗi tiếng Việt của trang riêng chờ UI.
3. Khách mở trang admin bị yêu cầu đăng nhập; CUSTOMER gõ trực tiếp
   `/admin/stations` bị 403; ADMIN mở `/my-trips` bị 403.
4. GET API admin ở ba trạng thái: khách 401 JSON, CUSTOMER 403 JSON, ADMIN qua
   lớp phân quyền (endpoint chưa tồn tại có thể trả 404).
5. Khách mở `/my-trips?status=upcoming`, đăng nhập CUSTOMER và kiểm tra URL quay
   lại nguyên vẹn. Trang My Trips chưa có nên 404 chỉ chứng minh saved request.
6. POST logout có CSRF về `/`; POST thiếu CSRF bị 403; sau logout không truy cập
   được trang bảo vệ. Kiểm tra logout qua cả hai UI khi #20/#21 đã tích hợp.

Bằng chứng compile, chạy MySQL và thử trình duyệt được cập nhật trong
`docs/plans/active/tien-do-ticket-ca-nhan.md`; không dùng checklist thay kết quả.

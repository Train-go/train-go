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

Chốt ngày 12/10/2026 (D4), bổ sung phone theo #23 ngày 21/10/2026.
Form đăng ký gồm họ tên, email, số điện thoại, mật khẩu và nhập lại mật khẩu:

- Họ tên bắt buộc, tối đa 100 ký tự.
- Email hợp lệ, tối đa 255 ký tự; cắt khoảng trắng đầu/cuối, lưu chữ thường.
- Số điện thoại đúng 10 chữ số ASCII, bắt đầu bằng 0. ADMIN có thể không có phone.
- Mật khẩu ít nhất 8 ký tự, tối đa 72 byte UTF-8 theo giới hạn BCrypt hiện dùng
  (ký tự có dấu có thể chiếm nhiều byte). Hai ô mật khẩu phải giống nhau.
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

`SecurityConfig` áp dụng (mặc định mở, khóa theo tiền tố,
`docs/decisions/0013-quyen-truy-cap-mac-dinh-mo.md`):

- Chỉ ADMIN: `/admin/**`, `/api/admin/**`.
- Chỉ CUSTOMER: `/booking/**`, `/my-trips/**`, `/tickets/**`.
- Đường dẫn còn lại ai cũng vào được, gồm `/`, `/trips/**`, `/api/trips/**`,
  `/login`, `/register`, `/error` và tài nguyên tĩnh. Người chưa đăng nhập gõ sai
  đường dẫn thấy trang 404, không bị đẩy về trang đăng nhập.
- API bị chặn trả JSON
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

Hash seed là BCrypt thuần, không có tiền tố `{bcrypt}`. `admin` / `admin` không còn
là tài khoản đăng nhập tạm.

## Trang đăng nhập và đăng ký

Ngày 24/10/2026, nhánh `feat/auth-ui` (bằng chứng ở
`docs/plans/active/tien-do-ticket-ca-nhan.md`):

- `AuthController` phục vụ `GET /login`, `GET` và `POST /register`; template
  `auth/login.html`, `auth/register.html` nằm trên khung client, chỉ dùng
  Bootstrap. `static/js/auth.js` chỉ bật/tắt hiển thị mật khẩu.
- `POST /login` vẫn do Spring Security xử lý. Email lạ và mật khẩu sai cùng một
  thông báo. Email vừa nhập đi qua session đúng một lần (khóa `AUTH_LOGIN_EMAIL`,
  tối đa 255 ký tự, xóa khi trang login đọc hoặc khi đăng nhập thành công), không
  qua URL; mật khẩu không bao giờ được giữ lại hay hiện lại trong HTML.
- Người đã đăng nhập mở `/login`, `/register` hoặc gửi `POST /register` được
  chuyển về đích theo role (ADMIN `/admin/trains`, CUSTOMER `/`) và không tạo
  thêm tài khoản.
- Đăng ký: lỗi hiện đúng ô (`passwordConfirmed` → `confirmPassword`,
  `passwordWithinLimit` → `password`, email trùng → `email`); họ tên, email, số
  điện thoại được giữ, hai ô mật khẩu luôn trống. Sau khi `UserService.register`
  commit, controller xóa request cache, đổi session id rồi gọi `request.login`
  để tự đăng nhập và chuyển về `/`. Nếu bước này lỗi, tài khoản vẫn còn và khách
  về `/login` với thông báo “Tài khoản đã được tạo. Vui lòng đăng nhập.”
- Navbar: khách thấy Đăng nhập/Đăng ký; người đã đăng nhập thấy họ tên, nút
  Quản trị (chỉ ADMIN) và Đăng xuất (POST có CSRF). Sidebar admin hiện họ tên.
- `DuplicateEmailException` đã kế thừa `BusinessRuleException`.
- Từ 31/10/2026: menu nằm giữa header; Hồ sơ, Đăng xuất và Quản trị (chỉ ADMIN)
  nằm trong dropdown tài khoản. Trang `/profile` (cần đăng nhập, cả CUSTOMER và ADMIN) cho đổi họ tên và
  số điện thoại theo cùng luật với đăng ký; email là tên đăng nhập nên chỉ hiện,
  không sửa được. Sau khi lưu, bản sao thông tin người dùng trong session được
  làm mới. Chưa có chức năng đổi mật khẩu.

Chưa kiểm chứng: case tự đăng nhập lỗi sau commit (chưa tái hiện được, xem hồ sơ
tiến độ), thao tác bằng bàn phím, chế độ tắt JavaScript.

## Checkpoint server #23

`V7__user_phone_number.sql` thêm `users.phone_number VARCHAR(15) NULL` và chỉ
điền số giả `0901000001`–`0901000011` cho 11 CUSTOMER seed theo email; ADMIN
giữ null. Không sửa V1–V6 hoặc thông tin booking/vé.

`User.newCustomer` luôn gán CUSTOMER. `RegisterRequest` kiểm tra dữ liệu bằng
Bean Validation; `UserService.register` cũng validate ở ranh giới service,
chuẩn hóa email bằng `Locale.ROOT`, hash BCrypt và lưu trong transaction.
Precheck `existsByEmailIgnoreCase` cho lỗi sớm; `saveAndFlush` bắt lỗi unique
MySQL 1062/23000 của `uk_users_email` khi hai đăng ký tranh chấp. Các lỗi toàn
vẹn khác vẫn được ném lại. `DuplicateEmailException.getField()` trả `email`.

Phần giao diện, tự đăng nhập và giữ form sau lỗi của #23 đã làm ở mục “Trang đăng nhập và đăng ký”.

## Kịch bản thử bằng tay — phần server #22

1. Với MySQL Server và dữ liệu seed, đăng nhập bằng email khác hoa/thường của
   CUSTOMER và ADMIN; xác nhận đích redirect theo role.
2. Thử email không tồn tại và mật khẩu sai: cùng lỗi đăng nhập chung, không tiết
   lộ tài khoản tồn tại. Lỗi hiện tiếng Việt trên trang login, email vừa nhập được giữ lại một lần.
3. Khách mở trang admin bị yêu cầu đăng nhập; CUSTOMER gõ trực tiếp
   `/admin/stations` bị 403; ADMIN mở `/my-trips` bị 403.
4. GET API admin ở ba trạng thái: khách 401 JSON, CUSTOMER 403 JSON, ADMIN qua
   lớp phân quyền (endpoint chưa tồn tại có thể trả 404).
5. Khách mở `/my-trips?status=upcoming`, đăng nhập CUSTOMER và kiểm tra URL quay
   lại nguyên vẹn. Trang My Trips chưa có nên 404 chỉ chứng minh saved request.
6. POST logout có CSRF về `/`; POST thiếu CSRF bị 403; sau logout không truy cập
   được trang bảo vệ. Thử nút Đăng xuất ở navbar (CUSTOMER, ADMIN) và sidebar admin.

Bằng chứng compile, chạy MySQL và thử trình duyệt được cập nhật trong
`docs/plans/active/tien-do-ticket-ca-nhan.md`; không dùng checklist thay kết quả.

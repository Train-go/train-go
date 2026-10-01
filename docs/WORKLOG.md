# Nhật ký thay đổi TrainGo

File này là chỉ mục kiểm toán cho các task làm thay đổi repository. Mục tiêu là
giúp nhóm và Harness biết thay đổi nào đã được thực hiện, vì sao có thay đổi đó
và bằng chứng nào đã được chạy.

Quy tắc bắt buộc áp dụng từ commit đưa file này vào dự án. Những dòng về thay
đổi cũ hơn, nếu có, chỉ là dữ liệu khởi tạo được truy vết từ Git và kết quả test.

## Quy tắc ghi

- Thêm một dòng cho mỗi task, đặt dòng mới nhất ở đầu bảng.
- Dùng loại thay đổi theo Conventional Commits như `feat`, `fix`, `docs`,
  `refactor`, `test`, `chore`, `perf`, `build` hoặc `ci`.
- Ghi kết quả và phạm vi chính, không ghi lại từng lệnh thao tác.
- Bằng chứng phải là test, kiểm tra cú pháp, kiểm tra giao diện hoặc quan sát
  runtime đã thực sự chạy. Nếu chưa kiểm chứng được, ghi rõ giới hạn.
- Bản ghi phải được commit cùng thay đổi mà nó mô tả.
- Khi bảng dài quá 200 dòng, chuyển các năm cũ sang
  `docs/worklog/<nam>.md` và giữ năm hiện tại trong file này.

## Nhật ký

| Ngày | Loại | Kết quả và phạm vi | Bằng chứng |
| --- | --- | --- | --- |
| 2026-10-01 | `feat(station)` | `/admin/stations` thêm, sửa, xóa ga trên MySQL thật qua `station/controller/StationAdminController`: validate tiếng Việt, mã ga tự viết hoa và không trùng, Post/Redirect/Get kèm thông báo, từ chối xóa ga đang được tuyến dùng, hộp xác nhận `data-confirm` trong `admin.js`. Tên admin trên header chuyển sang `AdminShellModelAdvice`; gỡ dữ liệu ga giả khỏi `AdminController`/`AdminService`. Sửa giao diện gặp trên đường: bảng admin xuống dòng từng chữ ở màn hình hẹp, mũi tên sắp xếp đè chữ tiêu đề, bảng "Booking gần đây" ở dashboard bị bóp ở 1280px. Ghi plan chia việc cho nhóm `docs/plans/active/giai-doan-2-du-lieu-va-man-hinh-admin.md`, cập nhật ADR 0001, 0004 và `docs/product/admin.md`. | Chromium qua Playwright trên MySQL thật: 28/28 bước chức năng của luồng quản lý ga đạt; lỗi console duy nhất là request 404 cố ý tới ga không tồn tại. Trang ga và form ga không tràn trang, không ô bảng nào bị xuống dòng ở 320/375/768/1280/1920px; dashboard hết xuống dòng từ 768px trở lên; khoảng cách chữ và mũi tên sắp xếp tối thiểu 4px trên ga, chuyến, người dùng. `/admin/trips` (dữ liệu giả) vẫn xuống dòng ở 768px, chưa sửa. `.\mvnw.cmd test`: `BUILD SUCCESS` nhưng 0 test, vì `app/src/test` đã bị xóa trong working tree. |
| 2026-10-01 | `feat(db)` | Chuyển schema sang Flyway (`ddl-auto=validate`): `V1__create_schema.sql` tạo đủ 11 bảng với 14 khóa ngoại, unique và `CHECK` có tên; `V2__seed_demo_data.sql` nạp 8 ga, 8 tuyến, 8 tàu, 32 toa, 768 ghế, 114 chuyến, 4 tài khoản và 4 booking. Thêm entity và repository cho 11 bảng; MySQL Docker chạy giờ +07:00. Ghi ADR 0005, cập nhật `docs/product/domain-model.md`, `auth.md`, `docs/RUNBOOK.md`, `AGENTS.md`. | MySQL 8.4 thật: Flyway áp V1 và V2 trong 0,564s, Hibernate `validate` không lỗi; số dòng từng bảng khớp thiết kế (10.944 `trip_seats`); tổng tiền booking bằng tổng giá vé và giá vé đúng `basePrice + priceModifier`; 7 lệnh ghi sai bị ràng buộc chặn; hash BCrypt trong seed được kiểm tra khớp mật khẩu. Migration chưa chạy trên H2. |
| 2026-09-27 | `refactor(admin,home)` | Bên trong package `admin` và `home` tách thêm subpackage `controller/model/service`: dữ liệu mẫu (seeding data) và các record trước đây khai báo ngay trong controller chuyển vào `model/` và `service/` (`AdminService`, `HomeService`), controller nhận service qua constructor injection. Model đổi từ `record` sang `class` thường có constructor và getter tường minh theo yêu cầu. Gộp các comment "Replace with XRepository.findAll()" lặp lại thành một Javadoc ở service. Ghi quy ước subpackage này vào `docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`. | Maven (`./mvnw.cmd test` trong `app/`): 20 test vượt qua, `BUILD SUCCESS`. |
| 2026-09-02 | `feat(design)` | Tải local bốn font OFL gồm Be Vietnam Pro, Inter, Onest và IBM Plex Sans; thêm stylesheet `@font-face` cùng Font Lab để so sánh mà chưa đổi font sản phẩm. | Kiểm tra magic bytes WOFF2 và bốn file OFL; JavaScript syntax; Maven: 15 test vượt qua; kiểm tra giao diện desktop và mobile. |
| 2026-09-02 | `docs(harness)` | Bổ sung quy định bắt buộc ghi lại mọi task thay đổi repository, tạo `docs/WORKLOG.md` và cập nhật bản đồ tài liệu. | Đối chiếu `AGENTS.md`, `docs/WORKFLOW.md` và bản đồ tài liệu hiện có; `git diff --check`. |
| 2026-09-02 | `feat(design)` | Thêm Color Lab và hệ token Railway Blue đầy đủ cho client, admin và semantic status. | Commit `52949fa`; Maven: 14 test vượt qua; route `/design/color-palettes.html` trả HTTP 200. |

# Execution Plan: MVP TrainGo

Date: 2026-08-23

## Status

Active

## Outcome

Luồng MVP chạy thông từ đầu đến cuối trên ứng dụng thật: Admin tạo ga → tuyến →
tàu (xem sơ đồ toa) → chuyến; Customer tìm chuyến → chọn toa → chọn chỗ →
nhập hành khách → review → thanh toán mô phỏng (lúc này mới khóa chỗ) → nhận vé điện tử → thấy booking
trong My Trips và hủy được nó.

Bằng chứng chấp nhận: chạy trọn luồng trên trình duyệt với MySQL thật, theo kịch
bản thử bằng tay của từng task. Dự án không có test tự động
(`docs/decisions/0007-bo-test-tu-dong.md`).

## Context

- Yêu cầu gốc: `SPEC.md`.
- Bản chắt lọc: `docs/product/overview.md`, `domain-model.md`, `booking-flow.md`,
  `admin.md`, `auth.md`.
- Quyết định đã chốt: `docs/decisions/` (0001, 0003-0007; 0002 đã bị 0007 thay).
- Cách chạy: `docs/RUNBOOK.md`.
- Code: `app/`, package gốc `com.voyagego.traingo`.

## Scope

In scope:

- Toàn bộ danh sách feature MVP trong `SPEC.md` mục 50.
- Seed data đủ để demo luồng chính.

Out of scope:

- Mọi mục trong `SPEC.md` phần 48 (Out of Scope).
- Tính năng optional: QR code, quét QR, Railway Map, xuất PDF. Chỉ bắt đầu sau
  khi luồng MVP đã chạy thông.

## Approach

Xây theo thứ tự phụ thuộc dữ liệu, mỗi giai đoạn kết thúc bằng một thứ chạy được
và một bộ test tương ứng.

**Giai đoạn 0 — Nền tảng.** Cấu trúc thư mục, build, bộ test, MySQL bằng Docker,
landing tạm.

**Giai đoạn 1 — Auth.** `User` entity, hash mật khẩu, đăng ký, đăng nhập, đăng
xuất, phân quyền `CUSTOMER` / `ADMIN`, seed tài khoản admin. Mọi việc sau đều cần
biết ai đang đăng nhập.

**Giai đoạn 2 — Dữ liệu vận hành.** Station → Route → Train (xem sơ đồ toa từ cấu
hình toa cố định `CoachLayout`) → Trip kèm sinh 96 TripSeat từ cấu hình đó. Làm
đúng thứ tự này vì mỗi bước phụ thuộc bước trước.

**Giai đoạn 3 — Tìm chuyến.** Form tìm kiếm ở landing, validate điều kiện tìm
kiếm, danh sách chuyến với số chỗ còn lại và giá thấp nhất, trang chi tiết chuyến.

**Giai đoạn 4 — Chọn chỗ.** Trang chọn chỗ hiện một toa một lần, có dải tab toa, sơ đồ
ghế và sơ đồ giường vẽ bằng JavaScript, endpoint JSON trả trạng thái TripSeat
(`AVAILABLE` hoặc `BOOKED`), lưu các chỗ đã chọn vào bản nháp trong session. Không giữ
chỗ, không đếm ngược, không hết hạn (ADR 0010).

**Giai đoạn 5 — Hành khách và review.** Form nhập hành khách khớp số chỗ, gán mỗi
hành khách một chỗ, màn hình review với giá từng vé và tổng tiền.

**Giai đoạn 6 — Thanh toán mô phỏng và vé.** Một transaction duy nhất khóa chỗ
(`UPDATE` có điều kiện `AVAILABLE` sang `BOOKED`), tạo booking `CONFIRMED` / `PAID` và
sinh Ticket; trang booking thành công, trang vé điện tử.

**Giai đoạn 7 — My Trips và hủy booking.** Danh sách theo ba nhóm, chi tiết
booking, hủy booking trả chỗ về `AVAILABLE`, kiểm tra quyền sở hữu.

**Giai đoạn 8 — Admin còn lại.** Dashboard, quản lý booking, danh sách người dùng.

**Giai đoạn 9 — Hoàn thiện.** Responsive, thông báo lỗi tử tế, seed data demo,
rà lại toàn bộ luồng.

Gợi ý chia việc cho ba thành viên sau giai đoạn 1: một người làm giai đoạn 2 và
8, một người làm giai đoạn 3 và 4, một người làm giai đoạn 5, 6 và 7. Mỗi nghiệp vụ
là một file riêng trong từng tầng package nên ba nhánh ít đụng nhau.

## Risks And Recovery

- **Khóa chỗ lúc thanh toán sai dẫn tới hai người cùng một ghế.** Đây là rủi ro lớn
  nhất. Phòng bằng câu `UPDATE ... WHERE status = 'AVAILABLE'` rồi so số dòng được
  cập nhật với số chỗ đã chọn (không "đọc rồi ghi"), rollback khi thiếu, và thử tay
  từng nhánh thất bại bằng hai trình duyệt và SQL.
- **Transaction thanh toán không bao trọn ba thay đổi** (khóa chỗ, tạo booking, tạo vé), để
  lại dữ liệu nửa vời.
  Phòng bằng `@Transactional` ở tầng service, kiểm tra hết điều kiện trước khi đổi
  dữ liệu, và thử tay trường hợp lỗi giữa chừng.
- **Khách mất chỗ ở bước cuối.** Vì chỗ chỉ khóa lúc thanh toán, hai người cùng chọn một
  chỗ chỉ biết kết quả khi thanh toán (lệch `SPEC.md` mục giữ chỗ, ADR 0010). Phòng bằng
  thông báo rõ, về trang chọn chỗ với sơ đồ mới, và giữ bản nháp hành khách trong
  session; nhóm nên báo giảng viên hoặc ghi chú khi nộp.
- **Lệch phiên bản Spring Boot 4 so với tài liệu 3.x** làm mất thời gian gỡ lỗi
  giả. Khi gặp lỗi không tìm thấy class hoặc annotation, kiểm tra jar thật trong
  `~/.m2/repository` trước khi tin bài viết trên mạng.
- **Sửa một migration Flyway đã chạy** làm ứng dụng không khởi động trên máy
  người khác (từ 01/10/2026 schema do Flyway quản lý, ADR 0005). Phòng bằng quy
  tắc chỉ thêm migration mới. Phục hồi máy dev bằng `docker compose down -v` rồi
  chạy lại; chấp nhận mất dữ liệu dev.
- **Tính năng optional lấn tiến độ.** Không bắt đầu QR, bản đồ hay PDF trước khi
  giai đoạn 7 xong.

## Progress

- [x] Giai đoạn 0: xóa skeleton trùng, đổi `demo/` thành `app/`, đổi định danh
      Maven và package sang `com.voyagego.traingo`, thêm dependency web,
      Thymeleaf, JPA, Security, Validation, MySQL driver, các starter test của
      Boot 4, cấu hình H2 cho test, viết `compose.yaml`, landing tạm, hai test
      mẫu. `.\mvnw.cmd test` xanh.
- [x] Giai đoạn 0b: Docker Desktop đã bật, container `traingo-mysql` healthy,
      `spring-boot:run` khởi động thành công và kết nối được database. Port đổi
      sang 8081 vì Apache của XAMPP đang giữ 8080. `docs/RUNBOOK.md` đã cập nhật
      theo bằng chứng thật (24/08/2026).
- [x] Giai đoạn 0c: nền tảng giao diện và landing tĩnh — Bootstrap phục vụ từ
      `static/vendor/`, layout fragment dùng chung, design token, landing đủ bốn
      khối theo `SPEC.md` mục 5. Chi tiết và bằng chứng:
      `docs/plans/active/nen-tang-giao-dien-va-landing.md`.
- [ ] Giai đoạn 1: Auth. Cập nhật 24/10/2026: server đăng nhập/đăng ký và shell
      đã merge; còn controller, form riêng, trạng thái tài khoản và kiểm chứng UI.
      Kế hoạch nháp tiếp nối trên `feat/auth-ui`:
      [Hoàn thiện UI đăng nhập và đăng ký](auth-ui/plan.md).
- [ ] Giai đoạn 2: Station, Route, Train, Trip, TripSeat. Đã có
      (01/10/2026): schema Flyway, entity, repository, seed demo, quản lý ga
      trên database thật; từ 19/10/2026 còn 8 bảng, không còn `coaches` và
      `seats` (ADR 0008, 0009). Còn: quản lý tuyến, tàu, chuyến, sơ đồ toa. Phần đã làm: `docs/plans/completed/giai-doan-2-du-lieu-va-man-hinh-admin.md`.
      Từ 04/10/2026, toàn bộ phần còn lại của MVP (giai đoạn 1-9) được chia thành 14
      task trong `docs/plans/active/chia-viec-mvp.md`.
- [ ] Giai đoạn 3: Tìm chuyến và danh sách chuyến.
- [ ] Giai đoạn 4: Chọn chỗ (không giữ chỗ, ADR 0010).
- [ ] Giai đoạn 5: Hành khách và review.
- [ ] Giai đoạn 6: Thanh toán mô phỏng, transaction, vé điện tử.
- [ ] Giai đoạn 7: My Trips và hủy booking.
- [ ] Giai đoạn 8: Admin dashboard, quản lý booking, danh sách người dùng.
- [ ] Giai đoạn 9: Hoàn thiện và seed data.

## Decisions

- 2026-08-23: Cấu trúc thư mục, package theo feature, mức tách client, tech stack
  — xem `docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`.
- 2026-08-23: Chiến lược test — xem `docs/decisions/0002-chien-luoc-test.md`.
- 2026-08-24: Hệ thống giao diện — tên hiển thị `TrainGo`, Bootstrap phục vụ từ
  ứng dụng, layout fragment Thymeleaf thuần, design token, `line-height` heading
  tối thiểu 1.25 cho chữ tiếng Việt có dấu. Xem
  `docs/decisions/0003-he-thong-giao-dien.md`.
- 2026-08-24: Ứng dụng chạy ở port 8081 thay vì 8080 vì Apache của XAMPP đang giữ
  8080 trên máy dev. Khai trong `app/src/main/resources/application.properties`.
- 2026-10-01: Schema do Flyway quản lý, Hibernate chỉ `validate`; tạo 11 bảng
  (sau này gộp còn 8, xem ADR 0008, 0009) và seed demo một lần. Xem `docs/decisions/0005-quan-ly-schema-bang-flyway.md`.
- 2026-10-01: Màn hình admin của một nghiệp vụ có controller riêng, không dồn vào
  `AdminController`. Xem `docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`.
- 2026-10-11: Package chia theo tầng (`controller`, `entity`, `model`,
  `repository`, `service`) thay cho theo feature. Xem
  `docs/decisions/0006-cau-truc-package-theo-tang.md`.
- 2026-10-12: Bỏ test tự động; mỗi task có kịch bản thử bằng tay. Xem
  `docs/decisions/0007-bo-test-tu-dong.md` (thay cho ADR 0002).
- 2026-10-12: Chốt tám quyết định nghiệp vụ D1-D8, gồm cách thu hồi TripSeat hết
  hạn giữ chỗ (D1, so giờ lúc đọc, không có job; sau đó không còn áp dụng, xem ADR 0010), quy tắc xóa dữ liệu đã bị tham
  chiếu (D2, chặn xóa, chuyến chỉ hủy). D3 (sinh thêm TripSeat khi thêm toa vào
  tàu đã có chuyến) không còn áp dụng sau ADR 0009. Xem bảng "Quyết định nghiệp vụ" trong
  `docs/plans/active/chia-viec-mvp.md`.
- 2026-10-19: Bỏ tính năng giữ chỗ: không còn `HELD`, `held_by_user_id`, `held_until`; chỗ khóa
  lúc thanh toán trong một transaction, booking tạo thẳng `CONFIRMED` / `PAID`. Xem
  `docs/decisions/0010-bo-giu-cho.md`. D1 không còn áp dụng, D7 viết lại.

## Validation

- Mỗi task: đi hết mục "Kịch bản thử bằng tay" trong file hướng dẫn của task.
- Toàn MVP: chạy tay trọn luồng đặt vé trên trình duyệt với MySQL thật (T14).
- Trước mọi báo cáo hoàn thành: `.\mvnw.cmd compile` không lỗi và ứng dụng khởi
  động được (`docs/decisions/0007-bo-test-tu-dong.md`).

## Result

Chưa hoàn thành. Cập nhật mục này khi luồng MVP chạy thông, kèm bằng chứng, giới
hạn còn lại và việc cần làm tiếp, rồi mới chuyển kế hoạch sang
`docs/plans/completed/`.

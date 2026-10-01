# Execution Plan: MVP TrainGo

Date: 2026-08-23

## Status

Active

## Outcome

Luồng MVP chạy thông từ đầu đến cuối trên ứng dụng thật: Admin tạo ga → tuyến →
tàu → toa và ghế → chuyến; Customer tìm chuyến → chọn toa → chọn chỗ → giữ chỗ →
nhập hành khách → review → thanh toán mô phỏng → nhận vé điện tử → thấy booking
trong My Trips và hủy được nó.

Bằng chứng chấp nhận: chạy trọn luồng trên trình duyệt với MySQL thật, cộng với
bộ test tự động xanh cho các nghiệp vụ bắt buộc liệt kê trong
`docs/decisions/0002-chien-luoc-test.md`.

## Context

- Yêu cầu gốc: `SPEC.md`.
- Bản chắt lọc: `docs/product/overview.md`, `domain-model.md`, `booking-flow.md`,
  `admin.md`, `auth.md`.
- Quyết định đã chốt: `docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`,
  `docs/decisions/0002-chien-luoc-test.md`.
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

**Giai đoạn 2 — Dữ liệu vận hành.** Station → Route → Train với Coach và sinh
Seat tự động → Trip kèm sinh TripSeat. Làm đúng thứ tự này vì mỗi bước phụ thuộc
bước trước.

**Giai đoạn 3 — Tìm chuyến.** Form tìm kiếm ở landing, validate điều kiện tìm
kiếm, danh sách chuyến với số chỗ còn lại và giá thấp nhất, trang chi tiết chuyến.

**Giai đoạn 4 — Chọn chỗ và giữ chỗ.** Trang chọn toa, sơ đồ ghế và sơ đồ giường
vẽ bằng JavaScript, endpoint JSON trả trạng thái TripSeat, endpoint giữ chỗ, đếm
ngược 5 phút, xử lý hết hạn.

**Giai đoạn 5 — Hành khách và review.** Form nhập hành khách khớp số chỗ, gán mỗi
hành khách một chỗ, màn hình review với giá từng vé và tổng tiền.

**Giai đoạn 6 — Thanh toán mô phỏng và vé.** Xác nhận booking trong một
transaction, sinh Ticket, trang booking thành công, trang vé điện tử.

**Giai đoạn 7 — My Trips và hủy booking.** Danh sách theo ba nhóm, chi tiết
booking, hủy booking trả chỗ về `AVAILABLE`, kiểm tra quyền sở hữu.

**Giai đoạn 8 — Admin còn lại.** Dashboard, quản lý booking, danh sách người dùng.

**Giai đoạn 9 — Hoàn thiện.** Responsive, thông báo lỗi tử tế, seed data demo,
rà lại toàn bộ luồng.

Gợi ý chia việc cho ba thành viên sau giai đoạn 1: một người làm giai đoạn 2 và
8, một người làm giai đoạn 3 và 4, một người làm giai đoạn 5, 6 và 7. Package chia
theo feature nên ba nhánh ít đụng nhau.

## Risks And Recovery

- **Giữ chỗ và xác nhận booking sai dẫn tới hai người cùng một ghế.** Đây là rủi
  ro lớn nhất. Phòng bằng ràng buộc unique `tripId + seatId` ở database, kiểm tra
  lại trạng thái ngay trước khi commit, và test cho từng nhánh thất bại.
- **Transaction không bao trọn bốn thay đổi trạng thái**, để lại dữ liệu nửa vời.
  Phòng bằng `@Transactional` ở tầng service và test khẳng định rollback.
- **Chưa quyết cách thu hồi chỗ hết hạn** (kiểm tra lười hay job định kỳ). Phải
  chốt trước khi bắt đầu giai đoạn 4, và ghi vào mục Decisions bên dưới.
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
- [ ] Giai đoạn 1: Auth.
- [ ] Giai đoạn 2: Station, Route, Train, Coach, Seat, Trip, TripSeat. Đã có
      (01/10/2026): schema Flyway cho cả 11 bảng của dự án, entity, repository,
      seed demo, quản lý ga trên database thật. Còn: quản lý tuyến, tàu, toa,
      chuyến. Chi tiết và chia việc:
      `docs/plans/active/giai-doan-2-du-lieu-va-man-hinh-admin.md`.
- [ ] Giai đoạn 3: Tìm chuyến và danh sách chuyến.
- [ ] Giai đoạn 4: Chọn chỗ và giữ chỗ.
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
- 2026-10-01: Schema do Flyway quản lý, Hibernate chỉ `validate`; tạo đủ 11 bảng
  và seed demo một lần. Xem `docs/decisions/0005-quan-ly-schema-bang-flyway.md`.
- 2026-10-01: Màn hình admin của một feature nằm trong package của feature đó.
  Xem `docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`.
- Chưa quyết: cách thu hồi TripSeat hết hạn giữ chỗ.
- Chưa quyết: quy tắc xóa dữ liệu đã bị tham chiếu, với các entity ngoài ga.
- Chưa quyết: thêm toa vào tàu đã có chuyến thì có sinh thêm TripSeat không.

## Validation

- Focused proof: unit test cho service nghiệp vụ, `@DataJpaTest` cho ràng buộc
  `tripId + seatId`, `@WebMvcTest` cho phân quyền và validation.
- Integration hoặc end-to-end proof: một `@SpringBootTest` chạy trọn luồng đặt
  vé, cộng với một lần chạy tay trên trình duyệt với MySQL thật.
- Repository-required checks: `.\mvnw.cmd test` phải xanh trước mọi báo cáo hoàn
  thành.

## Result

Chưa hoàn thành. Cập nhật mục này khi luồng MVP chạy thông, kèm bằng chứng, giới
hạn còn lại và việc cần làm tiếp, rồi mới chuyển kế hoạch sang
`docs/plans/completed/`.

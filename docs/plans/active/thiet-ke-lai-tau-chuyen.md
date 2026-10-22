# Execution Plan: Thiết kế lại ga, tuyến, tàu, chuyến và layout admin

Date: 2026-10-20

## Status

Active. Phần 1 đã làm và đã kiểm chứng. Phần 2 đã có bản đề xuất, chờ Phi chọn phương án.

## Outcome

Ga và tuyến là dữ liệu nền, xác định trước trên một trục Bắc - Nam. Chuyến thuộc về
tàu và các chuyến của một tàu nối chặng liền mạch. Trên nền đó, Phi duyệt một PDF
giải thích cách chọn thiết kế giao diện admin, rồi mới đến layout mẫu.

## Context

- Phi thấy cách nghĩ "chuyến đi theo tuyến" cũ (8 tàu, mỗi tàu một cặp ga cố định,
  ga rải rác) sai với lý thuyết nên không dựng được layout admin.
- `SPEC.md` mục 11.2 và 38: Trip là một lần chạy của Train, tạo từ Train và Route.
  Mục 10, 35, 42, 48, 54: không có ga trung gian.
- Quyết định: `docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`, D9 trong
  `docs/plans/active/chia-viec-mvp.md`.
- Luật: `docs/product/domain-model.md` (mục "Lịch của tàu và luật nối chặng"),
  `docs/product/admin.md`.

Phi chốt ngày 20/10/2026:

- Mô hình "chặng nối tiếp": một chuyến vẫn chỉ có 2 ga.
- Luật nối chặng bắt buộc, chuyến mới chỉ nối vào cuối lịch.
- 8 ga Bắc - Nam, không lấy vùng biên.
- 16 tuyến; tuyến dài khác admin vẫn tạo được.
- 3 tàu `SE1`, `SE2`, `SE3`; chỉ `SE1` có chuyến mẫu.
- Tàu không gắn với tuyến.

## Scope

In scope:

- Phần 1: migration `V5`, code tối thiểu theo schema mới, tài liệu và hướng dẫn task.
- Phần 2: `docs/reports/thiet-ke-giao-dien-admin.html` và bản PDF, giải thích cách
  chọn thiết kế từng trang admin trên mô hình mới.
- Phần 3: layout mẫu admin trong `docs/reports/ui-mau/` theo PDF đã duyệt.

Out of scope:

- Cài đặt các task T03-T13 (trang chi tiết tàu thật, form tạo chuyến thật, tìm
  chuyến...).
- Các thay đổi khác trong ERD ngày 19/10: `users_id`, `from/to_station_id`, bỏ
  `price_modifier`, `is_delete`, `date_of_birth`.
- Cột `region`, `image_url` của ga; chỉ xét lại ở phần 2 nếu thiết kế cần.

## Approach

1. Phần 1, logic (xong):
   - `V5__north_south_line.sql`:
     - thêm `stations.km_mark` (bắt buộc, không trùng, không âm);
     - đổi giờ chuyến thành `departure_at`, `arrival_at`;
     - thêm index theo tuyến và theo tàu;
     - xóa seed cũ và nạp lại seed mới.
   - Java: `Trip`, `Station`, `StationRequest`, `StationService`, `StationAdminController`
     (lỗi trùng lý trình), `TripRepository`, `RouteRepository` (xếp theo lý trình),
     `AdminService` (ngày-giờ, nhãn "Đã khởi hành" theo giờ thật), `HomeService`.
   - Template: thêm ô lý trình vào form ga, cột lý trình vào danh sách ga.
   - Tài liệu: ADR 0011, `domain-model.md`, `admin.md`, `booking-flow.md`,
     `RUNBOOK.md`, D9, các hướng dẫn task.
2. Phần 2, PDF, sau khi Phi duyệt phần 1:
   - Mô hình trong một hình.
   - Điều hướng xoay quanh tàu.
   - Từng trang: việc chính, layout chọn, phương án đã loại và lý do, wireframe SVG.
   - Quy tắc chung.
   - Xuất A4, không CDN.
3. Phần 3, layout mẫu, sau khi Phi duyệt PDF:
   - Cập nhật generator mockup với dữ liệu mới.
   - Trang tàu có lịch chạy và form nối chuyến.

## Risks And Recovery

- **Lệch SPEC mục 9 và 11.2.** Đã ghi trong ADR 0011; nhóm cần báo giảng viên.
- **V5 xóa dữ liệu demo cũ trên mọi máy chạy nó.**
  - Trên máy Phi đã dump bản trước V5 ở scratchpad của phiên làm việc.
  - Khôi phục: `docker compose down -v`, chạy lại ứng dụng tới V4 bằng code cũ, rồi
    nạp bản dump.
- **Phụ thuộc V4.** V4 và ADR 0008-0010 chưa commit; V5 phải vào cùng hoặc sau chúng.

## Progress

- [x] Đối chiếu lý trình 8 ga với bảng ga đường sắt Thống Nhất.
- [x] Migration V5 và code Java theo schema mới.
- [x] Tài liệu product, ADR 0011, RUNBOOK, D9.
- [x] Hướng dẫn task T03-T14 và README (T02 không cần đổi).
- [x] Kiểm chứng phần 1 (xem Validation).
- [x] ERD `docs/reports/erd.html`, `erd-hien-tai.md` theo V5 (Phi yêu cầu ngày 20/10).
- [x] Phần 2: `docs/reports/thiet-ke-giao-dien-admin.html` và `.pdf`.
- [x] Phần 3 (Phi yêu cầu làm luôn ngày 20/10): hai mẫu admin `docs/reports/ui-mau/admin/mau-a.html` và `mau-b.html`.
- [x] Phi thấy A, B còn rối, nhất là Ga và Tuyến (21/10): thêm bốn mẫu `mau-1.html` đến `mau-4.html` có tự điền khi thêm ga, tạo tuyến hàng loạt, ma trận ga × ga.
- [x] Phi bỏ Mẫu 1 "Ngăn chi tiết" (21/10).
- [x] Phi chọn và gộp thành một giao diện trong `docs/reports/ui-mau/admin/mau-2.html` (21/10). Các mẫu 3, 4, A, B đã xóa.
  - Tàu có hai view, đổi bằng nút "Theo ngày" / "Bảng tính": view theo ngày của Mẫu 4 (banner chặng tiếp theo, lịch nhóm theo ngày, nút "Nối chặng" mở trình hướng dẫn 4 bước) và view bảng tính của Mẫu 2 (dòng nháp, điền nhanh tới ga cuối).
  - Ga và tuyến lấy của Mẫu 3 (sơ đồ dọc Bắc - Nam); tab Tuyến riêng, Chuyến, Booking, Người dùng lấy của Mẫu 2. Bỏ ô Làm nhanh Ctrl K.
- [ ] Phần 3: layout mẫu admin.

## Decisions

- 2026-10-20: `km_mark` là `NOT NULL` thay vì `NULL` như bản plan đầu. V5 xóa hết
  ga cũ trước khi thêm cột, nên không có dòng nào thiếu giá trị, và ga nào cũng cần
  lý trình để xếp thứ tự.
- 2026-10-20: lịch mẫu của `SE1` chạy mỗi ngày một chặng lúc 08:00, để dễ hình dung
  nhất cho ý "đi qua các ga đều nhau, lần lượt".

## Validation

- **Focused proof** (20/10/2026, trên MySQL 8.4 thật):
  - Flyway áp V5 trong 0,513s, Hibernate `validate` không lỗi.
  - SQL đếm: 8 ga, 16 tuyến, 3 tàu, 18 chuyến, 1.728 `trip_seats`, 8 booking, 11 vé.
  - 0 chỗ đứt chuỗi, 0 chuyến chồng giờ, 0 chuyến có giờ đến trước giờ đi.
  - 0 vé sai công thức giá, 0 booking đặt sau giờ khởi hành.
- **Integration or end-to-end proof** (Chromium qua Playwright, 1440px và 390px):
  - Danh sách ga xếp theo lý trình. Form ga báo đúng 5 lỗi: thiếu lý trình, số âm,
    số lẻ, lý trình trùng, mã trùng.
  - Thêm, sửa, xóa một ga mới đều chạy; chặn xóa SGN có lý do.
  - Trang Tuyến đủ 16 tuyến, xếp theo lý trình. Trang Tàu có 3 tàu.
  - Trang Chuyến đủ 18 chuyến, chuyến 08:00 hôm nay hiện "Đã khởi hành".
  - Trang Booking đủ 8 dòng. Trang chủ liệt kê ga theo thứ tự Bắc - Nam.
  - Không có lỗi console.
- **Repository-required checks:**
  - `.\mvnw.cmd compile` thành công.
  - `.\mvnw.cmd spring-boot:run` khởi động được.

## Result

Chưa xong: đã có một giao diện admin gộp, nằm trong một file `docs/reports/ui-mau/admin/index.html`. Sau đó mới đưa thiết kế đã chọn vào Thymeleaf theo các task T03-T13.

# Execution Plan: Trang Tuyến — thêm, sửa, xóa, tìm kiếm (#24)

Date: 2026-10-26

## Status

Completed (2026-10-26)

## Outcome

`/admin/routes` quản lý tuyến bằng trang Thymeleaf thường: danh sách xếp theo lý trình có ô tìm không phân biệt dấu, thêm tuyến (một hoặc hai chiều), sửa khoảng cách, xóa tuyến chưa có chuyến. Lỗi hiện đúng ô; xóa tuyến đã có chuyến bị chặn kèm lý do.

## Context

- Issue #24 (assign `thanhphong97`), luật nghiệp vụ: `docs/product/admin.md` mục "Quản lý tuyến", D2.
- Server đã có và đã kiểm chứng: `RouteService`, `RouteCreateRequest`, `RouteDistanceRequest`, `RouteRow`, exception tuyến. Không làm lại.
- Mẫu giao diện: `controller/StationAdminController`, `templates/admin/stations.html`, `station-form.html` (Post/Redirect/Get, `data-confirm`).
- Hồ sơ tiến độ: `docs/plans/active/tien-do-ticket-ca-nhan.md`.

## Scope

In scope:

- `RouteAdminController` (list + tìm `?q=`, new, create, edit, update, delete).
- `RouteService.search(q)` và `RouteService.findRow(id)`.
- Templates `routes.html` (viết lại), `route-form.html`, `route-edit.html` (hoặc gộp).
- `static/js/admin/route-form.js`: tự điền km theo lý trình.
- Gỡ `routes()` khỏi `AdminController`/`AdminService`, xóa `AdminRoute`.
- Docs: `docs/product/admin.md`, hồ sơ tiến độ, WORKLOG.

Out of scope (người dùng chốt 25/10/2026):

- Ma trận ga × ga, popover, hoàn tác, bàn phím mũi tên (AC1–AC4 của #24) và REST `/api/admin/routes`.
  Hệ quả: #27 không có component ma trận dùng lại từ #24; xét lại khi làm #27.
- Tạo nhiều tuyến một lần (#39), sơ đồ dọc (#27).

## Approach

1. Controller theo mẫu `StationAdminController`; lỗi `differentStations` gắn vào `destinationStationId`; `DuplicateRouteException` gắn vào ô ga đến; `RouteInUseException` thành flash `errorMessage`.
2. `search(q)` lọc `findAll()` trong bộ nhớ: NFD, bỏ dấu, `đ→d`, không phân biệt hoa thường, so với tên và mã hai ga.
3. Template danh sách: cột tuyến, km, chiều ngược, chuyến sắp chạy, tổng chuyến, thao tác; nút xóa khóa kèm lý do khi `totalTrips > 0`.
4. Form tạo: select ga theo lý trình (`data-km-mark`), km tự điền khi chưa sửa tay, tích "Tạo luôn chiều ngược" mặc định bật.
5. Form sửa: hai ga chỉ đọc, chỉ đổi khoảng cách.

## Risks And Recovery

- Server chặn xóa bằng kiểm tra trước; nút khóa chỉ là gợi ý giao diện, POST thẳng vẫn phải bị từ chối.
- Không có migration; hoàn tác thay đổi bằng revert commit. Dữ liệu thử xóa lại bằng chính chức năng xóa.

## Progress

- [x] Lập kế hoạch, duyệt.
- [x] Codex implement (ck:cook --auto --no-test). Compile trong sandbox Codex bị chặn do quyền `~/.m2`; Claude compile lại ngoài sandbox.
- [x] Review diff: không finding cần sửa.
- [x] Compile, khởi động, thử tay trình duyệt.
- [x] Docs, WORKLOG, commit.

## Decisions

- 2026-10-25: không làm ma trận; trang Tuyến chỉ thêm/sửa/xóa/tìm kiếm thường (người dùng).
- 2026-10-26: gộp form thêm và sửa vào `route-form.html` (cờ `routeId`); thêm thông báo `typeMismatch` tiếng Việt cho ba ô số trong `messages.properties`.

## Validation

- Focused proof: probe nghiệp vụ qua trình duyệt (tìm "vinh"/"VIN"/"hue"; Huế → Vinh tạo 2 tuyến 369 km; trùng và cùng ga báo ở ô ga đến; km 0 báo ở ô km; sửa km; xóa tuyến chưa có chuyến qua xác nhận; xóa tuyến có chuyến bị chặn cả bằng POST thẳng).
- Integration or end-to-end proof: `.\mvnw.cmd compile`, `spring-boot:run` trên MySQL local, CUSTOMER vào `/admin/routes` bị 403, 1440px/390px không tràn ngang, hồi quy các trang admin khác.
- Repository-required checks: không test tự động (ADR 0007); một dòng WORKLOG cùng commit.

## Result

Đã kiểm chứng 26/10/2026 trên MySQL Server local: `mvnw clean compile` đạt; ứng dụng khởi động, Flyway V7 up to date. Playwright 44/44 PASS theo mục Validation, cộng id lạ 404, CUSTOMER 403, 1440px/390px không tràn ngang, hồi quy trang admin khác. Dữ liệu thử đã xóa bằng chính chức năng xóa; DB về 16 tuyến, 18 chuyến.

Giới hạn: ô km của form thêm hiện `0` trước khi chọn ga (field `int`); chưa thử bàn phím đầy đủ và chế độ tắt JS. AC ma trận của #24 không làm theo quyết định 25/10; #24 trên GitHub cần người dùng quyết định đóng hay sửa mô tả.

# Execution Plan: Đưa giao diện mới từ prototype v2 vào app TrainGo

Date: 2026-10-30

## Status

Active. Phi duyệt 30/10/2026: "thực hiện từ đầu đến cuối giống như mẫu", giữ Bootstrap, admin trước
rồi khách, hai nhánh mỗi nhánh một commit, CSS chia theo từng trang. Vì vậy các đợt admin gộp trong
nhánh `feat/admin-redesign-v2`, các đợt khách trong `feat/client-redesign-v2`.

## Outcome

Mọi màn của app Spring Boot, cả admin lẫn khách, có giao diện của
`D:\Study\Project\VoyageGO\traingo-redesign-v2\` và vẫn chạy trên dữ liệu thật: giữ nguyên
luật nghiệp vụ, không có link CDN, `app.css` và `admin.css` vẫn tách nhau. Đưa vào từng phần
qua pull request nhỏ; sau mỗi pull request, app vẫn dùng được từ đầu đến cuối.

## Context

- Hành vi: `SPEC.md`, `docs/product/admin.md`. Khi prototype lệch spec, xem bảng "Điểm lệch".
- Quyết định đang có hiệu lực: ADR 0003, 0004, 0012 (Bootstrap 5.3.3 dùng chung, `admin.css`
  là lớp theme, `window.TG`, font tự phục vụ).
- Hiện trạng app (đã đọc ngày 30/10/2026):
  - Template admin: `templates/admin/` gồm overview, train, trips, trip-wizard, stations,
    routes, route-form, bookings, users, history và 4 fragment.
  - CSS: `admin/admin.css` 995 dòng, `app.css` 4.448 dòng. JS admin: 19 file.
  - Font hiện có: Manrope, JetBrains Mono, Be Vietnam Pro, Noto Sans...; chưa có Plus Jakarta Sans.
- Hiện trạng prototype: HTML tĩnh, dựng màn bằng JS từ dữ liệu mẫu, tự đặt `.btn`,
  `.btn-primary`, `.container`. Các class này trùng tên Bootstrap, đúng loại xung đột ADR 0012
  đã mô tả. Class theo trang (`dc-`, `cc-`, `cv-`, `tb-`, `bm-`, `us-`, `route-`) không trùng.
- Plan cũ: `docs/plans/active/redesign-admin-v2.md` còn mô tả hướng navy và chưa commit;
  `thiet-ke-lai-tau-chuyen.md` có thể chồng phạm vi, cần đối chiếu ở Đợt 0.

## Scope

In scope:

- Admin: khung và menu, Tổng quan, Tàu (Theo ngày, Bảng tính, trình hướng dẫn), Chuyến, Ga và
  tuyến, Booking, Người dùng, Lịch sử.
- Khách: trang chủ, tìm chuyến, chọn chỗ, hành khách, thanh toán, thành công, đăng nhập, đăng
  ký, vé của tôi, vé, trang lỗi.
- Phần backend mà giao diện mới cần đọc thêm (mục "Hợp đồng dữ liệu"), cập nhật
  `docs/product/`, ADR mới, `docs/WORKLOG.md` theo từng pull request.

Out of scope:

- Đổi luật nghiệp vụ hoặc schema. Dự kiến không cần migration; nếu phát sinh thì theo Flyway
  và cập nhật ERD.
- Dự án frontend riêng, thư viện mới, dark mode. Test tự động (ADR 0007).
- Chép JS dữ liệu mẫu của prototype (`admin-data.js`, `admin-network.js`, `data.js`) vào app.

## Approach

**Nguyên tắc chuyển đổi**

1. **Giữ Bootstrap, chép class theo trang.** Token prototype đổ vào biến `--bs-*`. `.btn`,
   ô nhập, ngăn kéo, hộp xác nhận, toast dùng Bootstrap và `window.TG` như bảng của ADR 0012.
   Chỉ chép CSS thành phần có tiền tố riêng.
2. **Server dựng HTML, JS chỉ lo tương tác.** Danh sách và lịch render bằng Thymeleaf từ view
   model; không chép cách dựng màn bằng JS của prototype. Lọc và tìm giữ tham số trên URL như
   controller hiện có. Vị trí thanh trong lịch theo giờ do Java tính và truyền qua biến CSS.
3. **Giữ JS đang chạy thật khi được.** `train-sheet*.js`, `trip-wizard.js`, `fan-out.js`,
   `station-draft.js`, `trip-sheet.js`, `seat-map.js` đổi markup và class, không viết lại luật.
4. **Câu lỗi vẫn lấy từ service.** Câu chữ màn hình theo `admin-copy-glossary`.

**Các đợt.** Mỗi đợt là một nhánh và một pull request (trừ khi ghi khác). Cỡ: S, M, L.

| Đợt | Nội dung | Thay đổi chính | Cỡ |
| --- | --- | --- | --- |
| 0 | Chốt prototype | Làm Lịch sử, dọn các bản thử, chụp ảnh chuẩn; chép bản chốt vào `docs/reports/ui-mau/` vì prototype không có git; đóng plan cũ; ADR mới | S |
| 1 | Nền admin | Plus Jakarta Sans vào `static/fonts/`; token E trong `admin.css`; menu trắng, nhóm "Danh mục"; toast, ngăn kéo. Mọi trang cũ đổi màu ngay nhưng vẫn phải dùng được | M |
| 2 | Người dùng | Tìm kiếm, phân trang; bảng danh bạ. Đợt nhỏ để kiểm chứng cách làm | S |
| 3 | Booking | Danh sách theo chuyến, dòng hành khách và ghế, phân trang theo chuyến; ngăn chi tiết; câu chữ hủy | M |
| 4 | Chuyến | Sổ tồn chỗ, thanh theo toa, chip Sắp đầy và Bán chậm, sắp xếp; ngăn chi tiết chuyến | M |
| 5 | Tổng quan | Bố cục E, dùng lại API `overview`; sửa câu cảnh báo còn lại theo glossary | M |
| 6 | Tàu | Lịch theo giờ, 4 ô thông tin, sơ đồ toa; đổi giao diện Bảng tính và trình hướng dẫn | L |
| 7 | Ga và tuyến, Lịch sử | Đường tàu cùng bảng tuyến trên một trang; giữ chèn ga, combobox, tạo nhiều tuyến; gộp trang Tuyến nếu được duyệt | L |
| 8 | Dọn admin | Xóa CSS và JS không còn dùng (biểu đồ cũ, `line-map` nếu bỏ); rà responsive toàn khu | S |
| 9 | Nền khách và trang chủ | Token và font trong `app.css`, navbar, footer, ảnh tối ưu, trang chủ | L |
| 10 | Luồng đặt vé | Tìm chuyến, chọn chỗ, hành khách, thanh toán, thành công; đợt rủi ro nhất phía khách | L |
| 11 | Tài khoản và vé | Đăng nhập, đăng ký; vé của tôi, vé, trang lỗi. Các màn này chưa có mẫu, dựng từ cùng hệ | M |

Admin đi trước vì bản thiết kế mới hơn và hai khu dùng CSS tách nhau, nên giao được độc lập.

**Hợp đồng dữ liệu cần thêm**

| Màn | Hiện có | Cần thêm |
| --- | --- | --- |
| Người dùng | `list(role)` | `q`, `page`; đếm tổng theo role; query có phân trang |
| Booking | `list(tab, q, customer, open)`; `TicketRepository` đã nạp vé kèm ghế theo lô | Nhóm theo chuyến; phân trang theo chuyến; đã bán của chuyến; Đã đi và Đã hủy xếp mới nhất trước |
| Chuyến | `TripListView` | Số đã bán theo toa, một query gộp cho cả trang (tránh N+1); `band`, `sort`; dùng chung luật Bán chậm với `OverviewService` |
| Tàu | `TrainScheduleView` | View model theo ngày: đoạn của từng chặng trong ngày (chặng qua đêm tách hai), khoảng chờ, ga cuối ngày, ô chặng mới |
| Ga và tuyến | `NetworkView`, `RouteRow` | Trạng thái chiều và chuyến sắp chạy cho từng cặp ga, nếu `NetworkView` chưa có đủ |

Thanh theo toa dùng **ghế thật** từ `TripSeat`. Cách "lấp lần lượt từng toa" chỉ là dữ liệu
mẫu của prototype.

**Điểm lệch giữa prototype và app/spec**

| Điểm | Prototype | App/spec | Đề xuất |
| --- | --- | --- | --- |
| Thêm chuyến | Luôn sang Bảng tính | Có trình hướng dẫn 4 bước (#28, #40) | Câu hỏi 3 |
| Trang Tuyến | Gộp vào Ga và tuyến | Có `/admin/routes` và form riêng | Câu hỏi 4 |
| Chèn ga | Chỉ có nút "Thêm ga" | Dấu `+`, combobox, tạo nhiều tuyến (#38, #39) | Giữ tính năng app, đổi giao diện |
| Tuyến nhảy ga | Hàng trong bảng, nhãn "Nối thẳng" | Cung trên sơ đồ | Theo prototype |
| Hủy khi đã khởi hành | Booking ẩn nút; Chuyến còn câu "Không hủy được" | Spec ghi câu giải thích cho cả hai | Câu hỏi 5 |
| Câu "Thanh toán chỉ là mô phỏng..." | Đã bỏ | Spec có | Bỏ, sửa spec |
| Hành khách trong ngăn chuyến | Tên người đặt (thiếu dữ liệu) | Họ tên và 4 số CCCD | Giữ theo app |
| Toast khi ngăn đang mở | Tự xử lý trong `admin.js` | Offcanvas Bootstrap | Thử lại "Hoàn tác" khi ngăn mở |

## Quyết định đã chốt

Phi yêu cầu làm theo mẫu và không trả lời riêng các câu còn lại, nên áp dụng đề xuất của plan:

1. Giữ Bootstrap (Phi chốt).
2. Admin trước, khách sau (Phi chốt).
3. Giữ trình hướng dẫn thêm chuyến, đổi theo theme mới.
4. Gộp trang Tuyến vào Ga và tuyến; `/admin/routes` chuyển hướng về đó.
5. Booking không hủy được thì ẩn nút. Ngăn chuyến giữ câu "Không hủy được: ...", đúng như mẫu, vì
   admin cần biết phải hủy booking trước.

## Risks And Recovery

- **Sửa nhầm nghiệp vụ khi đổi markup** (JS tìm phần tử qua selector hoặc `data-*`): giữ nguyên
  `data-*` và `id` mà JS đang dùng; đi hết kịch bản thử tay của màn đó.
- **Đợt 1 đổi token làm trang cũ vỡ:** đợt này phải đi qua mọi trang admin trước khi merge.
- **Luồng đặt vé:** không đổi luồng giữ ghế và hủy; thử hai người cùng đặt một ghế, số hành
  khách bằng số chỗ, công thức giá.
- **Nhiều dữ liệu:** query theo lô và phân trang; đo số query ở Booking và Chuyến.
- **Trộn CSS hai khu:** kiểm tra mỗi trang chỉ nạp một trong `app.css`, `admin.css`.
- **Nhóm đang làm song song:** pull request nhỏ, cập nhật từ `main` trước khi mở; báo nhóm trước
  Đợt 1 và Đợt 9 vì hai đợt này đụng layout chung.
- **Khôi phục:** mỗi đợt revert độc lập được. Ngoại lệ: các đợt trang (2-7) cần token của Đợt
  1, nên phải revert từ đợt mới nhất về trước.

## Progress

- [x] Phi duyệt plan.
- [x] Đợt 1 đến Đợt 8: admin, nhánh `feat/admin-redesign-v2`.
- [ ] Đợt 9 đến Đợt 11: khách, nhánh `feat/client-redesign-v2`.

## Decisions

ADR `0014-giao-dien-admin-v2.md` cho admin. Hệ giao diện khách sẽ có ADR riêng ở nhánh của nó.

## Validation

Mỗi pull request, theo `AGENTS.md`:

- Focused proof: đi hết kịch bản thử tay của màn trên trình duyệt với MySQL và dữ liệu mẫu, cả
  trường hợp lỗi và rỗng; đối chiếu ảnh chuẩn của prototype ở 1440, 1280, 768, 375 và 320px;
  không cuộn ngang; không lỗi console; tắt JS vẫn đọc được danh sách.
- Integration or end-to-end proof: một vòng khách đặt vé, booking hiện ở Booking, Chuyến, Tổng
  quan, admin hủy thì chỗ mở bán lại. Chạy cuối Đợt 8 và cuối Đợt 11.
- Repository-required checks: `.\mvnw.cmd compile`; `.\mvnw.cmd spring-boot:run`; không có link
  CDN (`grep` trên template); bản ghi `docs/WORKLOG.md` trong cùng commit; cập nhật
  `docs/product/` khi hành vi đổi.

## Result

Admin xong, xem bản ghi 30/10/2026 trong `docs/WORKLOG.md`. Khách đang làm.

# Execution Plan: Giai đoạn 2 - Database và màn hình quản trị đầu tiên

Date: 2026-10-01

## Status

Completed (04/10/2026). Phần S xong; phần A, B chuyển sang
`docs/plans/active/chia-viec-mvp.md`.

## Outcome

Database thật cho toàn bộ dự án chạy trong MySQL Docker, đủ 11 bảng, relation và
seed data demo. Trên nền đó, ba màn hình quản trị đọc dữ liệu thật:

| Phần việc | Người làm | Kết quả nhìn thấy được |
| --- | --- | --- |
| S. Database + Quản lý ga | Phi (cùng Claude) | `/admin/stations` thêm, sửa, xóa ga trên MySQL |
| A. Quản lý tàu (MVC thuần) | Chưa giao | `/admin/trains` thêm, sửa, xem danh sách tàu |
| B. Sơ đồ toa (JS `fetch`) | Chưa giao | `/admin/trains/{id}` vẽ toa và ghế từ API JSON |

Phần S là mẫu tham khảo cho A và B: cùng cách chia package, cách validate, cách
redirect và báo kết quả.

## Context

- Yêu cầu: `SPEC.md` mục 9-15, 34, 36-37; bản chắt lọc `docs/product/admin.md`,
  `docs/product/domain-model.md`.
- Quyết định: ADR 0001 (package theo feature, controller admin nằm trong feature),
  ADR 0004 (khu vực admin), ADR 0005 (Flyway).
- Plan cha: `docs/plans/active/mvp-traingo.md`, Giai đoạn 2.
- Cách chạy và tài khoản: `docs/RUNBOOK.md`.

## Scope

In scope:

- S: migration `V1` (schema) và `V2` (seed), entity và repository cho 11 bảng,
  module quản lý ga đầy đủ.
- A: danh sách, thêm, sửa tàu.
- B: trang chi tiết tàu chỉ đọc, sơ đồ toa vẽ bằng JavaScript thuần.

Out of scope:

- Thêm toa từ giao diện. Cần chốt trước một quy tắc: thêm toa vào tàu đã có chuyến
  thì các chuyến đó có sinh thêm `TripSeat` không (xem Decisions).
- Xóa tàu, sửa hoặc xóa toa: phiên bản đầu không có.
- Quản lý tuyến, chuyến, booking, người dùng: đợt sau.
- Auth và phân quyền theo role.

## Approach

### Pattern chung, theo đúng `station/`

```
<feature>/
├── controller/<Feature>AdminController.java   @RequestMapping("/admin/<feature>s")
├── model/<Entity>.java, <Entity>Form.java      entity có sẵn; form tự viết
├── repository/<Entity>Repository.java          có sẵn
└── service/<Feature>Service.java               nghiệp vụ + @Transactional
```

Những điểm phải làm giống `StationAdminController`:

1. `@ModelAttribute("activeNav")` trả tên mục sidebar. Tên admin trên header đã có
   `admin/controller/AdminShellModelAdvice` lo, không cần khai lại.
2. `@InitBinder` đăng ký `StringTrimmerEditor(true)`: cắt khoảng trắng, chuỗi rỗng
   thành `null` trước khi validate.
3. Form object có `@NotBlank`, `@Size`, `@Pattern` với thông báo tiếng Việt. Entity
   không bao giờ được bind thẳng từ request.
4. Lỗi validation thì render lại form, giữ dữ liệu đã nhập. Thành công thì
   `redirect:` về danh sách kèm `RedirectAttributes.addFlashAttribute(...)`.
5. Luật cần database (mã trùng) kiểm tra trong service, ném exception riêng;
   controller bắt và gắn lỗi vào field bằng `bindingResult.rejectValue`.
6. Không bắt `DataIntegrityViolationException` bên trong method `@Transactional`:
   transaction bị đánh dấu rollback-only và commit sẽ ném
   `UnexpectedRollbackException`. Bắt nó ở controller, như `StationAdminController`.
7. Gỡ phần dữ liệu giả của màn hình mình khỏi `AdminController`, `AdminService` và
   `admin/model/`.

### A. Quản lý tàu (MVC thuần)

File: `train/model/TrainForm.java`, `train/service/TrainService.java`,
`train/controller/TrainAdminController.java`, `templates/admin/trains.html` (sửa),
`templates/admin/train-form.html` (mới).

| Method | URL | Việc |
| --- | --- | --- |
| GET | `/admin/trains` | Danh sách |
| GET | `/admin/trains/new` | Form thêm |
| POST | `/admin/trains` | Lưu tàu mới |
| GET | `/admin/trains/{id}/edit` | Form sửa |
| POST | `/admin/trains/{id}` | Lưu thay đổi |

- `code`: bắt buộc, chữ cái không dấu và chữ số, tối đa 10 ký tự, tự viết hoa,
  không trùng (`uk_trains_code`). `name`: bắt buộc, tối đa 100 ký tự.
- Không có xóa.
- Nút xem chi tiết trong bảng trỏ tới `/admin/trains/{id}` (trang của B).
- Bảng tạm bỏ hai cột Số toa và Số ghế. Mở rộng nếu còn thời gian: tính cả hai
  bằng **một** query gom nhóm trong `CoachRepository`, không query từng tàu.
- Gỡ handler `trains` khỏi `AdminController`, `TRAINS` và `trains()` khỏi
  `AdminService`, xóa `admin/model/Train.java`.

### B. Sơ đồ toa hiển thị bằng `fetch`

File: `train/service/TrainLayoutService.java`, `train/controller/TrainDetailController.java`
(trang), `train/controller/TrainLayoutApiController.java` (`@RestController`),
`templates/admin/train-detail.html`, `static/js/admin/train-detail.js`, style ô
ghế thêm vào cuối `admin.css` với class tiền tố `tg-`.

| Method | URL | Trả về |
| --- | --- | --- |
| GET | `/admin/trains/{id}` | Trang khung: tên tàu, chỗ trống để JS vẽ |
| GET | `/api/admin/trains/{id}/layout` | JSON bên dưới |

```json
{
  "trainId": 5, "trainCode": "SE19", "trainName": "Hà Nội - Đà Nẵng",
  "coaches": [
    {
      "id": 17, "coachNumber": 1, "coachType": "SEAT", "capacity": 32, "priceModifier": 0,
      "seats": [ { "id": 385, "code": "A01", "type": "SEAT", "cabinNumber": null } ]
    }
  ]
}
```

- Vẽ toa `SEAT` thành lưới 4 cột như `SPEC.md` mục 14; toa `SLEEPER` nhóm theo
  cabin, mỗi cabin 2x2 như mục 15. Có trạng thái đang tải, tàu chưa có toa, và lỗi.
- Hàm vẽ nhận JSON và trả về DOM, không tự gọi `fetch`. Giai đoạn 4 (khách chọn
  ghế) sẽ dùng lại nó, chỉ thêm trạng thái `AVAILABLE` / `HELD` / `BOOKED`.
- Seed có sẵn 8 tàu, mỗi tàu 4 toa, nên trang có dữ liệu ngay.

Bẫy đã kiểm tra trên code hiện tại:

1. `spring.jpa.open-in-view=false` và mọi `@ManyToOne` là `LAZY`. Trả entity từ
   `@RestController` sẽ gặp `LazyInitializationException`. Service phải dựng DTO
   (Java `record`) bên trong `@Transactional(readOnly = true)`. Lấy ghế của cả tàu
   bằng một query sắp xếp sẵn theo số toa rồi mã ghế, không query từng toa.
2. `/api/**` đang yêu cầu đăng nhập. Hết session thì Spring Security chuyển `fetch`
   sang trang login và trả HTML với status 200, nên `response.json()` ném lỗi. Bắt
   lỗi đó và báo "phiên đăng nhập đã hết, tải lại trang".
3. `admin.js` biến mọi bảng `.datatable` thành datatable ngay lúc trang tải, trước
   khi `fetch` xong. Phần do JS vẽ không được mang class `datatable`.
4. Layout admin chỉ nhận `title` và `main`. Đặt
   `<script th:src="@{/js/admin/train-detail.js}" defer></script>` bên trong
   `<main>`, không sửa `admin/layout/base.html`.
5. Gán chữ từ server bằng `textContent`, không dùng `innerHTML`.

### Phân chia file để không đụng nhau

| | S (đã xong) | A | B |
| --- | --- | --- | --- |
| Java | `station/**` | `train/model/TrainForm`, `train/service/TrainService`, `train/controller/TrainAdminController` | `train/service/TrainLayoutService`, `train/controller/TrainDetailController`, `train/controller/TrainLayoutApiController` |
| Template | `stations.html`, `station-form.html` | `trains.html`, `train-form.html` | `train-detail.html` |
| JS/CSS | dùng chung `admin.js` (`data-confirm`) | không có | `js/admin/train-detail.js`, cuối `admin.css` |
| File chung phải sửa | | `AdminController`, `AdminService`, xóa `admin/model/Train.java` | không có |

Chỗ chắc chắn conflict: `docs/WORKLOG.md`, vì ai cũng thêm một dòng đầu bảng. Giữ
cả hai dòng khi merge. Không ai sửa entity, migration, `SecurityConfig` hay
`admin/layout/base.html`; cần đổi schema thì báo nhóm và viết migration `V3` mới.

## Risks And Recovery

- Sửa một migration đã chạy làm Flyway từ chối khởi động trên máy người khác.
  Phòng: không sửa `V1`, `V2`; đổi schema bằng file mới. Phục hồi trên máy dev:
  `docker compose down -v` rồi chạy lại ứng dụng.
- Ngày trong seed cũ dần: sau 14 ngày không còn chuyến sắp tới. Phục hồi: làm mới
  dữ liệu theo `docs/RUNBOOK.md`.
- Entity lệch migration làm ứng dụng không khởi động (`ddl-auto=validate`). Đó là
  chủ ý: lỗi lộ ra ngay thay vì ở một trang nào đó.

## Progress

- [x] S1. Flyway, `ddl-auto=validate`, MySQL chạy giờ +07:00.
- [x] S2. `V1__create_schema.sql`: 11 bảng, 14 khóa ngoại, unique và `CHECK` có tên.
- [x] S3. Entity và repository cho 11 bảng.
- [x] S4. `V2__seed_demo_data.sql`.
- [x] S5. Module quản lý ga, tên admin chuyển sang `AdminShellModelAdvice`,
      hộp xác nhận `data-confirm` trong `admin.js`.
- [x] S6. Sửa giao diện gặp trên đường: bảng admin trên màn hình hẹp, mũi tên sắp
      xếp đè chữ, bảng "Booking gần đây" ở dashboard bị bóp.
- [ ] A. Quản lý tàu.
- [ ] B. Sơ đồ toa.

## Decisions

- 2026-10-01: Flyway, 11 bảng, seed demo đầy đủ, admin `admin@traingo.vn`. Phi
  chọn. Chi tiết: ADR 0005.
- 2026-10-01: Màn hình admin của feature nằm trong package của feature. Phi chọn.
  Chi tiết: ADR 0001.
- 2026-10-01: Mã ga và mã tàu chỉ gồm chữ cái không dấu và chữ số, tối đa 10 ký
  tự, tự viết hoa. Bỏ cột "Trạng thái" của ga vì spec không có.
- 2026-10-01: Tàu không có xóa; toa chỉ thêm, chưa sửa hay xóa. Giới hạn toa:
  `SEAT` 1-80 chỗ, `SLEEPER` 4-40 giường và chia hết cho 4. Ghi ở
  `docs/product/domain-model.md`.
- 2026-10-01: Seed dùng tám tàu, mỗi chiều một tàu, thay cho ba tàu như đề xuất
  ban đầu. Một tàu không thể chạy cả hai chiều trong cùng một ngày, nên cần tám tàu
  thì mỗi tuyến mới có chuyến hằng ngày.
- Chưa quyết: thêm toa vào tàu đã có chuyến thì các chuyến `SCHEDULED` sắp tới có
  sinh thêm `TripSeat` cho ghế mới không. Phải chốt trước khi làm form thêm toa.

## Validation

- S, đã chạy ngày 01/10/2026:
  - Ứng dụng khởi động trên MySQL 8.4: Flyway áp V1, V2; `validate` không báo lỗi.
  - Số dòng khớp thiết kế: 32 toa, 768 ghế, 114 chuyến, 10.944 `trip_seats`.
  - Bất biến seed: tổng tiền booking bằng tổng giá vé, giá vé đúng công thức, ghế
    của booking hủy về `AVAILABLE`, không còn chuyến quá khứ ở `SCHEDULED`.
  - Bảy lệnh ghi sai bị database chặn: tuyến trùng ga, tuyến trùng cặp, chỗ trùng
    trong chuyến, `HELD` thiếu người giữ, `CONFIRMED` chưa `PAID`, xóa ga đang
    dùng, trạng thái chuyến không hợp lệ.
  - Chromium qua Playwright: 28/28 bước luồng quản lý ga đạt; bước kiểm tra lỗi
    console chỉ bắt được request 404 cố ý tới ga không tồn tại. Không tràn trang ở
    320, 375, 768, 1280, 1920px.
- A, B: chạy thật trên trình duyệt với seed data; ghi kết quả vào mục Result.
- Repository-required checks: `.\mvnw.cmd test` trước khi báo xong.

## Result

Đóng ngày 04/10/2026.

- S hoàn thành ngày 01/10/2026, commit `9748dcd`, bằng chứng ở mục Validation: Flyway áp
  V1 và V2 trên MySQL 8.4, `validate` không lỗi, bảy lệnh ghi sai bị database chặn,
  28/28 bước luồng quản lý ga đạt trên Chromium.
- A và B chưa bắt đầu ở plan này. Chúng chuyển sang
  `docs/plans/active/chia-viec-mvp.md` thành T03 (Quản lý tàu) và T06 (Chi tiết tàu và
  sơ đồ toa), cùng hướng dẫn chi tiết trong `docs/plans/active/huong-dan-task/`.
- Giới hạn còn lại khi đóng: `.\mvnw.cmd test` chạy 0 test vì `app/src/test` bị xóa
  trong working tree; migration chưa chạy trên H2. Cả hai thuộc T01.

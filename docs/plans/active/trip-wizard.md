# Execution Plan: Trình hướng dẫn nối chặng (#28)

Date: 2026-10-27

## Status

Active

## Outcome

Admin mở `/admin/trains/{id}/trips/new` từ nút "Thêm chuyến" (hoặc "Thêm chuyến
đầu tiên") trên banner trang Tàu, đi qua 4 bước Đi đâu, Khi nào, Giá, Xác nhận và
thêm được một chuyến nối vào cuối lịch của tàu. Chuyến mới có đúng 96 chỗ
`AVAILABLE`. Server kiểm tra lại toàn bộ luật nối chặng; API gọi sai luật trả 409
và không tạo dòng nào; hai admin thêm cùng lúc thì chỉ một người thành công.

## Context

- Issue #28 (AC1-AC8), ADR 0011, `docs/product/admin.md` ("Quản lý tàu", "View
  Bảng tính", "Quản lý chuyến"), `docs/product/domain-model.md`.
- Mẫu: `docs/reports/ui-mau/admin/index.html#/noi-chuyen/SE1`: `TripWizardBody`,
  `SchedulePreview`, `Wizard`, `DestChoices`, `DepFields`, `ArrFields`,
  `PriceField`, `useLegDraft`, `validateLeg`; CSS ở khối `css-smart` và `css-page`.
- Phụ thuộc đã merge: #19 `CoachLayout` (PR #44), #20 khung admin (PR #45), #25
  trang Tàu và `TrainScheduleService` (PR #52), #36 View Bảng tính (PR #55).

## Phần đã có trên `main` (không làm lại)

#36 merge trước #28 nên đã dựng sẵn phần server mà issue #28 định làm ở Giai
đoạn 1-2. Đối chiếu từng sub-task:

| Sub-task trong issue | Trạng thái | Nơi đã có |
| --- | --- | --- |
| `suggestedMinutes`, `suggestedPrice` | Xong, đúng công thức | `TrainScheduleService` |
| `defaultDeparture(earliest)` | Có logic, đang private | `TrainScheduleService.firstDailyDeparture` |
| `RouteRepository.findByOriginStationIdAndDestinationStationId` | Xong, nạp sẵn hai ga | `RouteRepository` |
| `RouteRepository.findFromStation` | **Chưa có** | |
| `TrainRepository.findByIdForUpdate` | Xong, khóa dòng và loại tàu đã xóa | `TrainRepository` |
| `LegRequest`, `CreateTripsRequest` (tối đa 14 chặng) | Xong | `model/request/` |
| `LegRuleException` → 409 `fields["legs[i].x"]` | Xong | `common/exception/`, `ApiExceptionHandler` |
| `TripCreationService.createLegs` | Xong: khóa tàu, ga đi = ga cuối, 30 phút quay đầu, không ở quá khứ, giờ đến sau giờ đi, giá chia hết 1.000, 96 chỗ | `service/TripCreationService` |
| Kiểm tra "lịch đã đổi" | Xong: `originStationId` gửi lên khác ga tàu đang đứng thì 409 "Tàu đã ở X. Tải lại trang rồi thử lại." | `createLegs` |
| `POST /api/admin/trains/{id}/trips` → `201 { tripIds }` | Xong | `TrainAdminApiController` |
| `NextLegOptions`, `GET .../next-leg` | **Chưa có** | |
| `GET /admin/trains/{id}/trips/new`, view, JS, CSS | **Chưa có** | |
| Nút "Thêm chuyến" trên banner | **Chưa có** | |

Lỗi validate (`@Valid`) của từng chặng trả 400 với khóa `legs[0].basePrice`...,
cùng dạng khóa với lỗi luật 409, nên JS xử lý chung một cách.

## Điểm lệch giữa issue và repo

1. **Banner nằm ở fragment.** Issue ghi sửa `templates/admin/train.html`, nhưng
   banner "Chặng tiếp theo đi từ…" là section `.next-task` trong
   `templates/admin/fragments/train-day-view.html`. Nút đặt ở đó. View Bảng tính
   không có banner nên không có nút.
2. **Trùng tên `Destination`.** `model/response/Destination.java` là thẻ ga trên
   trang chủ. Ga đến của trình hướng dẫn là record lồng `NextLegOptions.Destination`.
3. **`StationOption` không có id.** Record này là `(code, name)` cho trang khách,
   còn trình hướng dẫn gửi `stationId`. Dùng record lồng `NextLegOptions.Station(id,
   name)`, không sửa kiểu của trang khách.
4. **`originStationId` luôn được gửi.** Issue ghi field này chỉ dùng khi tàu chưa
   có chuyến, nhưng server dùng nó để phát hiện lịch đã đổi (AC8). Trình hướng dẫn
   gửi nó cho mọi tàu, giống View Bảng tính.
5. **Dải 14 ngày.** Mẫu hiện 7 ngày với ‹ ›, AC3 ghi 14 ngày. Hiện 7 ngày một lần,
   ‹ › lật giữa hai trang, tổng 14 ngày tính từ ngày của giờ sớm nhất.
6. **Gợi ý giá trong mẫu đã cũ.** Mẫu ghi "Giường nằm cộng …"; giá giờ là một giá
   cho mọi chỗ (chốt 21/10/2026). Câu gợi ý chỉ còn "Gợi ý X theo 1.000đ/km."
7. **"Tạo tuyến và chọn"** trong danh sách ga chưa có tuyến thuộc #40. Ở #28 danh
   sách đó chỉ để đọc: tên ga và số km.

## Scope

In scope:

- `GET /api/admin/trains/{id}/next-leg?origin=` trả `NextLegOptions`.
- `GET /admin/trains/{id}/trips/new` và trang `admin/trip-wizard`.
- `static/js/admin/trip-wizard.js`, CSS `.wz*`, `.pv*`, `.dests`, `.dest`,
  `.chips`, `.chip`, `.confirm-list`, `.wz-done`, dải ngày trong `admin.css`.
- Nút trên banner (AC1).
- Cập nhật `docs/product/admin.md`, một dòng `docs/WORKLOG.md`.

Out of scope:

- "Nhiều chặng tới một ga", "Tạo tuyến và chọn": #40.
- Hủy chuyến: #29 (đã có).
- Thay đổi hợp đồng của `POST .../trips`.

## Approach

1. **Server**
   - `TrainScheduleService`: đổi `firstDailyDeparture` thành
     `public static LocalDateTime defaultDeparture(LocalDateTime earliest)`, View
     Bảng tính và `planLegs` dùng chung.
   - `RouteRepository.findFromStation(stationId)`: `join fetch` ga đến, xếp theo
     lý trình ga đến.
   - `model/response/NextLegOptions` (record, kèm record lồng `Station`,
     `Destination`, `TripSummary`): `trainId`, `trainCode`, `hasChain`, `origin`
     (null khi tàu chưa có chuyến và chưa chọn ga), `earliestDeparture`,
     `defaultDeparture`, `origins` (mọi ga theo lý trình, để chọn ga xuất phát),
     `destinations`, `withoutRoute`, `lastTrips`.
   - `TrainScheduleService.nextLegOptions(trainId, originId, now)`: nạp tàu chưa
     xóa; có chuyến thì ga đi là ga đến của chuyến cuối và bỏ qua `origin`; giờ
     sớm nhất dùng `earliestDeparture` có sẵn (khớp banner). Mỗi tuyến đi ra là
     một `Destination` với chiều "Vào Nam" khi lý trình ga đến lớn hơn, ngược lại
     "Ra Bắc"; `minutes = suggestedMinutes(km)`, `suggestedPrice(km)`. Ga không có
     tuyến kèm khoảng cách theo lý trình. `lastTrips` là 2 chuyến chưa hủy cuối.
   - `TrainAdminApiController`: `GET /{id}/next-leg`.
   - `TrainAdminController`: `GET /{id}/trips/new` nạp tàu qua `TrainService.findById`
     (404 với tàu đã xóa), view `admin/trip-wizard`.
2. **Giao diện**
   - `templates/admin/trip-wizard.html`: link về tàu, tiêu đề, `ol.wz-steps`,
     `.wz-body`, `.wz-preview`; data-* mang URL của `next-leg`, `trips` và trang tàu.
   - `trip-wizard.js`: object trạng thái `{ origin, destination, day, depTime,
     arrTime, arrOffset, arrTouched, price, priceTouched, step }`; dùng lại các hàm
     ngày giờ thuần của `train-sheet-derive.js` (`parseIso`, `toIso`, `atTime`,
     `addDays`, `startOfDay`, `daysBetween`, `fmtTime`, `fmtDayTime`, `dayLabel`)
     thay vì viết bản thứ hai. Giờ đến = giờ đi + `minutes` của ga đến, giá lấy
     `suggestedPrice` từ server; ô đã sửa tay không bị ghi đè.
   - Lỗi server `legs[0].x` đưa về bước chứa ô x: `originStationId`,
     `destinationStationId` về bước 1; `departureAt`, `arrivalAt` về bước 2;
     `basePrice` về bước 3. Lỗi không gắn ô thì hiện toast.
   - "Thêm chuyến từ B" gọi lại `next-leg` và về bước 1.
3. **Banner**: nút `btn btn-primary` trong `.next-task`, chữ theo AC1.
4. **Tài liệu**: mục trình hướng dẫn trong `docs/product/admin.md`.

## Risks And Recovery

- JS và server tính giờ sớm nhất khác nhau một chút: trang làm tròn lên 5 phút,
  server chỉ cắt tới phút. Trình hướng dẫn chặn theo con số của trang (chặt hơn),
  server vẫn là chốt cuối.
- Hai admin cùng thêm: khóa dòng tàu trong `createLegs`, cộng kiểm tra
  `originStationId`. Thử bằng hai request song song.
- Thay đổi chỉ thêm code và một method được mở `public`; không có migration. Hoàn
  tác bằng cách revert commit.

## Progress

- [x] Phân tích phần đã có và điểm lệch (27/10/2026).
- [x] Server: `defaultDeparture`, `findFromStation`, `NextLegOptions`, `next-leg`,
      route trang (27/10/2026).
- [x] Trang, JS, CSS của trình hướng dẫn; nút trên banner (27/10/2026).
- [x] `docs/product/admin.md`, `docs/WORKLOG.md` (27/10/2026).
- [x] Kịch bản thử bằng tay (dưới đây), trừ phần người khác đọc qua pull request.

## Decisions

- 2026-10-27: Giữ endpoint `next-leg` như issue thay vì để trang tự dựng từ
  `GET /api/admin/network`: một lần gọi có đủ ga đến, chiều, thời gian chạy, giá
  gợi ý, giờ sớm nhất và 2 chuyến cuối, và "Thêm chuyến từ B" không cần tải lại trang.
- 2026-10-27: Các quyết định ở mục "Điểm lệch giữa issue và repo".
- 2026-10-27: Trình hướng dẫn dùng lại các hàm ngày giờ của `train-sheet-derive.js`
  (export thêm `atTime`) thay vì viết bản thứ hai. Ô giờ nhận "8", "845", "8h45".
- 2026-10-27: Phi chốt chữ trên giao diện là "Thêm chuyến" thay cho "Nối chặng" của AC1:
  nút "Thêm chuyến" / "Thêm chuyến đầu tiên", tiêu đề "Thêm chuyến cho SE1" / "Chuyến đầu
  tiên của SE1", "Chuyến mới đi tới đâu?", "Thêm chuyến từ B". "Nối chặng" chỉ còn là tên
  luật trong tài liệu. "Thêm" khớp "Thêm tàu", "Thêm N chuyến" đã có.
- 2026-10-27: Thêm nhãn "Đi tới ga nào?" trên danh sách ga đến khi tàu chưa có chuyến;
  mẫu không có nhãn này nên hai nhóm nút nằm liền nhau khó phân biệt.

## Validation

Kịch bản thử bằng tay (issue #28):

- [x] SE1: nối Huế → Đà Nẵng (chuyến 153); 96 `trip_seats` đều `AVAILABLE`.
- [x] SE1: giờ đi 11:00 ngày 04/11 báo "Sớm nhất 04/11 11:15: tàu đến Ga Huế lúc 10:45 và
      cần 30 phút quay đầu."; nút 06:00, 08:00 bị khóa.
- [x] SE2: chọn Hà Nội rồi Thanh Hóa (chuyến 154), "Thêm chuyến từ Ga Thanh Hóa" mở bước 1
      với tiêu đề "Thêm chuyến cho SE2".
- [x] Giờ đến 07:00 với giờ đi 08:00: "Giờ đến phải sau giờ đi."; "Dùng giờ gợi ý 09:45"
      đưa về giá trị tính.
- [x] `POST` thẳng: đi sớm, ga đi sai, không có tuyến, giờ đến ≤ giờ đi, giá lẻ đều 409;
      giá âm 400; số dòng `trips` và `trip_seats` không đổi.
- [x] Hai `POST` song song cho SE2: một 201, một 409 "Tàu đã ở Ga Vinh…"; hai tab giao
      diện: tab sau quay về bước 1 với câu báo.
- [x] 1440px và 390px: không cuộn ngang, không lỗi console.

Repository-required checks: `.\mvnw.cmd compile`, `.\mvnw.cmd spring-boot:run`
với MySQL và dữ liệu mẫu, thử trên trình duyệt.

## Result

Đủ AC1-AC8 trên ứng dụng chạy thật (27/10/2026). Database local còn các chuyến thử 153
(SE1), 154-161 và 163 (SE2). Chuyến 162 (SE1 Đà Nẵng → Huế) không do các lần thử này tạo. Còn lại: pull request ghi `Closes #28` và một người khác đọc qua,
sau đó chuyển plan sang `docs/plans/completed/`.

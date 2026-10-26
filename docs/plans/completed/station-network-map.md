# Execution Plan: Trang Ga và tuyến — sơ đồ dọc Bắc–Nam (#27)

Date: 2026-10-26

## Status

Completed (2026-10-26)

## Outcome

`/admin/stations` thành trang "Ga và tuyến": bên trái sơ đồ đường tàu dọc Bắc–Nam (ga theo lý trình, đoạn kề, cung tuyến dài, dấu `+` chèn ga); bên phải một ngăn đổi theo thứ admin bấm (tổng quan, ga, sửa ga, cặp ga, chèn ga). Mọi thao tác gọi REST `/api/admin/...`, cập nhật sơ đồ không tải lại trang. Thay danh sách ga và form ga cũ.

## Context

- Issue #27 (assign `thanhphong97`), branch `feat/station-network-map`. Luật: `docs/product/admin.md` mục "Quản lý ga", ADR 0011, ADR 0012.
- Mẫu giao diện: `docs/reports/ui-mau/admin/index.html#/ga` — `layout`/`LineMap` (~dòng 4782–4837), `StationPanel`, `NewStationPanel`, `NetworkPage` (~4884–4990), `PairDetail` (~4348), `useStationDraft` (~4050); CSS khối `css-page`, `css-smart`.
- Server đã có: `StationService` (create/update/delete, chặn trùng mã/km), `RouteService` (findAll có số chuyến, create có khóa ga, delete chặn tuyến có chuyến), `ApiExceptionHandler` + `ApiError` (`{message, fields}`: 400/404/409), `admin.js` có `TG.api`, `TG.showFieldErrors`, `TG.clearFieldErrors`, `TG.confirm`, `TG.toast`.
- #24 (PR #50) **không có ma trận** và không có REST tuyến (quyết định 25/10).
- Seed V5: 8 ga HNI 0, THA 175, VIN 319, DHO 522, HUE 688, DNG 791, NTR 1315, SGN 1726; 16 tuyến = 7 cặp kề × 2 chiều + HNI⇄SGN × 2; 18 chuyến chỉ trên 14 tuyến kề (HNI⇄SGN không có chuyến). Mọi ga có 4 tuyến.
- Hồ sơ tiến độ: `docs/plans/active/tien-do-ticket-ca-nhan.md`.

## Scope

In scope: server API ga + tuyến, sơ đồ JS, trang + các ngăn, CSS, gỡ form ga cũ, docs.

Out of scope:

- **Ma trận tuyến** (AC6 của issue) — người dùng chốt 26/10 bỏ. Ngăn mặc định là "Tổng quan" có bộ chọn cặp ga; nút đầu trang là "Tổng quan".
- Tự điền theo danh mục ga thật, gợi ý tuyến sau khi thêm ga (#38); tạo tuyến từ một ga tới nhiều ga (#39).
- Test tự động (ADR 0007).

## Approach

Java tính từ `app/src/main/java/com/voyagego/traingo/`, resources từ `app/src/main/resources/`.

### Giai đoạn 1 — Server

1. `service/StationService.java`
   - `update`: đổi mã (so sau `Station.normalizeCode`) của ga đang có tuyến → `StationCodeLockedException` (mới, `common/exception`, kế thừa `BusinessRuleException`, field `code`, câu "Ga đã có tuyến nên giữ mã.").
   - Trùng km (create và update): `DuplicateStationKmMarkException` mang số km và ga đang giữ km đó → "Km 791 đã là lý trình của Ga Đà Nẵng." (field `kmMark`). Repository trả `Optional<Station>` (`findByKmMark`, `findByKmMarkAndIdNot`) thay cho `existsBy…` tương ứng.
   - `delete`: đếm bằng `RouteRepository.countByOriginStationIdOrDestinationStationId(id, id)`; > 0 → `StationInUseException(count)` → "Không xóa được: ga đang có N tuyến. Xóa các tuyến đó trước."
   - `freeKmBetween(int lo, int hi)` (readOnly): `lo >= hi` → 400; bắt đầu từ `(lo + hi) / 2`, dò xen kẽ ra hai phía trong khoảng mở `(lo, hi)`, bỏ km đã có ga (một query lấy các km trong khoảng); hết chỗ → 409 "Không còn km trống giữa hai ga."
2. `model/response/NetworkView.java`: `record NetworkView(List<StationNode> stations, List<RouteEdge> routes)`, lồng `record StationNode(long id, String code, String name, String city, String address, int kmMark, long routeCount)` và `record RouteEdge(long id, long originId, String originCode, long destinationId, String destinationCode, int distanceKm, long upcomingTrips, long totalTrips)`.
3. `service/NetworkService.java`: `network()` ghép `StationService.findAll()` và `RouteService.findAll()`; `routeCount` đếm trong bộ nhớ từ danh sách tuyến đã nạp. Không gọi database trong vòng lặp.
4. `controller/NetworkAdminApiController.java` (`@RestController`, constructor injection, JSON):
   - `GET /api/admin/network` → `NetworkView`.
   - `POST /api/admin/stations` (201) và `PUT /api/admin/stations/{id}` nhận `@Valid @RequestBody StationRequest`, trả `StationNode`; `DELETE /api/admin/stations/{id}` → 204.
   - `GET /api/admin/stations/free-km?lo=&hi=` → `{ "km": n }`.
   - `POST /api/admin/routes` (`@Valid @RequestBody RouteCreateRequest`, 201, trả id các tuyến đã tạo); `DELETE /api/admin/routes/{id}` → 204. Lỗi `differentStations` phải ra `fields.destinationStationId`.
   - `@RequestBody` không qua `StringTrimmerEditor`: JS trim và gửi `null` cho ô trống.
5. `controller/StationAdminController.java`: chỉ còn `GET /admin/stations` → view `admin/stations`. Xóa handler `/new`, `/{id}/edit`, `POST`, `/{id}/delete` và `templates/admin/station-form.html`; sửa mọi link còn trỏ tới chúng.

### Giai đoạn 2 — Sơ đồ `static/js/admin/line-map.js`

- `TG.lineMap(container, network, { selected, onStation, onPair, onGap })`, JS thuần (không React), vẽ SVG.
- Vị trí: `y[0] = 28`, `y[i] = max(y[i-1] + 60, 28 + km / kmCuối * 640)`; đường dọc `x = 104`.
- Đoạn giữa hai ga kề: `<g class="seg both|one|none">` theo số chiều đã có. Cặp không kề có tuyến: `<g class="arc both|one">` cong bên trái, `dx = min(LX - 14, 26 + (y2 - y1) * 0.1)`.
- Mỗi `<g>`: `role="button"`, `tabindex="0"`, phản hồi Enter/Space, có `<title>` và `aria-label`.
- Ga: `<button class="vstop-btn">` đặt tuyệt đối (chấm, tên, "km N"). Dấu `+`: `<button class="vgap">`, aria "Chèn ga giữa A và B". Đối tượng đang chọn có class `sel`.
- Tên hiển thị trên sơ đồ bỏ tiền tố "Ga ". Nhãn: "Huế ⇄ Đà Nẵng, 103 km, cả hai chiều" / "…, một chiều" / "…, chưa có tuyến"; cung: "Hà Nội ⇄ Sài Gòn, tuyến dài 1726 km, cả hai chiều".

### Giai đoạn 3 — Trang và ngăn

- `templates/admin/stations.html` viết lại: đầu trang "Ga và tuyến", "8 ga · 16 tuyến" (JS cập nhật), nút "Tổng quan" chỉ hiện khi ngăn không ở tổng quan; `.net-split` gồm `.net-map` (chữ Bắc, sơ đồ, chữ Nam, `.net-legend` 4 kiểu: Hai chiều, Một chiều, Chưa có tuyến, Tuyến dài chạy thẳng) và `.net-panel`; các `<template>` cho ngăn tổng quan, ga, sửa ga, cặp ga, chèn ga.
- `static/js/admin/stations.js` (tách thêm file kebab-case nếu quá ~200 dòng): tải `GET /api/admin/network`, giữ trạng thái `{ type: 'overview' | 'station' | 'pair' | 'gap', ... }`, vẽ sơ đồ + ngăn. Thao tác gọi `TG.api`; thành công thì tải lại network, vẽ lại, `TG.toast`, **không tải lại trang**; lỗi dưới ô bằng `TG.showFieldErrors`; xóa qua `TG.confirm`. Dưới 1100px, đổi lựa chọn thì `scrollIntoView` tới ngăn. Nạp bằng `<script defer>` sau `admin.js`.
  - **Tổng quan:** câu hướng dẫn; form chọn cặp ga (hai select theo lý trình, nút "Xem cặp ga"; chọn cùng một ga → lỗi dưới select thứ hai).
  - **Ga:** km, tỉnh, địa chỉ (thiếu thì "Chưa nhập tỉnh"/"Chưa nhập địa chỉ"), nút Sửa, Xóa. Ga có tuyến: Xóa bị khóa kèm câu "Không xóa được: ga đang có N tuyến. Xóa các tuyến đó trước."
  - **Sửa ga (trong ngăn):** 5 ô; ga có tuyến → ô mã `readonly` kèm "Ga đã có tuyến nên giữ mã"; Lưu/Hủy.
  - **Cặp ga:** tiêu đề "A ⇄ B", "N km theo lý trình"; hai dòng A → B và B → A: km và số chuyến sắp chạy, hoặc "Chưa có" kèm nút "Tạo chiều này" (km điền sẵn theo lý trình, sửa được). Chiều đã có: nút xóa, bị khóa (kèm lý do) khi tuyến đã có chuyến.
  - **Chèn ga:** tiêu đề "Chèn ga giữa A và B"; ô tên, mã, tỉnh, địa chỉ, km; km điền sẵn từ `free-km`; km ngoài khoảng hai ga → cảnh báo (vẫn cho lưu). Lưu xong ga mới hiện trên sơ đồ và ngăn chuyển sang ga đó.
- Hook kiểm thử (script Playwright dựa vào, giữ đúng tên):
  - `#net-count` (chữ "8 ga · 16 tuyến"), `#net-overview-btn`, `.net-map`, `.net-panel`, tiêu đề ngăn `#net-panel-h`, `.net-legend`.
  - Ga `.vstop-btn` nằm trong `.vstop` (class `sel` đặt trên `.vstop`); `.vgap`; `g.seg`/`g.arc` (class `sel` đặt trên `<g>`).
  - Ngăn ga: `[data-action=edit-station]`, `[data-action=delete-station]`. Form sửa/chèn: ô `name`, `code`, `city`, `address`, `kmMark`, nút `[type=submit]`, nút "Hủy". Cảnh báo km `#km-range-warning`, chữ "Km nằm ngoài khoảng giữa Đà Nẵng (791) và Nha Trang (1315)." (ẩn bằng `hidden` khi hết lỗi).
  - Ngăn cặp ga: mỗi chiều là một phần tử `[data-direction="HUE-VIN"]` (mã ga đi-ga đến), chứa chữ "Huế → Vinh", ô `input[name=distanceKm]` + `[data-action=create-route]` khi chưa có, hoặc `[data-action=delete-route]` khi đã có.
  - Tổng quan: `<form id="pair-picker">` với `select[name=a]`, `select[name=b]` (option là tên ga), nút `[type=submit]` "Xem cặp ga".
- CSS lấy từ khối `css-page`/`css-smart` của mẫu (`.net-split`, `.net-map`, `.vline`, `.vstop`, `.vgap`, `.panel-body`, `.facts`, `.pane-actions`, `.net-legend`, `.seg`/`.arc` both/one/none, `.sel`), thêm vào `static/css/admin/admin.css`. ≥1100px hai cột, `.net-map` sticky; <1100px xếp dọc; 390px không cuộn ngang.

## Risks And Recovery

- Server luôn chặn: nút khóa chỉ là gợi ý, gọi API thẳng vẫn phải bị 409.
- Đổi chữ ký `DuplicateStationKmMarkException`/`StationInUseException`: kiểm mọi chỗ gọi.
- Không migration; hoàn tác bằng revert commit. Dữ liệu thử xóa lại bằng chính API/UI.

## Progress

- [x] Lập kế hoạch + testcase, người dùng duyệt (26/10).
- [x] Codex lượt 1: server (`ck:cook --auto --no-test`). Claude compile ngoài sandbox (sandbox Codex không đọc được `~/.m2`) + API-01…13: đạt ngay, không vòng sửa.
- [x] Codex lượt 2: sơ đồ + trang. Codex hết hạn mức trước bước tự review; Claude review diff, chạy UI-01…14, DB-01.
- [x] Sửa lỗi bấm chuột vào đoạn (Claude, người dùng duyệt vì Codex hết hạn mức); gỡ comment thừa.
- [x] Docs, WORKLOG, commit.

## Decisions

- 2026-10-26: bỏ ma trận tuyến (AC6) vì #24 không có ma trận; ngăn mặc định "Tổng quan" có bộ chọn cặp ga để mở được cả cặp chưa có tuyến (người dùng).
- 2026-10-26: thêm `POST/DELETE /api/admin/routes` (issue không liệt kê, nhưng ngăn cặp ga cần).
- 2026-10-26: `routeCount` đếm trong bộ nhớ từ danh sách tuyến đã nạp thay vì `@Query` group by — cùng kết quả, không thêm query.
- 2026-10-26: testcase là kịch bản + Playwright chạy ngoài repo (ADR 0007).
- 2026-10-26: API gọi khi chưa đăng nhập trả 401 JSON theo ADR 0013 (không chuyển trang đăng nhập).
- 2026-10-26: nút `+` đặt bên trái đường dọc thay vì đè lên đoạn như mẫu, và vùng trống của hàng ga không chặn chuột: ga cách nhau 60px nên nút ga (44px) cùng nút `+` (24px) che kín phần bấm được của đoạn.
- 2026-10-26: ở ≥1100px sơ đồ sticky, cao tối đa bằng màn hình và cuộn bên trong; chú thích nằm cuối khung sơ đồ.

## Validation

Đăng nhập admin seed; CSRF lấy từ `meta[name=_csrf]`. Script Playwright nằm ngoài Git.

| ID | Bước | Mong đợi |
|---|---|---|
| API-01 | GET `/api/admin/network` | 200; 8 ga theo km tăng; 16 tuyến; HUE routeCount 4; 2 tuyến HNI⇄SGN totalTrips 0 |
| API-02 | GET network bằng CUSTOMER / ẩn danh | 403 / chuyển trang đăng nhập, không lộ dữ liệu |
| API-03 | PUT HUE kmMark 791 | 409, `fields.kmMark` = "Km 791 đã là lý trình của Ga Đà Nẵng." |
| API-04 | PUT HUE code "HUX" | 409, `fields.code` = "Ga đã có tuyến nên giữ mã." |
| API-05 | PUT HUE đổi địa chỉ, giữ mã; rồi trả lại | 200; 200 |
| API-06 | POST ga tên trống, mã "A B", km -1 | 400, `fields` có `name`, `code`, `kmMark` |
| API-07 | POST QNG "Quảng Ngãi" km 928 → POST lại mã QNG km 930 → DELETE QNG | 201 / 409 `fields.code` / 204; network còn 8 ga |
| API-08 | DELETE HUE | 409 "Không xóa được: ga đang có 4 tuyến. Xóa các tuyến đó trước." |
| API-09 | free-km lo=791 hi=1315; lo=100 hi=101; lo=500 hi=400 | `{"km":1053}`; 409; 400 — đều dạng ApiError |
| API-10 | POST route HUE→VIN 369 bothWays=false → POST lại → POST VIN→VIN → DELETE tuyến mới | 201 (1 tuyến) / 409 / 400 `fields.destinationStationId` / 204 |
| API-11 | DELETE route HUE→DNG (có chuyến) | 409 "Không xóa được: tuyến đã có N chuyến." |
| API-12 | PUT/DELETE ga id 999999; DELETE route 999999 | 404 dạng ApiError |
| API-13 | POST station không header CSRF | 403, không 500 |
| UI-01 | Mở `/admin/stations` 1440px | 8 nút ga có tên + "km N" đúng thứ tự; hai ga kề cách nhau ≥ 60px; "Bắc" trên, "Nam" dưới; chú thích 4 kiểu |
| UI-02 | Đếm đối tượng sơ đồ | 7 `.seg.both`, 0 `.seg.none`, 1 `.arc.both` (HNI⇄SGN), 7 `.vgap` |
| UI-03 | Đọc aria-label | đoạn = "Huế ⇄ Đà Nẵng, 103 km, cả hai chiều"; cung = "Hà Nội ⇄ Sài Gòn, tuyến dài 1726 km, cả hai chiều"; `+` = "Chèn ga giữa Đà Nẵng và Nha Trang" |
| UI-04 | Chỉ bàn phím: ga → Enter; đoạn → Enter; cung → Space; `+` → Enter | ngăn đổi đúng loại; đối tượng chọn có `.sel` |
| UI-05 | Bấm Huế | km 688, "Huế", "2 Bùi Thị Xuân"; Xóa khóa + "Không xóa được: ga đang có 4 tuyến. Xóa các tuyến đó trước." |
| UI-06 | Sửa Huế: ô mã readonly + câu khóa; km 791 → Lưu; Hủy; sửa địa chỉ → Lưu → trả lại | lỗi dưới ô km đúng câu API-03; Hủy không đổi gì; lưu có toast, không tải lại trang |
| UI-07 | Bấm `+` Đà Nẵng–Nha Trang | tiêu đề "Chèn ga giữa Đà Nẵng và Nha Trang"; km 1053; nhập 1400 → cảnh báo; nhập Quảng Ngãi/QNG/928 → Lưu → 9 ga, ngăn ga QNG, 2 `.seg.none` mới, DNG⇄NTR thành `.arc.both` |
| UI-08 | Ngăn QNG → Xóa → xác nhận | 8 ga, về Tổng quan, DNG–NTR lại là `.seg.both` |
| UI-09 | Bấm đoạn Huế–Đà Nẵng | hai chiều 103 km + số chuyến sắp chạy; hai nút xóa bị khóa |
| UI-10 | Tổng quan: chọn Huế + Vinh → Xem cặp ga → "Tạo chiều này" Huế → Vinh (km 369 sẵn) → xóa nó; chọn cùng ga | ban đầu hai chiều "Chưa có"; sau tạo có `.arc.one`, đầu trang 17 tuyến; sau xóa hết cung, 16 tuyến; cùng ga → lỗi dưới select |
| UI-11 | Cung HNI⇄SGN → xóa Sài Gòn → Hà Nội → tạo lại 1726 | `.arc.one` rồi `.arc.both` |
| UI-12 | Nút "Tổng quan" | ẩn ở tổng quan, hiện ở ngăn khác, bấm thì về tổng quan |
| UI-13 | 1440 / 1024 / 390px | 1440: hai cột, cuộn trang sơ đồ đứng yên; 1024: ngăn dưới sơ đồ, bấm ga → ngăn vào màn hình; 390: không cuộn ngang; không lỗi JS |
| UI-14 | `/admin/stations/new`, `/admin/stations/1/edit`; `/admin/routes`, `/admin/trains`, `/`; CUSTOMER `/admin/stations` | 404; 200; 403 |
| DB-01 | SQL trước/sau | 8 ga (HUE giữ nguyên), 16 tuyến, 18 chuyến |

- Repository-required checks: `.\mvnw.cmd compile`; `.\mvnw.cmd spring-boot:run` với MySQL (Flyway, `ddl-auto=validate`); một dòng `docs/WORKLOG.md` cùng commit; PR ghi `Closes #27`.

## Result

Đã kiểm chứng 26/10/2026 trên MySQL Server local: `mvnw compile` đạt; `spring-boot:run` khởi động, Flyway 7 migration up to date, Hibernate validate qua.

- API: 29/29 kiểm tra PASS (API-01…13 và DB không đổi).
- UI: 55/55 kiểm tra PASS (UI-01…14, thêm kiểm tra mọi đoạn/cung bấm được bằng chuột, DB-01). Ảnh 1440/1024/390px đã xem.
- DB sau thử: 8 ga (Huế giữ nguyên), 16 tuyến, 18 chuyến.

Giới hạn: chưa thử bằng trình đọc màn hình thật; ở hàng ga cách 60px, phần đoạn bấm được bằng chuột chỉ khoảng 16px. Chưa push/PR.

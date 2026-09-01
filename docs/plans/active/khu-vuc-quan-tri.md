# Execution Plan: Khu vực quản trị

Date: 2026-08-27

## Status

Active

## Outcome

`/admin` và sáu route con render bằng một shell dashboard riêng — sidebar,
header, footer, bảng có tìm kiếm và phân trang, biểu đồ — đủ bảy màn hình của
`docs/product/admin.md`. Khu vực này chạy được khi không có mạng, và landing
page cùng toàn bộ trang khách không đổi một pixel nào.

Bằng chứng chấp nhận: `.\mvnw.cmd test` xanh; mở `/admin` sau khi đăng nhập
thấy đủ bảy màn hình; tab Network không có request nào ra ngoài `localhost`;
mở `/` ngay sau đó vẫn đúng như kết quả đã ghi ở
`nen-tang-giao-dien-va-landing.md` và không nạp `admin.css` hay `admin.js`.

## Context

- Yêu cầu gốc: `SPEC.md` mục 33–40, chắt lọc ở `docs/product/admin.md`.
- Nền giao diện: `docs/decisions/0003-he-thong-giao-dien.md` (không CDN, layout
  fragment có tham số, token `--traingo-`, `line-height` heading 1.28).
- Quyết định của việc này: `docs/decisions/0004-khu-vuc-quan-tri.md`.
- Nguồn template: một dự án Spring Boot riêng ở `D:\IUH\Eclipse\demo`, không
  thuộc repo này.
- Trạng thái code trước khi làm: `app/` mới chỉ có landing page và
  `HomeController`; chưa có package `admin`.

## Scope

In scope:

- Tải tám thư viện về `static/vendor/`.
- `static/css/admin/admin.css` và `static/js/admin/admin.js` port từ template,
  đã gỡ sạch branding và reference CDN.
- `templates/admin/layout/base.html` cùng ba fragment `header`, `sidebar`,
  `footer`.
- Bảy trang: `dashboard`, `stations`, `routes`, `trains`, `trips`, `bookings`,
  `users` — markup tĩnh trên dữ liệu placeholder.
- `com.voyagego.traingo.admin.AdminController` với bảy route.
- Luật `/admin/**` trong `SecurityConfig` và `AdminControllerTest`.

Out of scope:

- Mọi truy cập database. Dữ liệu là hằng số trong `AdminController`.
- Nút Thêm / Sửa / Xóa. Markup có, `disabled`, chưa nối backend.
- `/admin/trains/{id}` và việc quản lý toa.
- Phân quyền theo role. Chưa có user store nên chỉ chặn tới mức
  `authenticated()`.
- Theme customizer của template (đã chốt bỏ ở ADR 0004).

## Approach

1. Tải vendor, kiểm tra file phục vụ được qua HTTP chứ không trả HTML trang
   login.
2. Port `admin.css` và `admin.js`.
3. Dựng shell cùng `dashboard.html` và `AdminController`, chạy thử trước khi
   nhân ra sáu trang còn lại.
4. Sáu trang còn lại.
5. Quét branding bằng `grep` như một bước riêng.
6. `SecurityConfig` và `AdminControllerTest`.
7. ADR 0004, plan này, `AGENTS.md`.

## Risks And Recovery

- **Đụng giao diện landing.** Triệu chứng chỉ hiện ra bằng mắt, không test nào
  bắt. Phòng bằng namespace ba tầng và bằng bước kiểm chứng đo lại
  `.btn-primary`, `line-height` heading và danh sách request của `/` sau khi
  admin đã xong.
- **Quên một reference CDN trong 4.900 dòng CSS.** Trang vẫn chạy khi có mạng
  nên không ai phát hiện cho tới lúc demo. Phòng bằng test khẳng định HTML của
  `/admin` không chứa `cdn.jsdelivr.net`, `unpkg.com`, `fonts.googleapis.com`,
  `cdn.ckeditor.com`.
- **Đặt sai thư mục font của boxicons.** `boxicons.min.css` tham chiếu
  `url(../fonts/…)`; sai một cấp là mất icon, log không báo gì.
- **Sót branding của tác giả template.** Phòng bằng bước quét `grep` riêng và
  bằng một test khóa lại.
- **Cú pháp fragment có tham số viết sai.** Chỉ lộ lúc render. Phòng bằng cách
  thử shell với riêng `dashboard.html` trước.

Recovery: toàn bộ thay đổi là file mới, cộng hai dòng trong `SecurityConfig` và
một mục trong `AGENTS.md`. Không đụng database, không có migration. Hỏng thì xóa
thư mục `admin/` ở ba tầng và `git checkout` hai file kia.

## Progress

- [x] Tám thư viện trong `static/vendor/`, tổng ~5,3MB. Bảng kích thước từng
      file ở ADR 0004.
- [x] `admin.css` 4.924 dòng: bỏ `@import` Google Fonts, đổi `font-family` sang
      stack của `app.css`, xóa 17 rule của footer cũ, thêm khối override
      TrainGo.
- [x] `admin.js` 115 dòng: giữ toggle sidebar, đồng bộ breakpoint, back-to-top;
      thêm khởi tạo datatable với nhãn tiếng Việt.
- [x] Shell `admin/layout/base.html` cùng `header`, `sidebar`, `footer`.
- [x] Bảy trang nội dung và `AdminController` với bảy route.
- [x] Quét branding: `grep -rniE "nazareth|leandro|opensource\.org|MIT License|
      linkedin|look-and-feel|github\.com"` trên `templates/admin`,
      `static/css/admin`, `static/js/admin` — không có kết quả.
- [x] `SecurityConfig` thêm `/admin/**`, `AdminControllerTest` 13 test.
- [x] ADR 0004, plan này, mục mới trong `AGENTS.md`.
- [x] Kiểm chứng bằng trình duyệt thật qua Playwright. Kết quả ở phần Result.

## Decisions

- 2026-08-27: Namespace ba tầng; static admin ở `css/admin/` và `js/admin/`
  chứ không phải `static/admin/`; vendor tám thư viện; bỏ theme customizer; gỡ
  sạch branding của template gốc. Chi tiết và lý do:
  `docs/decisions/0004-khu-vuc-quan-tri.md`.

## Validation

- Focused proof: `@WebMvcTest` cho `AdminController` — bảy route trả đúng view,
  shell render được, sidebar sáng đúng mục, không có CDN, không có branding, và
  `/admin` chặn khách chưa đăng nhập.
- Integration proof: chạy ứng dụng thật, duyệt bảy màn hình bằng Chromium, đo
  lại các bất biến của landing ở 375 / 768 / 1280px.
- Repository-required checks: `.\mvnw.cmd test` phải xanh.

## Result

Hoàn thành ngày 27/08/2026.

| Kiểm tra | Kết quả |
| --- | --- |
| `.\mvnw.cmd test` | `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0` |
| Bảy route `/admin/…` | 200, đúng view, đúng `<title>`, sidebar sáng đúng mục |
| Lỗi hoặc cảnh báo JavaScript | không có trên cả bảy trang |
| Request ra ngoài `localhost:8081` | 0 |
| Request lỗi | 0 |
| Font icon `.bi` | `bootstrap-icons` nạp được trên cả bảy trang |
| Biểu đồ ApexCharts ở `/admin` | render ra `<svg>` |
| Bảng datatable | sort, tìm kiếm, phân trang chạy, nhãn tiếng Việt |
| Màu mục sidebar đang mở | `rgb(15, 108, 189)` — bằng `--traingo-primary` |
| Tràn ngang ở `/admin` | không, trên cả bảy trang |
| `/admin` khi chưa đăng nhập | chuyển hướng về `/login` |
| `/` nạp `admin.css` hay `admin.js` | không |
| `.btn-primary` ở `/` | `rgb(15, 108, 189)` — không đổi |
| `line-height / font-size` của heading ở `/` | tất cả 1.28 — không đổi |
| Tràn ngang ở `/` tại 375 / 768 / 1280px | không |
| Branding của template gốc trong file mới | không có |

Giới hạn còn lại:

- Dữ liệu bảy màn hình là hằng số trong `AdminController`, thay bằng repository
  ở Giai đoạn 2 của `mvp-traingo.md`.
- Nút Thêm / Sửa / Xóa đang `disabled`, chưa có form và chưa có endpoint.
- Chưa có `/admin/trains/{id}`, nên chưa quản lý được toa và ghế.
- `/admin/**` mới chặn tới mức `authenticated()`. Bất kỳ tài khoản nào đăng nhập
  được cũng vào được admin cho tới khi có role.
- Ô tìm kiếm trên header trỏ `/admin/bookings` nhưng tham số `q` chưa được xử lý.
- Ứng dụng dùng để kiểm chứng chạy trên H2 in-memory (`-Dspring-boot.run.useTestClasspath=true`
  cùng override datasource) vì máy kiểm chứng không có Docker trên PATH. Bảy màn
  hình không chạm database nên kết quả không phụ thuộc điều đó, nhưng chưa có
  lần nào mở `/admin` trên MySQL thật.
- CKEditor, Quill, EditorJS và boxicons đã vendor nhưng chưa trang nào dùng.

Chưa chuyển sang `docs/plans/completed/` vì các giới hạn trên đều được gỡ trong
những giai đoạn kế tiếp của `mvp-traingo.md`.

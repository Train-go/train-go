# Execution Plan: Khung giao diện admin và client (#20, #21)

Date: 2026-10-22

## Status

Active. Phi duyệt ngày 23/10/2026; Q1-Q5 chốt ngày 22/10/2026. Bản nháp trong
`docs/session/` đã xóa khi chuyển sang đây. File này đi cùng branch
`feat/admin-shell` (#20); pull request #21 trỏ về đây.

## Outcome

Toàn bộ giao diện, cả khu khách lẫn khu admin, chạy trên **cùng một bản
Bootstrap** tự phục vụ (`static/vendor/bootstrap/`, 5.3.3) và dùng **font tự
phục vụ** trong `static/fonts/`:

- **Admin (#20, branch `feat/admin-shell`):** menu trái ba nhóm theo
  `docs/reports/ui-mau/admin/index.html`. `admin.css` là lớp theme đặt lên
  Bootstrap để ra màu, chữ, bo góc của mẫu, cộng khung menu và vài thành phần
  Bootstrap không có. Chữ Manrope, số và mã dùng JetBrains Mono. `window.TG`
  bọc Offcanvas, Modal, Toast của Bootstrap thành ngăn kéo, hộp xác nhận,
  toast, và thêm hàm gọi API. Bảy trang admin hiện có vẫn đọc được.
- **Client (#21, branch `feat/client-shell-errors`):** navbar, thanh 4 bước,
  footer theo `docs/reports/ui-mau/client/`, màu navy và cobalt, chữ Be Vietnam
  Pro. Trang 403, 404, 500 tiếng Việt. Mọi `@RestController` trả lỗi dạng
  `{ "message", "fields" }`.

Hai issue gặp nhau ở một chỗ: `TG.api` của #20 đọc đúng dạng lỗi mà #21 trả.
Hợp đồng đó được chốt ở mục "Hợp đồng chung", nên hai branch làm song song được.

## Context

- Issue: https://github.com/Train-go/train-go/issues/20,
  https://github.com/Train-go/train-go/issues/21.
- Mẫu: `docs/reports/ui-mau/admin/index.html` (khối `css-admin` dòng 2093-2354,
  khối `css-page` dòng 2490-2726, khung menu trong `js-app` dòng 5559-5590),
  `docs/reports/ui-mau/client/*.html`. Mẫu không dùng Bootstrap; plan này chỉ
  lấy màu, chữ, kích thước và bố cục của mẫu, rồi dựng lại bằng Bootstrap.
- Quyết định liên quan: ADR 0003 (Bootstrap tự phục vụ, token `--traingo-`),
  ADR 0004 (template dashboard bên ngoài, sẽ bị ADR 0012 thay một phần),
  ADR 0006 (package theo tầng), ADR 0007 (không test tự động).
- `AGENTS.md` mục "Hai khu vực giao diện": không nạp `app.css` và `admin.css`
  trên cùng một trang, không link CDN.

### Hiện trạng code (đọc ngày 22/10/2026 trên `origin/main` = `a138d97`)

Bootstrap và font:

- Bootstrap 5.3.3 đã có và đã được nạp ở **cả hai** khu, cùng một file
  `vendor/bootstrap/`. Khu admin nạp thêm CSS của template ngoài đè lên.
- Font **đã tải về nhưng chưa trang nào dùng**. `static/fonts/` có Be Vietnam
  Pro (400-700), Inter, Onest, IBM Plex Sans; `@font-face` của chúng chỉ nằm
  trong `font-candidates.css`, mà file này chỉ trang Font Lab
  `/design/font-families.html` nạp. `app.css` đặt `body` là `system-ui`.
  `admin.css` đòi `"Nunito"`, font không có trong repo, nên trình duyệt rơi về
  font hệ thống. Manrope và JetBrains Mono chưa có.

Admin:

- `templates/admin/layout/base.html` nạp Bootstrap CSS và JS, `bootstrap-icons`,
  `simple-datatables`, `apexcharts`, rồi `admin.css` và `admin.js`. Có
  fragment `header`, `sidebar`, `footer`, nút "lên đầu trang".
- `static/css/admin/admin.css` dài 4.986 dòng, port từ template ngoài, có reset
  `*` và bảng màu `--color-*`.
- `static/js/admin/admin.js`: thu gọn sidebar, nút lên đầu trang, listener
  `submit` trên `document` hỏi `window.confirm` cho form có `data-confirm`,
  khởi tạo `simple-datatables` cho mọi bảng `.datatable`.
- Logout hiện chỉ nằm trong dropdown của `header.html`. Header lấy tên từ
  `AdminShellModelAdvice` (`adminName = "Quản trị viên"`).
- Bảy trang con đã dùng class Bootstrap (`btn`, `table`, `form-control`,
  `is-invalid`, `invalid-feedback`, `alert`, `badge`, `row`/`col-md-*`), cộng
  vài class riêng của template: `pagetitle`, `breadcrumb`, `section`, `card`,
  `table-card`, `table-card-header`, `table-modern`, `table-toolbar`,
  `table-actions`, `datatable`.
- `vendor/` còn `boxicons`, `ckeditor`, `editorjs`, `quill` không trang nào
  dùng; `apexcharts` chỉ phục vụ `dashboard.html`.
- `bootstrap-icons` trong `vendor/` là v1.11.3, cùng bản với mẫu, có đủ icon
  của menu.

Client:

- `templates/layout/base.html` nạp Bootstrap và `app.css`; navbar là Bootstrap
  `navbar` có nút collapse. Chưa nạp `bootstrap-icons`.
- `app.css` đã map token `--traingo-*` sang biến `--bs-*` của Bootstrap.
- Footer hiện có dòng "Đồ án môn học. Thanh toán trên hệ thống là mô phỏng...".

Server:

- `SecurityConfig`: `/`, static, `/vendor/**` mở; `/admin/**` và mọi request
  khác cần đăng nhập; form login mặc định.
- Exception: `StationNotFoundException` (`@ResponseStatus(NOT_FOUND)`),
  `DuplicateStationCodeException`, `DuplicateStationKmMarkException`,
  `StationInUseException`, đều kế thừa thẳng `RuntimeException`, message tiếng
  Anh dành cho lập trình viên. Câu tiếng Việt cho người dùng hiện viết trong
  `StationAdminController`.
- Chưa có `@RestController` nào.

## Scope

In scope: các Acceptance Criteria và sub-task của #20 và #21, điều chỉnh theo
các quyết định dưới đây.

Out of scope:

- Nội dung từng trang admin (sơ đồ ga, ma trận tuyến, lịch tàu...) và trang chọn
  chuyến (#26).
- Đăng nhập, đăng ký, phân quyền theo role (#22). Hai khung chỉ chừa chỗ.
- Nâng Bootstrap lên bản 5.3 mới hơn. Hai khu dùng chung file 5.3.3 đang có.
- Chuyển MySQL từ Docker sang MySQL cài trên máy: task riêng.

## Quyết định

| | Câu hỏi | Chốt |
| --- | --- | --- |
| Q1 | Khu admin có dùng Bootstrap không? | **Có**, dùng chung bản Bootstrap với client. Thành phần của mẫu dựng bằng thành phần Bootstrap tương ứng, không chép class của mẫu (xem bảng ở #20 Giai đoạn 2) |
| Q2 | Người chưa đăng nhập gõ đường dẫn không tồn tại thì thấy gì? | **Trang 404** (phương án B): mặc định mở, chỉ khóa các tiền tố riêng tư. Xem bên dưới |
| Q3 | Navbar client trên điện thoại | Dưới 900px còn logo, "Đăng nhập" và nút menu mở danh sách ba mục, dùng collapse của Bootstrap |
| Q4 | Footer trang chủ | Giữ đủ các cột link như mẫu. Link chưa có trang thì để `href="#"`. Bỏ dòng "Thanh toán... mô phỏng" |
| Q5 | Trang Tổng quan cũ | Xóa `dashboard.html`, method thống kê không còn ai gọi và `apexcharts` trong branch này |

**Về Q2.** Spring Security đứng trước mọi controller. Với dòng
`anyRequest().authenticated()` hiện tại, nó chặn người chưa đăng nhập ngay khi
request tới, lúc đó chưa ai biết đường dẫn có tồn tại hay không, nên người chưa
đăng nhập gõ `/abc` bị đẩy về trang đăng nhập chứ không thấy trang 404. Phi
chọn đảo lại: **mặc định mở, chỉ khóa danh sách tiền tố riêng tư**. Danh sách
lấy từ bảng quyền của T02 (`huong-dan-task/T02-dang-ky-dang-nhap-phan-quyen.md`,
Bước 5):

```text
/admin/**, /api/admin/**                                  -> ADMIN (#22 siết role)
/booking/**, /my-trips/**, /tickets/**, /api/trips/**     -> CUSTOMER (#22 siết role)
mọi đường dẫn còn lại                                     -> ai cũng vào được
```

Trong #21 các tiền tố này dùng `authenticated()`, vì chưa có tài khoản khách;
#22 đổi thành `hasRole(...)`.

Cái giá của lựa chọn này: một trang riêng tư mới nằm ngoài các tiền tố trên sẽ
mở cho người lạ. Ba lớp chặn:

1. Comment ngay trên khối `requestMatchers` của `SecurityConfig` nói rõ luật
   "trang riêng tư mới phải nằm dưới một tiền tố đã khóa, hoặc thêm tiền tố mới
   vào đây".
2. Thêm dòng đó vào mục "Kiểm tra trước khi báo xong" của
   `huong-dan-task/README.md`, để mọi task tự kiểm.
3. Service vẫn tự kiểm quyền sở hữu (booking của ai), như `docs/product/`
   đang đòi, nên lọt lớp URL thì dữ liệu của người khác vẫn không lộ.

Đây là quyết định bảo mật dài hạn, nên ghi thành ADR 0013 và sửa dòng "Mọi
đường dẫn còn lại: Đã đăng nhập" trong bảng quyền của T02 thành "Ai cũng vào
được".

## Approach

### Hợp đồng chung giữa hai issue

```text
Lỗi API:  HTTP 400 | 404 | 409
          Content-Type: application/json
          { "message": "Câu tiếng Việt cho người dùng", "fields": { "code": "..." } }
          "fields" luôn có mặt, là {} khi lỗi không gắn với ô nào.

CSRF:     <meta name="_csrf" content="...">
          <meta name="_csrf_header" content="X-CSRF-TOKEN">
          Có ở cả layout admin lẫn layout client.

Font:     @font-face nằm trong chính stylesheet của từng khu (app.css,
          admin.css), trỏ vào /fonts/**. Font đặt qua --bs-body-font-family
          để mọi thành phần Bootstrap nhận theo.
```

`TG.api` (#20) phải chịu được cả phản hồi không theo hợp đồng: lỗi 403 do sai
CSRF và lỗi 500 đi qua `BasicErrorController` của Spring Boot, trả
`{ timestamp, status, error, path }` chứ không có `message` tiếng Việt. Khi đó
`TG.api` ném lỗi với câu chung "Có lỗi xảy ra, vui lòng thử lại." và
`fields = {}`. `TG.api` gửi header `Accept: application/json` để Spring trả JSON
thay vì trang HTML.

### Thứ tự làm

Làm #21 Giai đoạn 1 (quy ước exception, khoảng nửa ngày) trước, vì nó nhỏ và
chốt hợp đồng lỗi bằng code thật; sau đó làm #20 cho xong sớm vì mọi task admin
chờ nó; phần còn lại của #21 làm sau hoặc song song.

Chỗ hai branch và #22 cùng đụng: `docs/WORKLOG.md` (mỗi branch thêm một dòng),
`SecurityConfig.java` (#21 và #22), `AGENTS.md` (#20).

### #20 Khung admin

**Giai đoạn 1: font**

Tải bản variable `.woff2` của Manrope và JetBrains Mono, mỗi họ hai subset
`latin` và `vietnamese`, đặt tên theo kiểu `inter-400-700-*.woff2` đang có. Lưu
vào `static/fonts/manrope/`, `static/fonts/jetbrains-mono/`, kèm `OFL.txt`.
Kiểm tra 4 byte đầu mỗi file là `wOF2`. Khai `@font-face` có `unicode-range`
như `font-candidates.css`.

**Giai đoạn 2: `admin.css` là theme của Bootstrap**

Viết lại file từ đầu, không sửa dần file 4.986 dòng cũ. Thứ tự trong file:

1. `@font-face` của Manrope, JetBrains Mono.
2. Token của mẫu trên `:root` (`--bg`, `--surface`, `--ink`, `--ink-2`,
   `--muted`, `--line`, `--line-strong`, `--accent`, `--accent-soft`,
   `--accent-strong`, `--accent-line`, `--danger`, `--danger-soft`, `--warn`,
   `--font`, `--mono`, `--r-control`, `--r-panel`).
3. Map token sang biến của Bootstrap, giống cách `app.css` đang làm:
   `--bs-body-font-family`, `--bs-font-monospace`, `--bs-body-bg`,
   `--bs-body-color`, `--bs-border-color`, `--bs-primary` và `--bs-primary-rgb`,
   `--bs-danger`, `--bs-link-color`, `--bs-border-radius` (8px),
   `--bs-border-radius-lg` (12px), `--bs-focus-ring-color`. Với `.btn-primary`,
   `.btn-outline-secondary`, `.table`, `.form-control`, `.badge`, đặt biến
   `--bs-btn-*`, `--bs-table-*`... trên chính class đó vì Bootstrap 5.3 đọc
   chúng ở đây.
4. Khung: `.admin-shell`, `.admin-side`, `.admin-main`, `.admin-now`,
   `.page-head`, lấy kích thước từ `.v2*` của khối `css-page`.
5. Vài thành phần Bootstrap không có, giữ tên của mẫu vì không trùng class
   Bootstrap: `.icon-btn`, `.tag` (nhãn trạng thái có chấm màu), `.kv`,
   `.empty`, `.toolbar`, `.code`.

Thành phần của mẫu dựng bằng Bootstrap như sau. Đây cũng là danh sách class
các task admin sau này dùng:

| Mẫu | Bootstrap |
| --- | --- |
| `.btn` / `.btn.secondary` / `.btn.quiet` | `.btn.btn-primary` / `.btn.btn-outline-secondary` / `.btn.btn-link` |
| `.btn.danger` / `.btn.danger-quiet` | `.btn.btn-danger` / `.btn.btn-outline-danger` |
| `.btn.sm` / `.btn.lg` | `.btn-sm` / `.btn-lg` |
| `.input`, `.select`, `.input-affix` | `.form-control`, `.form-select`, `.input-group` + `.input-group-text` |
| `.field`, `.label`, `.hint`, `.error` | `.mb-3`, `.form-label`, `.form-text`, `.invalid-feedback` + `.is-invalid` |
| `.table`, `.table-wrap` | `.table.align-middle`, `.table-responsive` |
| `.seg` | `.btn-group` với `.btn-check` |
| `.note`, `.note.warn`, `.note.danger` | `.alert.alert-secondary`, `.alert-warning`, `.alert-danger` |
| `.sheet` | `.offcanvas.offcanvas-end` |
| `.dialog` | `.modal` |
| `.toasts`, `.toast` | `.toast-container`, `.toast` |
| `.sr-only`, `.skip` | `.visually-hidden`, `.visually-hidden-focusable` |

**Giai đoạn 3: template khung**

1. `admin/layout/base.html`: giữ `page(title, content)`. Nạp Bootstrap CSS,
   `bootstrap-icons`, `admin.css`, rồi `bootstrap.bundle.min.js`, `admin.js`.
   Bỏ `simple-datatables`, `apexcharts`. Thêm hai thẻ meta CSRF, link "Bỏ qua
   menu" trỏ tới `#main`, một `.toast-container` và một `.modal` dùng chung cho
   `TG.confirm`.
2. `admin/fragments/sidebar.html`: brand "TrainGo" về `/admin/trains`, ba nhóm
   menu với icon như mẫu, mục đang mở dùng `aria-current="page"` theo
   `activeNav`. Đáy menu là đồng hồ, rồi khối người dùng gồm chỗ chừa tên (#22
   điền) và **form `POST /logout` chuyển từ header sang**. Không chuyển thì xóa
   header là mất nút đăng xuất cho tới khi #22 xong.
3. Dưới 900px menu thành thanh trên cùng cuộn ngang; ẩn đồng hồ và nhãn nhóm
   như mẫu. Nút đăng xuất vẫn còn, dạng `.icon-btn` có `aria-label`.
4. Xóa `header.html`, `footer.html` của admin và `AdminShellModelAdvice`.
5. `AdminController.dashboard` trả `redirect:/admin/trains`. Xóa
   `dashboard.html` và method không còn ai gọi trong `AdminService`.
6. Xóa `vendor/apexcharts`, `vendor/simple-datatables`, `vendor/boxicons`,
   `vendor/ckeditor`, `vendor/editorjs`, `vendor/quill` sau khi grep xác nhận
   không còn chỗ nào tham chiếu. Giữ `vendor/bootstrap`, `vendor/bootstrap-icons`.

**Giai đoạn 4: `admin.js`**

Viết lại file, gắn mọi thứ vào `window.TG`. Bootstrap đã lo phần khó (khóa
focus, `Esc`, bấm nền, animation), `TG` chỉ thêm phần còn thiếu:

- `TG.sheet.open(id, triggerEl)` / `close(id)`: gọi
  `bootstrap.Offcanvas.getOrCreateInstance`. Bootstrap chỉ trả focus về nút đã
  mở khi mở bằng `data-bs-toggle`, nên `TG` nghe `hidden.bs.offcanvas` rồi tự
  `triggerEl.focus()`. Khi `shown.bs.offcanvas`, focus vào ô nhập đầu tiên.
- `TG.confirm({ title, body, confirmLabel, danger })`: điền modal dùng chung,
  trả `Promise<boolean>`. `Esc`, "Hủy", bấm nền trả `false`. Khi `danger`, nút
  xác nhận là `.btn-danger` và focus mặc định vào "Hủy".
- Giữ cách khai báo `data-confirm` trên form đang có, chỉ đổi phần xử lý:
  listener `submit` trên `document` luôn `preventDefault()`, chờ `TG.confirm`,
  được `true` thì gọi `form.submit()`. `form.submit()` không phát lại sự kiện
  `submit`, nên không lặp vô hạn.
- `TG.toast(message, { label, run })`: tạo `.toast` trong `.toast-container`,
  `delay: 6000`. Đang có 2 cái thì ẩn cái cũ nhất trước. Có `label` thì thêm
  nút gọi `run`. Container có `aria-live="polite"`.
- `TG.toastAfterReload(message, action)`: ghi vào `sessionStorage`, đọc và xóa
  khi trang tải. Bọc `try/catch` vì `sessionStorage` có thể ném lỗi.
- `TG.api(url, { method, body })`: như "Hợp đồng chung".
- `TG.showFieldErrors(form, fields)` / `TG.clearFieldErrors(form)`: tìm ô theo
  `name`, thêm `.is-invalid`, chèn `.invalid-feedback`, nối `aria-describedby`.
- Đồng hồ: điền ngay khi tải trang, rồi cập nhật đúng đầu mỗi phút (đặt
  `setTimeout` tới giây 0 rồi mới `setInterval` 60 giây). Định dạng bằng
  `Intl.DateTimeFormat('vi-VN')`: "Thứ Tư, 22/10/2026" và "14:05".
- Flash message: `successMessage` thành `TG.toast`; `errorMessage` hiện thành
  `.alert.alert-danger` trong trang, không tự tắt.

**Giai đoạn 5: bảy trang cũ**

Trang cũ đã dùng Bootstrap, nên chỉ bỏ class riêng của template cũ:

- `pagetitle` + `breadcrumb` thành `.page-head` với `h1`.
- `section`, `card`, `table-card`, `table-card-header`, `table-toolbar`,
  `table-actions` thành `.card` thường của Bootstrap hoặc bỏ hẳn.
- `table-modern datatable` bỏ, giữ `table align-middle`. Mất `simple-datatables`
  thì bảng mất ô tìm và phân trang; AC2 chỉ đòi đọc được, mỗi trang có task làm
  lại toolbar riêng.
- `btn-light` thành `.btn-outline-secondary` hoặc `.icon-btn`.
- Alert thành công của trang ga thành toast.

**Giai đoạn 6: tài liệu**

- `docs/decisions/0012-giao-dien-admin-moi.md` theo `docs/templates/decision.md`:
  admin tự dựng theo mẫu trên Bootstrap dùng chung với client, `admin.css` là
  theme, font Manrope và JetBrains Mono tự phục vụ, JS qua `window.TG` bọc thành
  phần Bootstrap, danh sách vendor đã xóa. Thêm vào `docs/decisions/README.md`.
- ADR 0004: dòng trạng thái "phần template dashboard đã bị ADR 0012 thay".
- `AGENTS.md` mục "Hai khu vực giao diện": sửa câu "`admin.css` đến từ template
  dashboard bên ngoài..." vì không còn đúng. Luật không nạp chung `app.css` và
  `admin.css` giữ nguyên.
- `docs/product/admin.md` mục "Dashboard `/admin`": ghi `/admin` chuyển sang
  `/admin/trains` cho tới task Tổng quan.
- `docs/plans/active/khu-vuc-quan-tri.md`: cập nhật Status, phần shell đã bị
  ADR 0012 thay.

### #21 Khung client và xử lý lỗi

**Giai đoạn 1: quy ước exception**

1. `ResourceNotFoundException` (abstract) và `BusinessRuleException` (có
   `getField()`) trong `common/exception`.
2. **Message của exception thành câu tiếng Việt cho người dùng**, vì
   `ApiExceptionHandler` đưa thẳng nó vào `message`. Ví dụ
   `DuplicateStationCodeException` mang "Mã ga này đã được dùng cho một ga
   khác." và field `code`. `StationAdminController.rejectDuplicate` khi đó dùng
   `ex.getField()` và `ex.getMessage()` thay cho câu viết cứng; nhánh
   `DataIntegrityViolationException` (hai request cùng lúc) giữ nguyên.
3. `model/response/ApiError` là `record ApiError(String message, Map<String,
   String> fields)`.
4. `ApiExceptionHandler` với `@RestControllerAdvice(annotations =
   RestController.class)` **và `@Order(Ordered.HIGHEST_PRECEDENCE)`**.
   `@RestController` mang sẵn `@Controller`, nên `PageExceptionHandler` với
   `annotations = Controller.class` cũng áp lên REST controller. Không đặt thứ
   tự thì Spring có thể chọn handler trả trang HTML cho lỗi của API.
5. `PageExceptionHandler`: `ResourceNotFoundException` trả view `error/404`,
   status 404. Bỏ `@ResponseStatus` trên `StationNotFoundException`.

**Giai đoạn 2: trang lỗi**

- `templates/error/404.html`, `403.html`, `500.html` dùng `layout/base`. Mỗi
  trang một tiêu đề, một câu, nút "Về trang chủ"; 403 thêm "Đăng nhập bằng tài
  khoản khác" (form `POST /logout`). Không in `exception`, `trace` hay `path`.
- `SecurityConfig` theo Q2: khối `requestMatchers(...).authenticated()` cho
  sáu tiền tố riêng tư, rồi `anyRequest().permitAll()`. Dòng
  `/admin/**` riêng đang có gộp vào khối này. Static, `/vendor/**`, `/error`
  tự động mở nhờ `anyRequest().permitAll()`, nên bỏ các dòng `permitAll()`
  cũ, kể cả comment về `/vendor/**` (lý do của nó không còn).
- `docs/decisions/0013-quyen-truy-cap-mac-dinh-mo.md` theo
  `docs/templates/decision.md`, thêm vào `docs/decisions/README.md`. Sửa bảng
  quyền ở Bước 5 của `huong-dan-task/T02-dang-ky-dang-nhap-phan-quyen.md` và
  thêm dòng kiểm tra vào `huong-dan-task/README.md` như mục "Về Q2". Báo người
  làm #22 vì T02 là chủ của `SecurityConfig`.

**Giai đoạn 3: khung client**

1. `layout/base.html`: giữ `page(title, content)`, thêm meta CSRF, nạp
   `vendor/bootstrap-icons/bootstrap-icons.css`.
2. Font: đưa `@font-face` của Be Vietnam Pro vào `app.css`, tải thêm weight 800
   (`latin`, `vietnamese`) vì logo và bước hiện tại của mẫu dùng 800. Đặt
   `--bs-body-font-family` là Be Vietnam Pro cho cả trang. Mẫu còn dùng Noto
   Sans cho chữ thân bài; không thêm, theo issue.
3. Token: đổi giá trị trong `:root` của `app.css`, giữ tiền tố `--traingo-`
   theo ADR 0003: `--traingo-primary: #2c4d98`, `--traingo-ink: #0b1a36`, nền
   `#edf1f5`, cùng `field`, `line`, `muted` của mẫu. Cập nhật luôn các
   `--bs-*-rgb` đi kèm.
4. Class mới dùng tiền tố `tg-` như `app.css` đang làm. Không dùng `.nav`,
   `.steps`, `.wrap` của mẫu vì `.nav` trùng class của Bootstrap.
5. `fragments/navbar.html`: dựng trên `navbar` của Bootstrap. Logo về `/`, ba
   mục dạng viên thuốc (Tìm vé `/trips`, Vé của tôi `/my-trips`, Hướng dẫn
   `/#how-it-works`), "Đăng nhập" / "Đăng ký" bên phải. Mục đang mở dùng
   `aria-current="page"` theo `activeNav` (`search`, `my-trips`). Chừa chỗ cho
   tên người dùng và "Đăng xuất". Dưới 900px theo Q3.
6. `fragments/booking-steps.html`, fragment `steps(current)`: `<ol>` trong
   `<nav aria-label="Các bước đặt vé">`; bước hiện tại `aria-current="step"`;
   bước đã qua có `bi-check-lg` và là link; bước chưa tới không phải link. Ở
   640px trở xuống, chữ nằm dưới chấm như mẫu `hanh-khach-2-the-ve.html`.
7. `fragments/footer.html`: fragment `footer` đủ các cột như
   `trang-chu-8-swiss.html` (Q4), và `footer-compact` một dòng cho các trang đặt
   vé.
8. ADR 0003: thêm đoạn ghi font sản phẩm của khu khách là Be Vietnam Pro tự
   phục vụ.

## Risks And Recovery

- **Theme Bootstrap không ra đúng mẫu** ở vài chỗ (chiều cao nút, viền bảng).
  So ảnh chụp trang admin với `index.html` mẫu cùng độ rộng; lệch thì chỉnh biến
  `--bs-*` trên class, không viết đè rule.
- **Thứ tự hai `ControllerAdvice` sai** làm API trả HTML. Kiểm chứng bằng
  `@RestController` thử tạm, không commit.
- **Xóa `vendor/` còn chỗ dùng.** Grep `vendor/<tên>` trong `templates` và
  `static` trước khi xóa.
- **Font thiếu dấu tiếng Việt** nếu chỉ tải subset `latin`. Thử chuỗi "Đường
  sắt Hà Nội - Huế - Sài Gòn, Thứ Năm" ở mọi weight; `lineHeight / fontSize`
  của heading không dưới 1.25 theo ADR 0003.
- Hai branch đều hoàn tác được bằng cách revert commit merge; không có
  migration, không đổi dữ liệu.

## Progress

Cập nhật 24/10/2026. #20 đã commit và push (`feat/admin-shell`, `9dd9237`), chưa mở pull
request. #21 đã commit trên `feat/client-shell-errors` và merge `feat/admin-shell` vào, nên
gộp #20 vào `main` trước thì pull request #21 không còn conflict. Worktree tạm của #21 đã gỡ.

#20 (`feat/admin-shell`):

- [x] Font Manrope, JetBrains Mono + `OFL.txt`.
- [x] `admin.css` mới (theme Bootstrap + khung).
- [x] `base.html`, `sidebar.html`; xóa header, footer, `AdminShellModelAdvice`.
- [x] `/admin` chuyển hướng; xóa `dashboard.html`, `recentBookings`, vendor thừa. Bốn
      method `total*` của `AdminService` giữ lại vì #37 dùng lại.
- [x] `admin.js` với `window.TG`.
- [x] Bảy trang cũ bỏ class template; xóa ga qua `TG.confirm`.
- [x] ADR 0012, ADR 0004, `AGENTS.md`, `docs/product/admin.md`, plan
      `khu-vuc-quan-tri.md`. `docs/decisions/README.md` không có mục lục nên không sửa.
- [x] Kịch bản thử, ảnh 1440px và 390px, dòng `WORKLOG.md`.
- [x] Commit và push (`46f2637`, `c629257`, `9dd9237`).
- [ ] Pull request `Closes #20`, review.

#21 (`feat/client-shell-errors`):

- [x] Exception, `ApiError`, `ApiExceptionHandler`, `PageExceptionHandler`. Thêm handler
      chung: lỗi bất ngờ của API vẫn trả `{ message, fields }`, không lộ stack trace.
- [x] Trang 403, 404, 500, cộng `4xx.html`, `5xx.html`; `SecurityConfig` theo Q2;
      ADR 0013; sửa T02 và `huong-dan-task/README.md`.
- [x] Font, token, `base.html`, navbar, thanh bước, footer (`fullFooter`,
      `compactFooter`); ADR 0003.
- [x] Trang chủ dựng lại theo `trang-chu-8-swiss.html` (Phi đưa vào #21 ngày 24/10/2026
      vì không issue nào làm phần giao diện này); form tìm giữ nguyên ô cho #26.
- [x] Rà theo quy trình redesign; cặp font Noto Sans và Be Vietnam Pro, giảm độ đậm.
- [x] Kịch bản thử, ảnh trang chủ và 404 ở 1440px và 390px, dòng `WORKLOG.md`.
- [x] Commit (`19c5f77`, `bb7ae24`, `53bd952`) và merge `feat/admin-shell` (`6e8d265`).
- [ ] Thử 500 bằng cách dừng MySQL (cần quyền Administrator); đã thử 500 bằng
      controller ném exception tạm.
- [ ] Xác nhận nguồn và quyền dùng ảnh hero `hero-sa-pa.jpg` (Phi gửi, chưa có tác giả,
      giấy phép), rồi thêm dòng ghi nguồn ở footer hoặc đổi ảnh.
- [ ] Push, pull request `Closes #21`, review; báo người làm #22 về ADR 0013.

## Decisions

- 2026-10-22: Một plan cho hai issue vì chúng chia chung hợp đồng lỗi, meta
  CSRF và cách nạp font; mỗi issue vẫn là một branch, một pull request.
- 2026-10-22: Phi chốt Q1 (admin dùng Bootstrap chung với client), Q3, Q4, Q5
  như bảng "Quyết định".
- 2026-10-22: Phi chốt Q2 phương án B (mặc định mở, khóa theo tiền tố), để
  người chưa đăng nhập gõ sai đường dẫn thấy trang 404 đúng như AC4.
- 2026-10-23: Fragment footer đặt tên `fullFooter`, `compactFooter`, không dùng
  `footer`: selector trùng tên thẻ kéo cả hai thẻ `<footer>` của file vào trang.
- 2026-10-23: `TG.api` chỉ tin `message` khi có cả `fields`, vì JSON lỗi mặc định
  của Spring Boot cũng có `message` (tiếng Anh, có nội dung khi bật devtools).
- 2026-10-24: Trang chủ theo mẫu vào #21; #26 vẫn giữ phần chức năng của form tìm.
- 2026-10-24: Font khu khách là Noto Sans cho chữ đọc và Be Vietnam Pro cho tiêu đề
  (Phi chọn sau khi so bốn hướng trên trang thật); ghi ở ADR 0003.
- 2026-10-24: Ảnh hero là ảnh tàu leo núi Sa Pa Phi gửi; chiều cao hero và vị trí ảnh
  đặt theo từng khoảng độ rộng để tàu không chạm tiêu đề hay thanh tìm (dò 320-2560px).
- 2026-10-24: `server.servlet.session.tracking-modes=cookie`: trang lỗi là trang đầu
  của phiên từng mất CSS vì link bị gắn `;jsessionid` và bị firewall trả 400.

## Validation

Dự án không có test tự động (ADR 0007). Mỗi pull request cần:
`.\mvnw.cmd compile` không lỗi, ứng dụng khởi động bằng
`.\mvnw.cmd spring-boot:run` với MySQL và dữ liệu mẫu, rồi đi hết kịch bản dưới
đây trên trình duyệt.

### Kịch bản thử bằng tay #20

1. Mở `/admin`: chuyển sang `/admin/trains`, mục "Tàu" tô đậm.
2. Bấm lần lượt sáu mục menu: đúng trang, đúng mục tô đậm. Mở
   `/admin/stations/1/edit`: mục "Ga và tuyến" vẫn tô đậm.
3. Đồng hồ hiện đúng thứ, ngày, giờ máy; để trang mở qua đầu phút kế tiếp, giờ
   đổi đúng lúc.
4. Xóa một ga đang có tuyến: hộp xác nhận hiện; "Hủy" và `Esc` không gửi gì;
   "Xóa" gửi form và trang hiện lỗi "đang là điểm đi hoặc điểm đến". Thêm ga
   Quảng Ngãi (km 928) rồi xóa: toast thành công hiện.
5. Bàn phím: `Tab` từ đầu trang gặp "Bỏ qua menu", rồi đi hết menu theo thứ tự;
   trong hộp xác nhận `Tab` không thoát ra ngoài.
6. Ngăn kéo và `TG.api`: chưa trang nào dùng, nên thử trong DevTools Console:
   tạo tạm một `.offcanvas`, gọi `TG.sheet.open`, kiểm tra `Esc`, bấm nền, focus
   quay về nút; gọi `TG.toast` ba lần, chỉ còn 2 cái; gọi `TG.api` tới URL
   không tồn tại, lỗi ném ra có `message` tiếng Việt.
7. Ở 1440px, 1024px, 390px, 320px mở bảy trang:
   `document.documentElement.scrollWidth <= innerWidth` trên mọi trang. Dưới
   900px menu là thanh trên cùng cuộn ngang được, nút đăng xuất vẫn bấm được.
8. Tab Network: không request nào ra ngoài `localhost`; trang admin không tải
   `app.css`; Bootstrap và font `manrope`, `jetbrains-mono` trả 200. DevTools
   tab Computed của `body` cho `font-family` bắt đầu bằng Manrope, và mục
   "Rendered Fonts" hiện Manrope chứ không phải font hệ thống.
9. Đăng xuất từ menu: về trang đăng nhập.

### Kịch bản thử bằng tay #21

1. Trang chủ ở 1440px, 390px, 320px: navbar, footer đúng mẫu, không cuộn
   ngang; trên điện thoại mở được menu và tới "Vé của tôi". "Rendered Fonts"
   của tiêu đề và thân bài là Be Vietnam Pro.
2. Gõ `/trips/999999` và `/khong-co-trang-nay` khi đã đăng nhập: trang 404 tiếng
   Việt, có nút về trang chủ, không có stack trace.
3. Lặp lại bước 2 khi chưa đăng nhập (cửa sổ ẩn danh): cả hai đường dẫn đều
   ra trang 404, không bị đẩy về trang đăng nhập. Rồi gõ `/admin/stations`,
   `/my-trips`, `/booking/passengers`, `/tickets/1`: cả bốn đều về trang đăng
   nhập. Gọi `fetch('/api/admin/x')` và `fetch('/api/trips/1')`: phản hồi có
   `res.redirected === true` và `res.url` kết thúc bằng `/login`. Trước khi #22
   thêm mã 401 cho `/api/**`, `fetch` tự đi theo chuyển hướng nên `res.status`
   vẫn là 200; đừng dựa vào status ở bước này.
4. 403 trước khi có #22: ở trang ga, DevTools xóa ô ẩn `_csrf` trong form xóa
   rồi bấm xóa, trình duyệt hiện trang 403 tiếng Việt. Phần "khách gõ
   `/admin/stations`" của AC4 chỉ thử được sau khi #22 gộp; ghi rõ trong pull
   request.
5. 500: khi ứng dụng đang chạy, dừng MySQL (`Stop-Service MySQL97` với quyền
   Administrator), mở `/admin/trains`. Sau khoảng 30 giây chờ kết nối, trang 500
   tiếng Việt hiện. Bật lại MySQL.
6. API: thêm tạm một `@RestController` ném `DuplicateStationCodeException`,
   `StationNotFoundException` và nhận `@Valid @RequestBody` sai. Gọi bằng
   `fetch` trong DevTools, đọc JSON: 409 có `fields.code`, 404, 400 có lỗi từng
   ô. Xóa controller thử trước khi commit.
7. Thanh bước: gắn tạm `steps(3)` vào một trang, bước 1-2 có tick và là link,
   bước 4 mờ; ở 390px đọc đủ bốn nhãn.
8. Network: trang khách không tải `admin.css`, không request ra ngoài.

## Result

Điền sau khi hai pull request được gộp và kiểm chứng.

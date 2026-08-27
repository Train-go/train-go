# Execution Plan: Nền tảng giao diện và landing page

Date: 2026-08-24

## Status

Active

## Outcome

Trang `/` là landing thật theo `SPEC.md` mục 5 với đủ bốn khối: navbar, hero kèm
form tìm chuyến, popular routes, how it works. Bootstrap được phục vụ từ chính
ứng dụng nên trang giữ nguyên giao diện khi không có mạng. Layout fragment
Thymeleaf và bộ design token có sẵn để hơn 20 màn hình còn lại kế thừa mà không
phải chép lại phần `<head>`, navbar và footer.

Bằng chứng chấp nhận: `.\mvnw.cmd test` xanh; mở `http://localhost:8081` thấy đủ
bốn khối; tab Network không có request nào ra `cdn.jsdelivr.net`; ngắt mạng rồi
reload cứng, trang vẫn nguyên style.

## Context

- Yêu cầu gốc: `SPEC.md` mục 5 (Landing Page), 44 (Tech Stack), 45.1 (Responsive).
- Bản chắt lọc: `docs/product/overview.md`, mục "Định hướng giao diện".
- Quyết định nền: `docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`.
- Trạng thái code: `app/src/main/resources/templates/index.html` đang là landing
  tạm; `app/src/main/resources/static/css/app.css` mới có 3 khối CSS;
  `SecurityConfig.java` chỉ mở `/`, `/css/**`, `/js/**`, `/images/**`.

## Scope

In scope:

- Bootstrap 5.3.3 tải về `static/vendor/bootstrap/`.
- Mở `/vendor/**` và `/favicon.ico` trong `SecurityConfig`.
- Layout fragment `layout/base.html` cùng `fragments/navbar.html`, `footer.html`.
- Design token trong `app.css`, gồm quy tắc `line-height` cho heading tiếng Việt.
- Viết lại `index.html` thành landing đủ bốn khối.
- Dữ liệu ga và tuyến phổ biến khai tĩnh trong `HomeController`.
- Mở rộng `HomeControllerTest`.

Out of scope:

- Entity `Station` và mọi truy cập database. Dropdown ga dùng dữ liệu tĩnh.
- Route `/trips`. Form submit sẽ trả 404 cho tới Giai đoạn 3.
- Trang `/login`, `/register` và phần navbar phụ thuộc trạng thái đăng nhập.

## Approach

Làm theo thứ tự phụ thuộc, mỗi bước để lại một thứ kiểm chứng được.

1. Tải Bootstrap về `static/vendor/bootstrap/`.
2. Mở `/vendor/**` trong `SecurityConfig` và viết test khẳng định file CSS truy
   cập được khi chưa đăng nhập. Làm bước này ngay sau bước 1 vì đây là bẫy im
   lặng nhất của cả plan.
3. Dựng `layout/base.html` và hai fragment, cho `index.html` hiện tại dùng thử.
   Chạy app xác nhận layout đúng trước khi viết nội dung mới.
4. Mở rộng `app.css` thành bộ token.
5. Viết bốn khối của landing, thêm dữ liệu tĩnh vào `HomeController`.
6. Mở rộng test, chạy `.\mvnw.cmd test`.
7. Kiểm tra bằng mắt: ba breakpoint, chế độ ngắt mạng, heading có dấu.

## Risks And Recovery

- **Quên mở `/vendor/**` trong `SecurityConfig`.** Spring Security trả HTML của
  trang login thay cho file CSS; trình duyệt bỏ qua im lặng và trang hiện ra
  không có style, log không báo lỗi. Phòng bằng test ở bước 2. Khi nghi ngờ, mở
  DevTools xem response của file CSS là CSS hay HTML.
- **Cú pháp fragment có tham số của Thymeleaf viết sai.** Lỗi chỉ lộ lúc render,
  không lộ lúc biên dịch. Phòng bằng cách thử layout với riêng `index.html` trước
  khi nhân ra các trang khác.
- **Heading tiếng Việt bị cắt dấu.** Bootstrap đặt `$headings-line-height: 1.2`;
  ở tỉ lệ đó dấu mũ và dấu nặng của `Ế`, `Ộ`, `ữ` chạm dòng trên hoặc mất ngọn.
  Phòng bằng override tối thiểu 1.25 trong `app.css` và soi mắt ở bước 7.
- **Tên ga dài làm vỡ card popular route.** Kiểm tra bằng một tên ga dài bất
  thường ở bước 7.

Recovery: toàn bộ thay đổi nằm trong `app/src/main/resources` và một file Java
config, không đụng database và không có migration. Hỏng thì `git checkout` lại
là xong.

## Progress

- [x] Tải Bootstrap 5.3.3 về `static/vendor/bootstrap/`: `bootstrap.min.css`
      232.803 bytes, `bootstrap.bundle.min.js` 80.721 bytes.
- [x] Mở `/vendor/**` và `/favicon.ico` trong `SecurityConfig` kèm test
      `bootstrapStylesheet_isReachable_withoutLogin`.
- [x] `layout/base.html` cùng `fragments/navbar.html` và `fragments/footer.html`.
- [x] Design token trong `app.css`, gồm override `line-height` heading 1.28.
- [x] Bốn khối landing, dữ liệu ga và tuyến tĩnh trong `HomeController`,
      tương tác trong `static/js/landing.js`.
- [x] `HomeControllerTest` mở rộng lên 5 test. `.\mvnw.cmd test`:
      `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`
      (24/08/2026).
- [x] Kiểm tra bằng mắt qua Playwright ở 375px, 768px, 1280px. Kết quả bên dưới.

## Decisions

- 2026-08-24: Tên hiển thị là `TrainGo`; Bootstrap tải về local; layout fragment
  thuần không dùng dialect; `line-height` heading tối thiểu 1.25. Chi tiết và lý
  do: `docs/decisions/0003-he-thong-giao-dien.md`.

## Validation

- Focused proof: `@WebMvcTest` cho `HomeController` — model attribute, bốn khối
  trong nội dung render, và `/vendor/**` truy cập được khi chưa đăng nhập.
- End-to-end proof: mở `http://localhost:8081` trên trình duyệt thật, kiểm tra ở
  375px, 768px, 1280px, và một lần với mạng đã ngắt.
- Repository-required checks: `.\mvnw.cmd test` phải xanh.

## Result

Hoàn thành ngày 24/08/2026, đã kiểm chứng bằng ứng dụng thật chạy trên MySQL
(container `traingo-mysql` healthy, app ở `http://localhost:8081`).

Bằng chứng đã quan sát được:

| Kiểm tra | Kết quả |
| --- | --- |
| `.\mvnw.cmd test` | `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` |
| `GET /` | 200 |
| `GET /vendor/bootstrap/bootstrap.min.css` | 200, `text/css`, 232.803 bytes |
| `GET /vendor/bootstrap/bootstrap.bundle.min.js` | 200, 80.721 bytes |
| Stylesheet trang `/` nạp | chỉ `localhost:8081/vendor/...` và `localhost:8081/css/app.css` |
| Chuỗi `cdn.jsdelivr.net` trong HTML | 0 lần |
| Token áp vào Bootstrap | `.btn-primary` có `background-color: rgb(15, 108, 189)` |
| Tỉ lệ `line-height / font-size` của 8 heading | tất cả 1.28; không heading nào dưới 1.15 |
| Tràn ngang ở 375px, 768px, 1280px | không |
| Tên ga dài `Ga Sài Gòn — Bến xe Miền Đông mới` ở 375px | không tràn |
| Click card tuyến phổ biến | điền đúng `Ga Hà Nội` → `Ga Đà Nẵng` |
| Nút đổi chiều | hoán đổi đúng hai ga |
| Ngày mặc định và `min` | đều là ngày hiện tại |
| Chặn ga đi trùng ga đến | hiện lỗi, không rời trang |
| Lỗi JavaScript trên console | không có |

Giới hạn còn lại:

- Form submit sang `/trips` trả 404 vì route chưa tồn tại. Đúng như thiết kế ở
  giai đoạn này, sẽ hết khi Giai đoạn 3 xong.
- Navbar hiện luôn `Đăng nhập` / `Đăng ký` vì chưa có auth. Cần
  `thymeleaf-extras-springsecurity6` và `sec:authorize` ở Giai đoạn 1.
- Danh sách ga và tuyến phổ biến là dữ liệu tĩnh trong `HomeController`, phải
  thay bằng `StationRepository` ở Giai đoạn 2.
- Ô `input type="date"` hiển thị theo locale của trình duyệt, nên trên máy đặt
  tiếng Anh sẽ ra `mm/dd/yyyy`. Không sửa được bằng HTML thuần; muốn ép định
  dạng `dd/mm/yyyy` phải thay bằng date picker tự viết, chưa đáng làm bây giờ.

Chưa chuyển sang `docs/plans/completed/` vì bốn giới hạn trên đều được gỡ trong
các giai đoạn kế tiếp của `mvp-traingo.md`; giữ ở `active/` để không mất dấu.

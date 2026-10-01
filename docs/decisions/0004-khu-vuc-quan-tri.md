# 0004 Khu vực quản trị

Date: 2026-08-27

## Status

Accepted

## Context

`docs/product/admin.md` mô tả bảy màn hình quản trị nhưng repo chưa có một dòng
code nào cho khu vực này. Nhóm có sẵn một template dashboard Bootstrap của tác
giả khác trong một dự án Spring Boot riêng (`D:\IUH\Eclipse\demo`) và muốn dùng
lại phần khung thay vì tự dựng sidebar, header, bảng và biểu đồ từ đầu.

Template đó không lắp thẳng vào `app/` được vì ba lý do:

1. **Trùng tên file.** Template có `templates/index.html` và
   `templates/fragments/footer.html`; `app/` cũng có đúng hai file đó cho
   landing page.
2. **Đụng CSS.** `style.css` của template mở đầu bằng
   `* { margin: 0; padding: 0; border: 0; list-style: none }` và một bộ
   `:root { --color-* }` riêng. Nạp chung với `app.css` là landing page vỡ.
3. **Lệch ADR 0003.** Template nạp Bootstrap, icon, biểu đồ và ba trình soạn
   thảo từ CDN, trong khi `0003-he-thong-giao-dien.md` đã chốt phục vụ mọi thư
   viện từ chính ứng dụng để demo chạy được khi mất mạng.

Thêm một điểm không thuộc kỹ thuật: template mang tên tác giả, dòng license và
link mạng xã hội cá nhân ở footer và header.

## Decision

**Khu vực quản trị được namespace ở cả ba tầng**, không sửa file nào của
landing:

| Tầng | Landing / khách | Admin |
| --- | --- | --- |
| Template | `templates/layout/base.html`, `templates/fragments/` | `templates/admin/layout/base.html`, `templates/admin/fragments/`, `templates/admin/*.html` |
| CSS | `/css/app.css` | `/css/admin/admin.css` |
| JS | `/js/landing.js` | `/js/admin/admin.js` |
| Java | `com.voyagego.traingo.home` | `com.voyagego.traingo.admin`, và từ 01/10/2026 `<feature>/controller/` cho màn hình đã nối database (ADR 0001) |
| URL | `/`, `/login` | `/admin`, `/admin/…` |

**Static của admin nằm dưới `static/css/admin/` và `static/js/admin/`, không
phải `static/admin/`.** Đặt ở `static/admin/css/…` thì URL thành
`/admin/css/…`, lọt vào namespace controller `/admin/**` và vào luật phân
quyền cùng tên. `/css/**` và `/js/**` thì `SecurityConfig` đã mở sẵn.

**Không bao giờ nạp `app.css` và `admin.css` trên cùng một trang.** Đây là luật
bắt buộc, không phải khuyến nghị: `admin.css` mang reset và bảng màu riêng.

**Admin dùng chung idiom layout với landing** — fragment Thymeleaf có tham số
`~{admin/layout/base :: page(~{::title}, ~{::main})}`, đúng như ADR 0003 đã
chốt. Template gốc dùng `model.addAttribute("content", "index")` rồi
`~{${content} :: content}`; cách đó bị bỏ để repo chỉ có một cách dựng trang.

Trạng thái active của sidebar đọc từ model attribute `activeNav` do controller
đặt, không suy từ đường dẫn, nên route lồng như `/admin/trains/7` vẫn sáng đúng
mục "Quản lý tàu".

**Tám thư viện được tải về `static/vendor/`, không dùng CDN**, đúng tinh thần
ADR 0003. Bootstrap 5.3.3 dùng lại bản đã có, không tải thêm bản 5.3.2 mà
template tham chiếu.

| File | Kích thước |
| --- | --- |
| `bootstrap-icons/bootstrap-icons.css` | 98.255 |
| `bootstrap-icons/fonts/bootstrap-icons.woff` | 176.032 |
| `bootstrap-icons/fonts/bootstrap-icons.woff2` | 130.396 |
| `boxicons/css/boxicons.min.css` | 68.028 |
| `boxicons/fonts/boxicons.eot` | 405.670 |
| `boxicons/fonts/boxicons.svg` | 1.242.122 |
| `boxicons/fonts/boxicons.ttf` | 320.944 |
| `boxicons/fonts/boxicons.woff` | 321.020 |
| `boxicons/fonts/boxicons.woff2` | 115.680 |
| `apexcharts/apexcharts.min.js` | 533.680 |
| `simple-datatables/style.css` | 4.873 |
| `simple-datatables/simple-datatables.js` | 98.086 |
| `quill/quill.snow.css` | 24.606 |
| `quill/quill.js` | 205.935 |
| `ckeditor/ckeditor.js` | 1.342.867 |
| `editorjs/editorjs.js` | 207.948 |
| `editorjs/header.js` | 6.710 |
| `editorjs/list.js` | 5.737 |

Tổng khoảng 5,3MB. Bảy màn hình hiện tại chỉ dùng `bootstrap-icons`,
`apexcharts` và `simple-datatables`; năm thư viện còn lại được vendor sẵn cho
màn hình sau và **không** được layout nạp mặc định — trang nào cần thì tự khai.

`boxicons.min.css` phải nằm ở `boxicons/css/` vì nó tham chiếu font bằng
`url(../fonts/…)`. Đặt sai một cấp là mất icon mà không có lỗi nào trong log.

**Bỏ theme customizer của template.** Admin dùng một theme sáng cố định:
`<html data-theme="light" data-bs-theme="light">`. Không port `theme.css`,
`theme-custom.css`, `toastify-custom.css`, `theme-custom.js` và fragment
`theme-controls.html`; cũng bỏ khối "Menu Position" / "Interface theme" trong
dropdown profile. Bảng màu sáng và tối vẫn nằm sẵn trong `admin.css` qua các
selector `[data-theme=…]`, nên bật lại về sau không phải viết lại CSS.

Bẫy kèm theo: `main.js` gốc gọi `setTheme(getPreferredTheme())` **trước** khi
kiểm tra `#theme-toggle-icon` có tồn tại không. Bỏ nút mà giữ đoạn theme là
script ném `TypeError` trên null và chết luôn cả sidebar. `admin.js` vì vậy cắt
trọn khối đó, chỉ giữ toggle sidebar, đồng bộ theo breakpoint và back-to-top.

**Gỡ sạch branding của template gốc trước khi file vào repo.** Đã gỡ:

- `fragments/footer.html`: viết mới hoàn toàn. Bản gốc chứa dòng copyright tên
  tác giả, link `opensource.org/licenses/MIT` kèm chữ "MIT License", một câu
  trích, link GitHub của repo template và link LinkedIn cá nhân.
- `fragments/header.html`: tên trong logo, tên hiển thị và ba ảnh avatar mang
  tên tác giả trong `alt`.
- `admin.css`: các rule `.footer-meta`, `.footer-links`, `.footer-divider`,
  `.footer-accent`, `.footer-line`, `.footer-license`, `.footer-quote` — chỉ
  phục vụ footer cũ, giữ lại là để CSS chết và để lại dấu vết.

Một `@WebMvcTest` khẳng định HTML của `/admin` không chứa `Nazareth`,
`MIT License`, `opensource.org`, `linkedin.com` hay `github.com`, để branding
không lẻn về ở lần sửa sau.

Ghi nhận cho rõ: template gốc phát hành theo MIT, và MIT yêu cầu giữ dòng
copyright. Đây là đồ án môn học nộp nội bộ nên nhóm chấp nhận đánh đổi này. Nếu
repo được public, chỗ để đặt lại ghi nhận là một file `NOTICE` ở gốc, không phải
giao diện.

**`admin.css` đổi ba chỗ so với `style.css` gốc**, ngoài phần gỡ footer:

- Xóa `@import url('https://fonts.googleapis.com/…Poppins…')` — reference CDN
  duy nhất trong file.
- `font-family` của `body` và `.card-title` đổi sang đúng stack của `app.css`
  (`system-ui, "Segoe UI", Roboto, Arial, sans-serif`), nên không phải vendor
  thêm file font.
- `--color-primary` kéo về `#0f6cbd` cho khớp `--traingo-primary`. Riêng màu
  của mục sidebar đang mở phải sửa tại chỗ khai báo: nó nằm trong khối lồng
  `[data-bs-theme="light"] { … }` và hardcode `#4154f1`, nên một override phẳng
  ở cuối file thua về độ ưu tiên.

**`SecurityConfig` thêm `.requestMatchers("/admin/**").authenticated()`.** Hôm
nay luật này trùng với `anyRequest().authenticated()` nên không đổi hành vi;
nó tồn tại để tính năng auth có đúng một dòng phải sửa thành
`hasRole("ADMIN")`, và để test khóa hành vi lại từ bây giờ.

## Alternatives Considered

1. **Copy thẳng template vào `app/`, đổi tên file bị trùng.** Nhanh nhất. Bị
   loại vì `style.css` vẫn nạp chung không gian với `app.css` khi có người vô
   tình thêm link, và reset `*` của nó sẽ phá landing — một lỗi chỉ lộ ra bằng
   mắt, không có test nào bắt được nếu không namespace.
2. **Giữ CDN cho riêng admin, landing vẫn local.** Tiết kiệm 5,3MB trong repo và
   port nhanh hơn hẳn. Bị loại vì mâu thuẫn trực tiếp với ADR 0003 và vì cùng
   một buổi demo: mất wifi thì landing còn nguyên còn admin vỡ trắng, khó giải
   thích hơn là cả hai cùng chạy.
3. **Chỉ vendor ba thư viện admin thực sự dùng.** Repo nhẹ hơn khoảng 3,1MB.
   Không chọn vì các màn hình sau (form nhập toa, editor mô tả chuyến) nhiều khả
   năng cần tới, và tải rời rạc từng đợt thì mỗi lần lại phải nhớ luật "không
   CDN" một lần nữa.
4. **Port toàn bộ ~25 trang mẫu của template làm thư viện component.** Có sẵn
   chỗ tra cứu nút, form, bảng, badge. Bị loại vì 18 trong số đó không tương ứng
   màn hình nào của `SPEC.md`, và mỗi trang thừa là một trang phải gỡ branding,
   dịch và bảo trì.
5. **Giữ theme customizer.** Người dùng đổi được màu và vị trí menu. Bị loại vì
   65KB JavaScript cho một tính năng không có trong `SPEC.md`, và vì nó dựng một
   hệ token thứ hai (`app.theme`, `app.palette` trong localStorage) song song với
   `--traingo-`.

## Consequences

Positive:

- Admin có đủ sidebar, header, bảng sort/search/paginate và biểu đồ mà không
  phải tự viết; bảy màn hình còn lại chỉ là markup.
- Landing và trang khách không đổi một pixel nào, và điều đó kiểm chứng được:
  `/` không nạp `admin.css` hay `admin.js`.
- Cả `/` lẫn `/admin` chạy được khi ngắt mạng.
- Đổi màu chủ đạo của admin là sửa một dòng `--color-primary`.
- Thêm màn hình admin mới chỉ cần khai `<title>` và `<main>`.

Tradeoffs:

- Repo nặng thêm khoảng 5,3MB, trong đó 1,3MB CKEditor và 2,5MB boxicons chưa
  trang nào dùng. Nâng version về sau là tải tay, không công cụ nào nhắc.
- Có hai hệ CSS trong một dự án. Người mới rất dễ nạp nhầm `app.css` vào trang
  admin hoặc ngược lại, và triệu chứng là giao diện vỡ chứ không phải lỗi biên
  dịch.
- `admin.css` gần 4.900 dòng, phần lớn là style của những component chưa dùng.
- Class của admin không mang tiền tố `tg-` như ADR 0003 quy định cho code của
  nhóm, vì chúng đến từ template. Tiền tố `tg-` vẫn áp dụng cho class do nhóm
  tự viết ở cả hai khu vực.

## Follow-Up

- Khi tính năng auth xong: đổi `.requestMatchers("/admin/**").authenticated()`
  thành `hasRole("ADMIN")`, và thay tên tĩnh trong
  `admin/controller/AdminShellModelAdvice` bằng tên người đang đăng nhập.
  (Ngày 01/10/2026 tên này chuyển từ `AdminController` sang advice đó, để
  controller admin nằm ở package khác vẫn có tên trên header.)
- Khi có entity thật: thay từng danh sách placeholder bằng repository, và nối
  các nút Thêm/Sửa/Xóa hiện đang `disabled`. Theo ADR 0001, màn hình đó chuyển
  ra package của feature. `/admin/stations` đã làm xong ngày 01/10/2026; còn
  tuyến, tàu, chuyến, booking và người dùng.
- Thêm `/admin/trains/{id}` để quản lý toa, theo `docs/product/admin.md`.
- Nếu tới cuối dự án CKEditor, Quill, EditorJS và boxicons vẫn không trang nào
  dùng, xóa khỏi `static/vendor/`.

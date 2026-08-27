# 0003 Hệ thống giao diện

Date: 2026-08-24

## Status

Accepted

## Context

`SPEC.md` mục 44 chốt Bootstrap trong tech stack nhưng không nói nạp bằng cách
nào, cũng không nói các trang chia sẻ phần khung ra sao. Landing tạm hiện tại nạp
Bootstrap 5.3.3 từ CDN jsdelivr và tự viết lại toàn bộ `<head>` trong
`index.html`.

Dự án còn hơn 20 màn hình phải xây, ba người làm song song trên các package khác
nhau. Nếu mỗi trang tự khai `<head>`, navbar và footer thì mọi thay đổi khung sẽ
phải sửa ở 20 chỗ, và ba người sẽ tự đặt ra ba cách viết khác nhau.

Thêm hai điểm chưa chốt chặn việc bắt đầu: `README.md` gọi sản phẩm là
"Voyage GO" trong khi `SPEC.md` và code gọi là "TrainGo"; và toàn bộ giao diện là
tiếng Việt nhưng chưa có quy tắc nào về việc chữ có dấu hiển thị thế nào.

## Decision

**Tên hiển thị là `TrainGo`.** Khớp `SPEC.md` — bản gốc của môn học — cùng
`artifactId` `traingo` và class `TrainGoApplication`. `voyage-go` chỉ còn là tên
repository trên GitHub, không xuất hiện trên giao diện.

**Bootstrap 5.3.3 phục vụ từ chính ứng dụng**, không dùng CDN. Hai file nằm trong
`app/src/main/resources/static/vendor/bootstrap/`, tải ngày 24/08/2026 từ
`https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/`:

| File | Kích thước |
| --- | --- |
| `bootstrap.min.css` | 232.803 bytes |
| `bootstrap.bundle.min.js` | 80.721 bytes |

Bản `bundle` đã gồm Popper, cần cho dropdown và collapse của navbar.

**`SecurityConfig` phải mở `/vendor/**`.** Chuỗi filter hiện tại kết thúc bằng
`anyRequest().authenticated()`, nên một đường dẫn tĩnh không được liệt kê sẽ bị
chặn.

**Layout dùng Thymeleaf fragment có tham số**, không thêm
`thymeleaf-layout-dialect`. `templates/layout/base.html` giữ `<head>`, navbar,
footer và thẻ script; mỗi trang khai `th:replace="~{layout/base :: page(~{::title}, ~{::main})}"`.

**Màu và khoảng cách khai bằng CSS custom property** trong `:root` của
`static/css/app.css`, tiền tố `--traingo-`. Trang không được hardcode mã màu.
Layout dùng utility class của Bootstrap; `app.css` chỉ bổ sung token và các
component riêng của TrainGo.

**Class riêng của dự án mang tiền tố `tg-`**, ví dụ `tg-hero`,
`tg-search-card`, `tg-route-card`. Tiền tố này giúp phân biệt ngay class nào là
của Bootstrap và class nào là của nhóm, nên xóa hay đổi một class riêng không
phải dò xem có đụng framework không. Biến giữ tiền tố dài `--traingo-` vì chúng
xuất hiện thưa hơn nhiều và tên dài đọc rõ hơn.

**Id trong template dùng camelCase với tiền tố `tg`**, ví dụ `tgSearchForm`,
`tgSwap`, để JavaScript trong `static/js/` bắt được mà không phụ thuộc cấu trúc
DOM.

**`line-height` của heading tối thiểu 1.25.** Bootstrap đặt
`$headings-line-height: 1.2`. Ở tỉ lệ đó, dấu mũ và dấu nặng của những chữ như
`Ế`, `Ộ`, `ữ` bị cắt ngọn hoặc chạm dòng phía trên. Lỗi này không xuất hiện với
tiếng Anh nên upstream không có lý do sửa; dự án tiếng Việt phải tự override.

## Alternatives Considered

1. **Giữ CDN jsdelivr.** Không thêm file nào vào repo, đổi version chỉ sửa một
   dòng, và người dùng thật có thể đã cache sẵn Bootstrap từ trang khác. Bị loại
   vì bài này được chấm bằng một buổi demo: mất wifi hoặc CDN bị chặn là toàn bộ
   giao diện vỡ trắng ngay trước mặt giảng viên. Đổi 313KB trong repo lấy sự chắc
   chắn đó là đáng.
2. **WebJars qua Maven.** Cách sạch nhất về quản lý version: thêm dependency vào
   `pom.xml`, Spring Boot tự phục vụ ở `/webjars/**`, không có file thư viện nào
   nằm trong repo. Bị loại vì thêm một lớp khái niệm mới cho nhóm đang học Spring
   Boot, và khi đường dẫn sai thì lỗi khó lần hơn hẳn so với một file tĩnh nhìn
   thấy được trong thư mục.
3. **`thymeleaf-layout-dialect`.** Cú pháp `layout:decorate` dễ đọc hơn
   `~{::title}` và giống JSP tiles mà nhiều tutorial dùng. Bị loại vì thêm một
   dependency ngoài chỉ để tiết kiệm vài ký tự, trong khi fragment có tham số là
   tính năng lõi của Thymeleaf 3 và không cần cài gì.
4. **Tự viết toàn bộ CSS, không dùng framework.** Kiểm soát hoàn toàn và không
   thừa một dòng nào. Bị loại vì lệch `SPEC.md` mục 44, và vì sơ đồ ghế cùng các
   bảng quản trị sẽ ngốn thời gian gấp nhiều lần phần trang trí.
5. **Đổi tên sản phẩm thành "Voyage GO".** Khớp tên repository và README. Bị loại
   vì phải sửa `SPEC.md` — bản gốc của môn học — cùng năm file trong
   `docs/product/` và định danh Maven đang dùng `traingo`.

## Consequences

Positive:

- Demo chạy được cả khi không có mạng.
- Thêm một trang mới chỉ cần khai `<title>` và `<main>`; phần khung tự có.
- Đổi màu chủ đạo của cả sản phẩm là sửa một dòng trong `:root`.
- Ba người viết trang song song mà khung vẫn giống nhau.
- Chữ tiếng Việt có dấu hiển thị đủ, không phải phát hiện bằng cách nhìn ảnh chụp
  màn hình lúc đã muộn.

Tradeoffs:

- Repo nặng thêm khoảng 313KB và có hai file thư viện được commit. Nâng version
  Bootstrap về sau là tải lại file thủ công, không có công cụ nào nhắc.
- Cú pháp `~{::title}` lạ mắt với người mới, và viết sai thì lỗi chỉ lộ lúc
  render chứ không lộ lúc biên dịch.
- Quên `/vendor/**` trong `SecurityConfig` sẽ làm trang mất sạch style mà log
  không báo gì. Đã khóa bằng một test riêng trong `HomeControllerTest`.
- Override `line-height` khiến heading của dự án cao hơn Bootstrap mặc định một
  chút, nên ảnh chụp so với tài liệu Bootstrap sẽ không khớp từng pixel.

## Follow-Up

- Khi có auth, navbar phải đổi theo trạng thái đăng nhập bằng `sec:authorize`;
  cần thêm dependency `thymeleaf-extras-springsecurity6`.
- Khi có `Station` thật, thay dữ liệu ga tĩnh trong `HomeController` bằng
  `StationRepository`.
- Cân nhắc thêm một trang mẫu tập hợp mọi component (nút, form, bảng, badge
  trạng thái) để ba người đối chiếu, nếu bắt đầu thấy giao diện các trang lệch
  nhau.

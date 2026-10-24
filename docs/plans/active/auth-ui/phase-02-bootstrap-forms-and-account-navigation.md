---
phase: 2
title: Bootstrap forms and account navigation
status: completed
effort: 1–2h
priority: P1
dependencies:
  - 1
---

# Phase 2: Bootstrap forms and account navigation

## Overview

Tạo hai form Bootstrap gọn trong shell client; thay placeholder danh tính ở navbar/sidebar.
Trạng thái: completed (24/10/2026).

## Context Links

- [Tổng quan](./plan.md), [phase 1](./phase-01-controller-and-security-integration.md).
- `app/src/main/resources/templates/layout/base.html`, `fragments/navbar.html`, `admin/fragments/sidebar.html`.

## Key Insights

Shell đã nạp Bootstrap, icons và `app.css`, có skip link và compact footer. Admin dùng shell/CSS riêng.
`currentUser.fullName` và `currentUser.role` đã có; sidebar đang để tên trống, navbar đang là placeholder.

## Requirements

- Chỉ class/component Bootstrap cho phần mới; không CSS inline, `<style>`, file CSS hay include CSS mới.
- Giữ font/màu đã có, không nạp CDN hoặc trộn `app.css` với `admin.css`.
- Căn giữa ngang; form đăng ký cao được cuộn tự nhiên, không ép chiều cao viewport gây cắt nội dung.
- Tiếng Việt; label/input gắn đúng, autocomplete đúng, lỗi inline rõ và thao tác hoàn toàn bằng bàn phím.
- Login/register có link qua lại, nút submit chính; không thêm forgot password/OAuth/remember-me ngoài phạm vi.

## Architecture

Hai trang dùng `layout/base :: page(title=~{::title}, content=~{::main}, compactFooter=true)`.
Markup Bootstrap xử lý bố cục/lỗi. `auth.js` chỉ bật/tắt visibility password, nạp bên trong main của trang auth
theo khả năng shell hiện có, không refactor layout chỉ để thêm slot script.

## Related Code Files

- Tạo: `app/src/main/resources/templates/auth/login.html`, `auth/register.html`;
  `app/src/main/resources/static/js/auth.js` (nhỏ, chỉ visibility).
- Sửa: `app/src/main/resources/templates/fragments/navbar.html`, `admin/fragments/sidebar.html`.
- Đọc: `layout/base.html`, `admin/layout/base.html`, `static/css/app.css` (chỉ đối chiếu ảnh hưởng hiện có).
- Không tạo/sửa CSS, vendor, dependency hoặc redesign shell.

## Implementation Steps

1. Dùng `container py-4 py-md-5`, `row justify-content-center`,
   `col-12 col-sm-10 col-md-7 col-lg-5`; chọn card border hoặc form gọn nhất quán.
   Không `vh-100` hoặc style width/height mới. Title/h1 rõ “Đăng nhập” / “Đăng ký”.
2. Login: email + password, autocomplete `username`/`current-password`, lỗi chung tiếng Việt,
   email giữ an toàn qua model. Hiện flash account-created sau auto-login lỗi.
3. Register: fullName/email/phoneNumber/password/confirmPassword; autocomplete
   `name`, `email`, `tel`, `new-password`; phone dùng input tel/text giữ số 0 đầu, không number.
   Ghi rõ phone 10 chữ số bắt đầu 0 và password 8 ký tự/72 byte UTF-8; không dùng maxlength ký tự
   thay cho giới hạn byte, backend là nguồn kiểm tra cuối cùng.
4. Dùng `th:action` + POST để Spring/Thymeleaf cấp CSRF; kiểm DOM thật có token.
   Lỗi field dùng `is-invalid`, `invalid-feedback`, `aria-invalid`, `aria-describedby`;
   summary chung có `role="alert"`. Không hiển thị tên thuộc tính synthetic cho người dùng.
5. Password luôn input trống trên response: khai name/id/type, không `th:value` hay binding hồi phục
   rejectedValue. Nút visibility `type="button"`, label truy cập được và `aria-pressed` cập nhật;
   giữ focus, thao tác Enter/Space; JS tắt vẫn gửi form bình thường.
6. Navbar khách chưa login giữ link login/register. Khi login, hiện fullName escaped bằng `th:text`,
   link phù hợp role và POST logout; ADMIN có lối vào `/admin/trains`.
   Đặt tên dài/controls vào phần collapse trên màn hình hẹp để không ép logo/toggler tràn 320px;
   không rút gọn bằng CSS mới. Link đăng ký mobile chỉ hiện cho khách chưa login.
7. Sidebar admin thay tên placeholder bằng `currentUser.fullName`, giữ POST logout có CSRF.
   Không render email/password hash hay principal toàn bộ; không dùng `th:utext` cho dữ liệu tài khoản.
8. Loại bỏ comment placeholder đã lỗi thời; compile và kiểm tra template qua server, không chỉ mở HTML tĩnh.

## Todo List

- [x] Login/register dùng shell, compact footer và Bootstrap thuần.
- [x] Form/errors/password visibility có nhãn; thao tác bàn phím chưa thử kỹ (xem phase 3).
- [x] Navbar ba trạng thái guest/CUSTOMER/ADMIN; sidebar tên thật + logout.
- [x] Không CSS mới; không tràn ngang 1440/390/320px, kể cả fullName dài.

## Success Criteria

- [x] Có form đầy đủ với lỗi tiếng Việt và chuyển trang qua lại.
- [x] Non-password fields giữ sau lỗi; hai password không value trong DOM response.
- [x] Họ tên hiển thị an toàn ở shell khách/admin, logout là POST có CSRF.
- [x] Chỉ auth.js nhỏ; layout không đổi nếu không có nhu cầu bắt buộc được chứng minh.

## Risk Assessment

- Lớp shell hiện có có thể ảnh hưởng card/input: kiểm bằng mắt, ưu tiên Bootstrap utilities thay CSS override.
- Tên dài + nút ở thanh 320px: chuyển phần tài khoản vào collapse, thử dữ liệu dài 100 ký tự.
- Native validation có thể che lỗi server khi thử: kiểm cả hành vi thường và submission bỏ qua client validation.

## Security Considerations

HTML escape email/fullName; không password value, không HTML lấy từ người dùng.
Ẩn menu không thay thế role guards. Token CSRF phải còn đúng sau login/register (session rotation).

## Next Steps

Đi hết ma trận phase 3 bằng server thật; nếu lỗi, sửa rồi chỉ chạy lại phần chịu ảnh hưởng và hồi quy auth chính.

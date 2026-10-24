# 0013 Quyền truy cập: mặc định mở, khóa theo tiền tố

Date: 2026-10-23

## Status

Accepted. Thay dòng "Mọi đường dẫn còn lại: Đã đăng nhập" trong bảng quyền của
`docs/plans/active/huong-dan-task/T02-dang-ky-dang-nhap-phan-quyen.md`.

Cập nhật 24/10/2026, khi gộp #21 với #22 (đã ở `main`, dùng `hasRole(...)` và mặc
định khóa): giữ mặc định mở; `/api/trips/**` chuyển sang công khai như #22 đã làm.

## Context

Issue #21 yêu cầu: người chưa đăng nhập gõ một đường dẫn không tồn tại phải thấy
trang 404 tiếng Việt, không bị đẩy về trang đăng nhập.

Spring Security đứng trước mọi controller. Với luật cũ
`anyRequest().authenticated()`, nó chặn người chưa đăng nhập ngay khi request tới,
lúc đó chưa ai biết đường dẫn có tồn tại hay không. Nên chỉ mở thêm `/error` là
chưa đủ: `/abc` vẫn chuyển sang trang đăng nhập.

Có hai cách. Mặc định khóa và chỉ mở các trang công khai: an toàn hơn, nhưng
đường dẫn gõ sai vẫn về trang đăng nhập. Mặc định mở và chỉ khóa các tiền tố
riêng tư: đúng yêu cầu của #21, nhưng trang riêng tư nào quên khai thì mở cho
người lạ. Phi chọn cách thứ hai ngày 22/10/2026 (Q2 của
`docs/plans/active/khung-giao-dien-admin-va-client.md`).

## Decision

**Mặc định mở, khóa theo tiền tố.** `SecurityConfig` khóa đúng năm tiền tố, mọi
đường dẫn khác `permitAll()`:

| Tiền tố | Ai được vào |
| --- | --- |
| `/admin/**`, `/api/admin/**` | Admin, `hasRole("ADMIN")` |
| `/booking/**`, `/my-trips/**`, `/tickets/**` | Khách, `hasRole("CUSTOMER")` |
| Mọi đường dẫn còn lại, gồm `/api/trips/**` | Ai cũng vào được |

`/api/trips/**` công khai vì sơ đồ chỗ chỉ để xem và giống nhau với mọi người; thao
tác đặt vé nằm dưới `/booking/**`.

**Trang hoặc API riêng tư mới phải nằm dưới một tiền tố trên**, hoặc thêm tiền tố
mới vào `SecurityConfig` trong cùng thay đổi.

## Alternatives Considered

1. **Mặc định khóa, mở `/error` và các trang công khai.** Trang riêng tư quên khai
   vẫn bị khóa. Bị loại vì người chưa đăng nhập gõ sai đường dẫn sẽ thấy trang đăng
   nhập thay vì trang 404, trái với #21.

## Consequences

Positive:

- Người chưa đăng nhập gõ sai đường dẫn thấy trang 404, đúng yêu cầu #21.
- File tĩnh (`/css`, `/js`, `/fonts`, `/vendor`) và `/error` mở mà không cần dòng
  `permitAll()` riêng; không còn lỗi "trang mất CSS vì Spring Security trả trang
  đăng nhập cho file stylesheet".

Tradeoffs:

- Trang riêng tư đặt ngoài năm tiền tố sẽ mở cho người lạ. Ba lớp chặn: comment
  trên khối luật trong `SecurityConfig`; dòng tự kiểm trong
  `docs/plans/active/huong-dan-task/README.md`; service vẫn tự kiểm quyền sở hữu
  (booking của ai), nên lọt lớp URL thì dữ liệu người khác vẫn không lộ.

## Follow-Up

- Xong ngày 24/10/2026: #22 đã đổi hai nhóm tiền tố sang `hasRole(...)` và cho
  `/api/**` bị chặn trả JSON 401 hoặc 403 thay vì chuyển trang.

# 0009 Bỏ bảng toa và ghế, dùng cấu hình toa cố định

Date: 2026-10-19

## Status

Accepted. Điều chỉnh mục 42 của `SPEC.md` (hai entity `Coach` và `Seat`) và các
mục quản lý toa trong `SPEC.md`. Bổ sung cho
`docs/decisions/0008-gop-ve-vao-hanh-khach.md`.

## Context

Nhóm muốn rút gọn database để query và làm tính năng dễ hơn. Sau ADR 0008, bảng
còn 10. Dữ liệu mẫu cho thấy cả 8 tàu có đúng một cấu hình toa, nên `coaches` và
`seats` thực chất chỉ giữ hằng số: 32 toa và 768 ghế là một cấu hình lặp lại 8
lần. Mỗi lần đọc sơ đồ ghế của một chuyến phải join `trip_seats`, `seats`,
`coaches`.

## Decision

- Bỏ bảng `coaches` và `seats`. Số bảng từ 10 xuống 8: `users`, `stations`,
  `routes`, `trains`, `trips`, `trip_seats`, `bookings`, `tickets`.
- Mọi tàu dùng chung **một cấu hình toa cố định**, viết trong code (không phải
  trong database), gồm 4 toa:

  | Toa | Loại | Số chỗ | Mã chỗ | `price_modifier` |
  | --- | --- | --- | --- | --- |
  | 1, 2 | `SEAT` | 32 | `A01` đến `A32` | 0 |
  | 3, 4 | `SLEEPER` | 16 | `B01` đến `B16`, cabin = (số thứ tự - 1) / 4 + 1 | 200.000 |

  Một chuyến luôn có 96 chỗ.
- Khi tạo chuyến, hệ thống sinh 96 dòng `trip_seats` từ cấu hình này, tất cả
  `AVAILABLE`. Mỗi dòng giữ luôn dữ liệu của chỗ: `coach_number`, `coach_type`,
  `seat_code`, `cabin_number`, `price_modifier`. Sơ đồ ghế của một chuyến đọc từ
  một bảng.
- Giá vé vẫn là `trips.base_price` + `trip_seats.price_modifier`. `price_modifier`
  được chép vào dòng lúc tạo chuyến, nên đổi hằng số sau này không làm đổi giá
  chuyến đã tạo.
- Khóa duy nhất của `trip_seats` đổi thành `(trip_id, coach_number, seat_code)`,
  vẫn là chốt cuối chặn hai booking cùng một chỗ.
- Không còn `seat_type`: toa `SLEEPER` là giường, toa `SEAT` là ghế, `coach_type`
  đã nói rõ.
- Admin không quản lý toa nữa: bỏ chức năng thêm toa, task T08 bị bỏ, và quyết
  định D3 (thêm toa vào tàu đã có chuyến) không còn áp dụng.
- Thực hiện trong migration `V4__simplify_schema.sql` (chung với ADR 0008 và 0010): thêm cột vào
  `trip_seats`, copy từ `seats` và `coaches`, đổi khóa duy nhất, bỏ cột `seat_id`,
  xóa hai bảng. V1-V3 giữ nguyên.

## Alternatives Considered

1. **Giữ `coaches` và `seats`.** Đúng nguyên văn `SPEC.md`, admin cấu hình được
   từng tàu. Không chọn vì không có tàu nào khác cấu hình chuẩn, nên khả năng này
   chưa ai dùng, mà join ba bảng nào cũng phải trả.
2. **Chỉ bỏ `seats`, giữ `coaches`.** Vẫn còn một bảng cấu hình và một join. Không
   chọn vì `coaches` cũng chỉ là bốn dòng giống nhau cho mọi tàu.
3. **Bỏ luôn `routes` và `trains` (còn 6 bảng).** Mất màn admin tuyến và tàu. Chưa
   chọn; có thể xem lại sau.

## Consequences

Positive:

- Ít hơn hai bảng, hai entity, hai repository. Sơ đồ ghế của chuyến là một câu
  `SELECT ... FROM trip_seats WHERE trip_id = ? ORDER BY coach_number, seat_code`.
- Bỏ toàn bộ chức năng thêm toa và đồng bộ chỗ khi thêm toa vào tàu đã có chuyến.

Negative:

- Lệch `SPEC.md`: spec yêu cầu admin cấu hình toa tàu và liệt kê `Coach`, `Seat`
  là entity. Nhóm cần báo giảng viên hoặc ghi chú khi nộp.
- Muốn thêm một kiểu tàu khác thì phải sửa code (và có thể cần lại bảng cấu hình
  toa). Mọi tàu hiện có cùng một cấu hình.
- Dữ liệu chỗ (số toa, mã chỗ, giá phụ thu) lặp trên mọi chuyến: khoảng 96 dòng
  mỗi chuyến, 10.944 dòng với dữ liệu mẫu.
- Migration `V4` dùng cú pháp riêng của MySQL, và không đảo ngược được bằng
  migration (hai bảng bị xóa). Chỉ có bản backup ngoài mới khôi phục được.

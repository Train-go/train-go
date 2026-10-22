# 0008 Gộp vé vào bảng hành khách

Date: 2026-10-19

## Status

Accepted. Điều chỉnh mục 42 của `SPEC.md`, nơi liệt kê `BookingPassenger` và
`Ticket` là hai entity riêng.

## Context

Database có 11 bảng và nhóm muốn rút gọn cho dễ hiểu. Bảng `tickets` có khóa
unique trên `booking_passenger_id`, nên mỗi dòng `booking_passengers` có tối đa
một vé: vé thực chất chỉ là hai cột (`ticket_code`, `issued_at`) của hành khách.
Toàn bộ dữ liệu mẫu V1-V3 cũng có đủ 35 vé cho 35 hành khách.

## Decision

- Gộp `tickets` vào `booking_passengers` rồi đổi tên bảng thành `tickets`; entity
  là `Ticket`. Một dòng `tickets` là một hành khách ngồi một chỗ trong một
  booking, và là vé điện tử của chỗ đó.
- Dòng được tạo cùng booking, lúc chọn hành khách. `ticket_code` và `issued_at`
  để `NULL` cho tới khi booking được thanh toán, rồi điền cùng lúc. Ràng buộc
  `ck_tickets_issued` bắt hai cột này cùng `NULL` hoặc cùng có giá trị.
- Số bảng giảm từ 11 xuống 10. Không đổi quan hệ nào khác.
- Thực hiện trong migration `V4__simplify_schema.sql` (chung với ADR 0009 và 0010): copy
  mã vé và `issued_at` sang `booking_passengers`, xóa `tickets` cũ, đổi tên bảng
  và các ràng buộc. V1-V3 giữ nguyên vì đã chạy trên máy cả nhóm.

## Alternatives Considered

1. **Giữ hai bảng.** Đúng nguyên văn `SPEC.md`. Không chọn vì quan hệ 1-1 không
   mang thêm thông tin, chỉ thêm một join và một entity.
2. **Bỏ `seats`, để `trip_seats` giữ luôn thông tin chỗ.** Giảm thêm một bảng
   nhưng mất khái niệm chỗ vật lý dùng lại qua các chuyến, và lệch spec rõ hơn.
   Chưa chọn; có thể xem lại sau.

## Consequences

Positive:

- Ít hơn một bảng, một entity, một repository. Đọc vé của một hành khách không
  cần join.

Negative:

- Lệch `SPEC.md` mục 42 (hai entity thành một). Nhóm cần báo giảng viên hoặc ghi
  chú rõ khi nộp.
- Bảng `tickets` có dòng chưa phát hành (booking `PENDING`). Mọi truy vấn "vé đã
  phát hành" phải lọc `ticket_code IS NOT NULL`.
- Migration `V4` dùng cú pháp riêng của MySQL (đổi tên FK và CHECK bằng cách xóa
  rồi thêm lại). Dự án đã bỏ H2 nên không ảnh hưởng.

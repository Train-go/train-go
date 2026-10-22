# 0010 Bỏ giữ chỗ, khóa chỗ lúc thanh toán

Date: 2026-10-19

## Status

Accepted. Điều chỉnh các mục giữ chỗ trong `SPEC.md` và thay thế các quyết định
D1 (hết hạn giữ chỗ) và D7 (bản nháp booking) của `docs/plans/active/chia-viec-mvp.md`.
Một phần của ADR 0008 không còn đúng: dòng `tickets` không còn tồn tại trước khi
thanh toán.

## Context

Nhóm chốt ngày 19/10/2026 không làm tính năng giữ chỗ 5 phút. Giữ chỗ kéo theo cột
`held_by_user_id`, `held_until`, trạng thái `HELD`, đếm ngược, xử lý hết hạn, giữ
đè và trả chỗ, và booking `PENDING`. Đây là phần khó và dễ sai nhất của luồng đặt
vé.

## Decision

- Bỏ trạng thái `HELD`. `trip_seats.status` chỉ còn `AVAILABLE` và `BOOKED`. Bỏ cột
  `held_by_user_id` và `held_until`, bỏ ràng buộc `ck_trip_seats_hold`.
- Chỗ được khóa **lúc thanh toán**, không phải lúc chọn chỗ. Giữa các bước chọn
  chỗ, nhập hành khách và xem lại, dữ liệu chỉ nằm trong bản nháp trong session.
  Chưa có dòng nào được ghi vào database.
- Bấm "Thanh toán" (mô phỏng) chạy **một transaction**:
  1. Kiểm tra chuyến còn `SCHEDULED` và chưa khởi hành.
  2. Đổi từng chỗ đã chọn từ `AVAILABLE` sang `BOOKED` bằng câu cập nhật có điều
     kiện (`UPDATE ... SET status = 'BOOKED' WHERE id IN (...) AND status =
     'AVAILABLE'`), rồi so số dòng được cập nhật với số chỗ đã chọn. Thiếu dòng
     nào nghĩa là chỗ đó vừa bị người khác đặt: rollback toàn bộ.
  3. Tạo booking với `CONFIRMED` và `PAID` cùng `paid_at`, rồi tạo các dòng
     `tickets` kèm `ticket_code` và `issued_at`.
- Người đặt chậm hơn thấy thông báo "chỗ vừa được đặt" và quay lại trang chọn chỗ
  với sơ đồ mới. Không có hết hạn, không có giữ đè.
- `tickets.ticket_code` và `tickets.issued_at` quay lại `NOT NULL`, bỏ ràng buộc
  `ck_tickets_issued`: dòng vé giờ chỉ được tạo cùng booking đã thanh toán. Thay
  cho `Ticket.issue(...)`, constructor của `Ticket` nhận `ticketCode`.
- Booking không còn trạng thái `PENDING` và `UNPAID` trong thực tế: booking được tạo
  thẳng `CONFIRMED` / `PAID`. Hai giá trị này vẫn nằm trong các ràng buộc `CHECK`
  của `bookings` nhưng không có code nào dùng. Gọn hơn nữa có thể làm sau.
- Thực hiện trong migration `V4__simplify_schema.sql` (chung với ADR 0008 và 0009). V1-V3 giữ
  nguyên.

## Alternatives Considered

1. **Khóa ngay khi tạo booking `PENDING`.** Kéo theo việc dọn booking bị bỏ dở, tức
   là trở lại bài toán hết hạn. Không chọn.
2. **Giữ chỗ như `SPEC.md`.** Trải nghiệm tốt nhất, nhưng nhóm không làm. Không
   chọn.

## Consequences

Positive:

- Bỏ cột, trạng thái, đếm ngược, job hoặc quy tắc hết hạn và giữ đè. Luồng đặt chỉ
  còn một điểm ghi duy nhất, dễ đọc và dễ thử bằng tay.

Negative:

- Lệch `SPEC.md` (mục giữ chỗ): hai người cùng chọn một chỗ chỉ biết kết quả lúc
  thanh toán, người chậm hơn bị báo lỗi và phải chọn lại. Nhóm nên báo giảng viên
  hoặc ghi chú khi nộp.
- Khách đã nhập xong hành khách vẫn có thể mất chỗ ở bước cuối. Bản nháp còn nguyên
  trong session nên chọn lại chỗ không phải nhập lại.
- Migration `V4` xóa cột nên không đảo ngược được bằng migration.

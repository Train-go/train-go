# T14 Hoàn thiện và kiểm thử xuyên suốt

> **Tóm tắt:** sau task này, toàn bộ luồng đặt vé chạy trơn tru từ đầu tới cuối, lỗi nào
> cũng có thông báo dễ hiểu, trang cho khách dùng tốt trên điện thoại, và cả luồng đặt vé đã
> được thử trọn bằng tay trên trình duyệt. Nhóm sẵn sàng cho buổi demo.

| Đợt | Phần việc | Độ khó | Cần xong trước | Task đang chờ task này | Tên branch |
| --- | --- | --- | --- | --- | --- |
| 4 | Cả nhóm | Vừa (khoảng 2-3 ngày) | T02-T13 | Buổi demo, đóng plan MVP | `feat/t14-polish` (hoặc mỗi phần một branch) |

Các từ như rollback, seed được giải thích trong `README.md` cùng thư mục.

## 1. Task này làm gì

Đây là task cuối, cả nhóm cùng làm khi các task trước đã xong. Mục tiêu:

- MVP chạy thông từ đầu tới cuối như `SPEC.md` mục 50.
- Mọi trường hợp lỗi ở `SPEC.md` mục 45.5 đều có thông báo dễ hiểu.
- Giao diện cho khách dùng được trên điện thoại.
- Cả luồng đặt vé được thử trọn bằng tay trên trình duyệt với MySQL thật, kể cả trường hợp
  hết thời gian giữ chỗ.

## 2. Phạm vi

Làm: sáu phần việc ở mục 4, chia đều cho ba người khi tới đợt 4:

| Phần | Gợi ý người làm | Việc |
| --- | --- | --- |
| Trang lỗi | Phần việc C | `templates/error/404.html`, `403.html`, `500.html` theo khung trang cho khách; Spring Boot tự chọn trang theo mã lỗi |
| Thử trọn luồng | Phần việc C | Chạy tay cả luồng đặt vé trên trình duyệt, kể cả ca hết thời gian giữ chỗ (bước 2) |
| Responsive | Phần việc B | Landing, tìm chuyến, chọn chỗ, chuyến của tôi, vé ở 375, 768, 1280px (`SPEC.md` mục 45.1) |
| Thông báo lỗi | Phần việc B | Rà đủ 7 trường hợp của `SPEC.md` mục 45.5 |
| Dữ liệu demo | Phần việc A | Làm mới dữ liệu mẫu trước buổi demo, chuẩn bị kịch bản demo |
| Tài liệu | Phần việc A | Cập nhật plan, RUNBOOK, đóng plan MVP |

Không làm: các tính năng tùy chọn như mã QR, quét QR, bản đồ, xuất PDF vé (`SPEC.md` mục
47). Chỉ bắt đầu chúng sau khi mọi mục kiểm tra ở cuối file này đều xong.

## 3. Dữ liệu dùng tới

Task này **không đổi database**: không thêm bảng, không thêm cột, không viết migration mới.
Bạn chỉ dùng lại dữ liệu và code của các task trước.

## 4. Các bước làm

Mỗi bước dưới đây là một phần việc độc lập. Người được giao phần nào làm bước đó.

### Bước 1. Trang lỗi

Tạo `templates/error/404.html`, `403.html`, `500.html` theo khung trang cho khách. Spring
Boot tự chọn trang theo mã lỗi, nên bạn không cần viết controller nào. Nội dung bằng tiếng
Việt.

### Bước 2. Thử trọn luồng đặt vé

Chạy tay toàn bộ luồng đặt vé trên trình duyệt, với MySQL thật và tài khoản khách thật,
theo kịch bản A và B ở mục 6. Kịch bản B đẩy hạn giữ chỗ về quá khứ bằng SQL ngay trước khi
bấm "Thanh toán", rồi kiểm tra database không có chỗ `BOOKED` hay vé nào của booking đó.

Vì sao cần kịch bản B: các task trước thử từng bước riêng lẻ. Kịch bản này chứng minh khi
thanh toán thất bại giữa luồng, transaction rollback thật trên database, không để lại dữ
liệu nửa vời.

### Bước 3. Responsive

Mở năm trang cho khách: landing, tìm chuyến, chọn chỗ, chuyến của tôi, vé. Kiểm tra từng
trang ở ba bề rộng màn hình 375, 768 và 1280px (`SPEC.md` mục 45.1). Trang không được tràn
ngang, và chữ tiếng Việt không được bị cắt mất dấu.

### Bước 4. Thông báo lỗi

Thử bằng tay đủ 7 trường hợp lỗi của `SPEC.md` mục 45.5:

| Trường hợp | Ở task nào | Kết quả mong đợi |
| --- | --- | --- |
| Chuyến không tồn tại | T05 | Trang 404 tiếng Việt |
| Chuyến bị hủy | T05, T09 | Nhãn "Đã hủy", không chọn chỗ được |
| Chỗ vừa bị người khác giữ | T09 | Thông báo, sơ đồ tải lại |
| Hết thời gian giữ chỗ | T11, T12 | Thông báo, quay về chi tiết chuyến |
| Booking không tồn tại | T10, T12 | 404 |
| Booking của người khác | T10, T12 | 404 |
| Form không hợp lệ | Mọi form | Lỗi dưới từng ô, giữ dữ liệu đã nhập |

Không trường hợp nào được hiện stack trace (đoạn thông báo lỗi kỹ thuật dài của Java) ra
màn hình. Cách gây ra từng trường hợp nằm ở kịch bản C, mục 6.

### Bước 5. Dữ liệu demo

Ngày trong dữ liệu mẫu (seed) tính từ lúc database được tạo, và chỉ kéo dài 14 ngày. Một
hai ngày trước buổi demo, làm mới dữ liệu theo `docs/RUNBOOK.md`, mục "Làm mới dữ liệu
demo".

Kịch bản demo gợi ý: admin tạo một chuyến mới (T07) → khách tìm và đặt 2 chỗ trên chuyến
đó → xem vé → hủy trong "Chuyến của tôi" → admin thấy booking đã hủy và số liệu trên trang
tổng quan thay đổi.

### Bước 6. Tài liệu

- Ghi mục Result của `docs/plans/active/mvp-traingo.md`, kèm bằng chứng đã kiểm tra, rồi
  chuyển file sang `docs/plans/completed/`. Plan chia việc cũng chuyển theo.
- Cập nhật `docs/RUNBOOK.md` nếu cách chạy ứng dụng có gì thay đổi.

## 5. Luật và lỗi phải xử lý

- Mọi trường hợp lỗi ở bảng bước 4 đều có thông báo dễ hiểu bằng tiếng Việt.
- Không bao giờ hiện stack trace cho người dùng.
- Không bắt đầu tính năng tùy chọn khi các mục kiểm tra ở mục 8 chưa xong.

## 6. Kịch bản thử bằng tay

Làm mới database theo `docs/RUNBOOK.md` trước khi bắt đầu, để mọi chuyến trong seed còn ở
tương lai. Chạy ứng dụng bằng `.\mvnw.cmd spring-boot:run`. Khi cần xem hay sửa database,
mở MySQL bằng lệnh:

```
docker exec -it traingo-mysql mysql -utraingo -ptraingo --default-character-set=utf8mb4 traingo
```

Câu SQL dùng lại nhiều lần trong mục này, xem trạng thái một booking cùng chỗ và vé:

```sql
SELECT b.booking_code, b.booking_status, b.payment_status, b.paid_at,
       ts.id AS trip_seat_id, ts.status, ts.held_by_user_id, t.ticket_code
FROM bookings b
JOIN booking_passengers bp ON bp.booking_id = b.id
JOIN trip_seats ts ON ts.id = bp.trip_seat_id
LEFT JOIN tickets t ON t.booking_passenger_id = bp.id
WHERE b.booking_code = '<mã booking>';
```

**Kịch bản A. Đặt vé thành công, từ đầu tới cuối.**

1. Đăng ký một tài khoản khách mới, đăng nhập.
2. Ở landing, tìm một chuyến còn chỗ, mở chi tiết chuyến.
3. Chọn toa, chọn 2 chỗ, bấm giữ chỗ. Đồng hồ đếm ngược bắt đầu.
4. Nhập 2 hành khách, sang trang xem lại: giá từng vé và tổng tiền đúng.
5. Bấm "Tiếp tục thanh toán", ghi lại mã booking. Bấm "Thanh toán".
6. Trang thành công có 2 vé. Mở từng vé: đủ thông tin, không hiện đủ số CCCD.
7. Chạy câu SQL chung. Kết quả đúng: `CONFIRMED`, `PAID`, `paid_at` có giá trị, 2 chỗ
   `BOOKED` với `held_by_user_id` rỗng, 2 mã vé.
8. Mở `/my-trips`: booking ở nhóm Sắp đi. Hủy booking: chuyển sang Đã hủy, chạy lại SQL thấy
   2 chỗ `AVAILABLE`.
9. Đăng nhập admin: booking hiện trong `/admin/bookings` với trạng thái đã hủy, các con số
   trên dashboard đổi theo.

**Kịch bản B. Hết thời gian giữ chỗ ngay trước khi thanh toán.**

1. Với tài khoản ở kịch bản A, đi lại các bước 2 tới 5 nhưng **chưa bấm** "Thanh toán". Ghi
   lại mã booking.
2. Đẩy hạn giữ chỗ về quá khứ:

   ```sql
   SELECT id FROM users WHERE email = '<email tài khoản của bạn>';
   UPDATE trip_seats SET held_until = NOW() - INTERVAL 1 MINUTE
   WHERE held_by_user_id = <id của bạn> AND status = 'HELD';
   ```

3. Bấm "Thanh toán".
4. Kết quả đúng: quay về chi tiết chuyến kèm thông báo hết thời gian giữ chỗ. Câu SQL chung
   cho thấy `CANCELLED`, `UNPAID`, không chỗ nào `BOOKED`, các chỗ đã về `AVAILABLE`, không
   có vé nào.
5. Mở `/my-trips`: booking này không nằm ở nhóm Sắp đi.

**Kịch bản C. Bảy trường hợp lỗi.**

| Trường hợp | Cách gây ra |
| --- | --- |
| Chuyến không tồn tại | Mở `/trips/999999` |
| Chuyến bị hủy | `UPDATE trips SET status = 'CANCELLED' WHERE id = <id>;` rồi mở chi tiết và trang chọn chỗ của chuyến đó; xong trả về `SCHEDULED` |
| Chỗ vừa bị người khác giữ | Mở trang chọn chỗ của cùng một chuyến ở hai trình duyệt, hai tài khoản; người thứ nhất giữ một chỗ, người thứ hai bấm giữ đúng chỗ đó |
| Hết thời gian giữ chỗ | Đẩy `held_until` về quá khứ như kịch bản B, ở trang hành khách và ở trang thanh toán |
| Booking không tồn tại | Mở `/my-trips/999999` |
| Booking của người khác | Đổi id trên đường dẫn `/my-trips/<id>` và `/tickets/<id>` sang của tài khoản khác |
| Form không hợp lệ | Gửi form đăng ký, tìm chuyến, hành khách và các form admin với ô trống hoặc sai định dạng |

Kết quả đúng của từng trường hợp là cột cuối của bảng ở bước 4. Không trang nào hiện stack
trace.

**Kịch bản D. Responsive.**

Mở DevTools, chế độ thiết bị, lần lượt đặt bề rộng 375, 768, 1280px cho năm trang ở bước 3.
Kết quả đúng: không có thanh cuộn ngang, chữ tiếng Việt có dấu không bị cắt ở trên hay dưới.

## 7. Lỗi hay gặp

- Chỉ kiểm tra giao diện ở màn hình máy tính: lỗi tràn ngang trên điện thoại chỉ lộ ra ở bề
  rộng 375px.
- Quên làm mới dữ liệu mẫu trước buổi demo: các chuyến trong seed đã qua ngày, tìm chuyến
  không ra kết quả nào.

## 8. Kiểm tra trước khi báo xong

- [ ] Kịch bản A và B ở mục 6 chạy trọn và đạt trên MySQL thật.
- [ ] Bảy trường hợp lỗi ở bước 4 đều thử bằng tay và đạt.
- [ ] Năm trang cho khách không tràn ngang ở 375px; chữ tiếng Việt không bị cắt dấu.
- [ ] Kịch bản demo chạy trọn trên MySQL với dữ liệu mới.
- [ ] `docs/plans/active/mvp-traingo.md` có mục Result kèm bằng chứng, rồi được chuyển sang
      `docs/plans/completed/`. Plan chia việc cũng chuyển theo.
- [ ] `.\mvnw.cmd compile` chạy không lỗi và ứng dụng khởi động được
      (`.\mvnw.cmd spring-boot:run`).
- [ ] Mỗi phần việc có một dòng trong `docs/WORKLOG.md`, nằm trong cùng commit với code.

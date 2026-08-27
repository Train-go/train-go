# Luồng đặt vé

Nguồn gốc: `SPEC.md` mục 16–32. Đây là nghiệp vụ khó nhất của dự án và là nơi
mọi lỗi dữ liệu sẽ lộ ra.

## Các bước

```
Tìm chuyến -> Chọn chuyến -> Chọn toa -> Chọn ghế / giường
  -> Nhập hành khách -> Review -> Thanh toán mô phỏng -> Vé điện tử
```

## Trạng thái của một chỗ

```
AVAILABLE  chỗ trống, ai cũng chọn được
HELD       một Customer đang giữ tạm trong lúc hoàn tất booking
BOOKED     chỗ đã thuộc về một booking đã xác nhận
```

`SELECTED` chỉ tồn tại trên giao diện, không lưu database.

Chuyển trạng thái hợp lệ:

```
AVAILABLE -> HELD -> BOOKED          (thanh toán thành công)
AVAILABLE -> HELD -> AVAILABLE       (hết thời gian giữ chỗ)
BOOKED    -> AVAILABLE               (hủy booking)
```

## Giữ chỗ

Khi Customer chọn một chỗ, hệ thống chuyển TripSeat sang `HELD` và ghi `heldBy`
cùng `heldUntil`. Thời gian giữ là **5 phút**. Giao diện đếm ngược cho người dùng
thấy còn bao lâu.

Customer khác không được giữ một TripSeat đang `HELD` hoặc `BOOKED`.

Backend luôn kiểm tra lại trạng thái trước khi xác nhận booking, không tin trạng
thái mà trình duyệt gửi lên.

Phiên bản đầu không bắt buộc dùng WebSocket. Giao diện chỉ cần hỏi lại trạng thái
ghế khi cần.

Điểm cần quyết khi làm: chỗ hết hạn `HELD` được trả về `AVAILABLE` bằng cách nào —
kiểm tra lười khi có người đọc lại sơ đồ ghế, hay một job chạy định kỳ. Chưa có
quyết định, đừng tự chọn ngầm trong code mà không ghi lại.

## Thông tin hành khách

Mỗi hành khách gồm họ tên, ngày sinh, CCCD hoặc passport, và chỗ được gán.

Số hành khách phải bằng số chỗ đã chọn. Mỗi hành khách gán đúng một chỗ.

Phiên bản đầu không có giá theo người lớn / trẻ em, người cao tuổi, sinh viên,
không có chính sách giảm giá.

## Review trước khi trả tiền

Màn hình review hiển thị: tuyến, ngày giờ đi, tàu, toa, ghế hoặc giường, danh
sách hành khách, giá từng vé và tổng tiền. Có nút quay lại và nút tiếp tục thanh
toán.

## Thanh toán mô phỏng

Không kết nối ngân hàng, không cổng thanh toán, không form nhập thẻ, không
callback, không webhook, không hoàn tiền thật.

Khi người dùng bấm thanh toán:

```
Booking.paymentStatus  UNPAID  -> PAID
Booking.bookingStatus  PENDING -> CONFIRMED
TripSeat.status        HELD    -> BOOKED
sinh Ticket cho từng hành khách
```

## Transaction khi xác nhận booking

Bốn việc trên phải nằm trong **một** transaction. Đánh dấu service bằng
`@Transactional`.

Hai trạng thái tuyệt đối không được phép tồn tại:

- Booking đã `CONFIRMED` nhưng TripSeat vẫn `AVAILABLE`.
- TripSeat đã `BOOKED` nhưng Booking chưa được xác nhận.

Trước khi commit, backend phải kiểm tra lại: chuyến còn hợp lệ, các TripSeat vẫn
đang được chính Customer này giữ, và thời gian giữ chưa hết hạn.

## Vé điện tử

Sau khi booking thành công, mỗi hành khách có một vé riêng gồm mã vé, mã booking,
tên hành khách, tuyến, tàu, ngày giờ khởi hành, toa và chỗ. Vé xem trực tiếp trên
web.

QR code và xuất PDF là tính năng optional, làm sau khi MVP ổn định, theo thứ tự
ưu tiên: vé trên web → QR code → PDF. Nếu làm QR, mã chỉ chứa `ticketCode` hoặc
`ticketId`, tuyệt đối không chứa CCCD, passport, thông tin thanh toán hay toàn bộ
thông tin cá nhân.

## My Trips

Customer xem booking của mình, chia ba nhóm Upcoming, Completed, Cancelled. Mỗi
dòng hiển thị tuyến, ngày đi, tàu, toa, chỗ, trạng thái booking và trạng thái
thanh toán. Hành động: xem booking, xem vé, hủy booking.

Không cần filter hay tìm kiếm nâng cao.

## Hủy booking

Customer hủy được khi booking đang `CONFIRMED` và chuyến chưa khởi hành.

```
Booking   CONFIRMED -> CANCELLED
TripSeat  BOOKED    -> AVAILABLE
```

Phiên bản đầu không tính phí hủy, không hoàn tiền, không có quy định 24h hay 48h.
Vì thanh toán chỉ là mô phỏng, hủy chỉ đổi trạng thái và trả lại chỗ.

Việc hủy cũng phải nằm trong transaction.

## Các lỗi phải xử lý tử tế

- Chuyến không tồn tại hoặc đã bị hủy.
- Chỗ vừa bị người khác đặt trong lúc mình đang thao tác.
- Hết thời gian giữ chỗ.
- Booking không tồn tại.
- Booking không thuộc về Customer đang đăng nhập.
- Form không hợp lệ.

Mỗi trường hợp cần một thông báo người dùng hiểu được, không để văng stack trace
ra màn hình.

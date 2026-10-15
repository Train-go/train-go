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

Chỗ hết hạn giữ được trả lại bằng cách so giờ khi đọc, không có job chạy định kỳ
(D1, chốt 12/10/2026). Database có thể vẫn ghi `HELD`, nhưng chỗ `HELD` mà
`heldUntil` đã qua được coi là `AVAILABLE` ở mọi nơi: khi đếm chỗ trống, khi vẽ
sơ đồ ghế, và khi người khác muốn giữ. Người khác giữ chỗ đó thì ghi đè `heldBy`
và `heldUntil`. Người giữ cũ quay lại sau khi hết hạn sẽ nhận lỗi "hết thời gian
giữ chỗ".

## Bản nháp booking giữa các bước

Dữ liệu giữa các bước đặt vé được lưu như sau (D7, chốt 12/10/2026):

```
Trang chọn chỗ, bấm "Tiếp tục"
  -> giữ mọi chỗ đã chọn trong một transaction, lưu bản nháp booking trong session
Trang hành khách, trang review
  -> chỉ sửa bản nháp trong session, chưa ghi database
Trang review, bấm "Tiếp tục thanh toán"
  -> tạo Booking PENDING / UNPAID trong database, giá chốt tại lúc này
Trang thanh toán, bấm "Thanh toán"
  -> xác nhận booking (mục Thanh toán mô phỏng)
```

Booking `PENDING` không hiện ở My Trips.

## Thông tin hành khách

Mỗi hành khách gồm họ tên, ngày sinh, CCCD hoặc passport, và chỗ được gán.

Số hành khách phải bằng số chỗ đã chọn. Mỗi hành khách gán đúng một chỗ.

Mỗi booking có từ 1 đến 10 hành khách (D6, chốt 12/10/2026), khớp ô "Hành khách"
ở trang chủ. Backend kiểm tra giới hạn này ở form tìm chuyến và khi giữ chỗ,
không chỉ dựa vào ô nhập trên trình duyệt. Giới hạn trên ngăn một người giữ cả
toa trong 5 phút.

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

Cách tạo mã (D5, chốt 12/10/2026), giống dữ liệu mẫu:

- Mã booking: `BK` + năm tháng + số thứ tự 3 chữ số trong tháng, lấy số tiếp
  theo số lớn nhất đã có, ví dụ `BK202610005`.
- Mã vé: mã booking đổi `BK` thành `TK`, thêm `-` và thứ tự hành khách 2 chữ số,
  ví dụ `TK202610005-01`.

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

"Chưa khởi hành" nghĩa là ngày giờ khởi hành (`departureDate` cộng
`departureTime`) còn ở sau hiện tại (D8, chốt 12/10/2026). Luật đặt, giữ chỗ và
hủy đều so như vậy, không dựa vào `Trip.status = COMPLETED`; trạng thái đó chỉ để
hiển thị. Nhóm Upcoming và Completed ở My Trips cũng chia theo cùng phép so này.

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

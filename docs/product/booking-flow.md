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
BOOKED     chỗ đã thuộc về một booking đã xác nhận
```

`SELECTED` chỉ tồn tại trên giao diện, không lưu database.

Chuyển trạng thái hợp lệ:

```
AVAILABLE -> BOOKED     (thanh toán thành công)
BOOKED    -> AVAILABLE  (hủy booking)
```

## Không giữ chỗ

Dự án không có giữ chỗ tạm (`docs/decisions/0010-bo-giu-cho.md`). Chọn một chỗ trên
sơ đồ không khóa chỗ đó với người khác. Chỗ chỉ được khóa **lúc thanh toán**, bên
trong một transaction. Hai người cùng chọn một chỗ thì ai thanh toán trước được
chỗ; người sau nhận thông báo "chỗ vừa được người khác đặt" và quay lại trang chọn
chỗ với sơ đồ mới.

Backend luôn kiểm tra lại trạng thái chỗ trong transaction thanh toán, không tin
trạng thái mà trình duyệt gửi lên. Phiên bản đầu không bắt buộc dùng
WebSocket. Giao diện chỉ cần hỏi lại trạng thái ghế khi cần.

## Bản nháp booking giữa các bước

Dữ liệu giữa các bước đặt vé chỉ nằm trong session cho tới khi thanh toán:

```
Trang chọn chỗ, bấm "Tiếp tục"
  -> lưu các chỗ đã chọn vào bản nháp trong session, chưa ghi database
Trang hành khách, trang review
  -> chỉ sửa bản nháp trong session
Trang thanh toán, bấm "Thanh toán"
  -> một transaction tạo booking, đổi chỗ, phát hành vé (mục Thanh toán mô phỏng)
```

Bỏ dở giữa chừng thì không có gì trong database cần dọn. Booking không bao giờ ở
trạng thái chờ: nó được tạo thẳng `CONFIRMED` / `PAID`, nên My Trips không có
booking chưa thanh toán.

## Thông tin hành khách

Mỗi hành khách chỉ nhập họ tên (`fullName`) và số CCCD (`identityNumber`),
đồng thời gắn với một chỗ đã chọn. Họ tên được điền sẵn từ tài khoản đang đăng nhập
và cho phép sửa để đặt cho người khác. Không thu thập ngày sinh.

Số hành khách phải bằng số chỗ đã chọn. Mỗi hành khách gán đúng một chỗ.

Mỗi booking có từ 1 đến 10 hành khách (D6, chốt 12/10/2026, sửa chữ 21/10/2026).
Số chỗ khách chọn ở bước chọn chỗ là số vé, tối đa 10; trang chủ và trang Chọn
chuyến không có ô "Hành khách". Backend kiểm tra giới hạn này ở bước chọn chỗ và
khi thanh toán, không chỉ dựa vào giao diện. Giới hạn trên ngăn một người đặt cả
toa trong một lần.

Phiên bản đầu không có giá theo người lớn / trẻ em, người cao tuổi, sinh viên,
không có chính sách giảm giá.

Giá mỗi ghế hoặc giường đều bằng `Trip.basePrice`, không có phụ thu theo toa.
Khi thanh toán, lưu giá này vào `Ticket.ticketPrice` và cộng các vé thành
`Booking.totalPrice`. Giá vé và tổng tiền booking cũ giữ nguyên khi giá chuyến
thay đổi hoặc khi áp migration V6.

## Review trước khi trả tiền

Màn hình review hiển thị: tuyến, ngày giờ đi, tàu, toa, ghế hoặc giường, danh
sách hành khách, giá từng vé và tổng tiền. Có nút quay lại và nút tiếp tục thanh
toán.

## Thanh toán mô phỏng

Không kết nối ngân hàng, không cổng thanh toán, không form nhập thẻ, không
callback, không webhook, không hoàn tiền thật.

Khi người dùng bấm thanh toán, **một** transaction (đánh dấu service bằng
`@Transactional`) làm đủ các việc sau, hoặc không làm gì cả:

```
1. Kiểm tra chuyến còn SCHEDULED và chưa khởi hành
2. TripSeat.status   AVAILABLE -> BOOKED  cho mọi chỗ đã chọn, bằng một câu cập nhật có điều kiện
                     (WHERE status = 'AVAILABLE'); thiếu dòng nào so với số chỗ đã chọn thì rollback
3. Tạo Booking với bookingStatus = CONFIRMED, paymentStatus = PAID, paidAt = bây giờ
4. Tạo một Ticket cho từng hành khách, kèm ticketCode và issuedAt
```

Hai trạng thái tuyệt đối không được phép tồn tại:

- Booking đã `CONFIRMED` nhưng TripSeat vẫn `AVAILABLE`.
- TripSeat đã `BOOKED` nhưng không thuộc booking nào đã xác nhận.

Bước 2 là chốt chặn hai người đặt trùng chỗ, cùng với khóa duy nhất trên
`trip_seats` ở tầng database.

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
`ticketId`, tuyệt đối không chứa CCCD, thông tin thanh toán hay toàn bộ
thông tin cá nhân.

## My Trips

Customer xem booking của mình ở **Vé của tôi** `/my-trips` (#31), chia ba tab theo
D8, mặc định mở Sắp đi:

- Sắp đi: `CONFIRMED`, giờ đi sau bây giờ (xếp giờ tăng dần);
- Đã đi: `CONFIRMED`, giờ đi không sau bây giờ (xếp giờ giảm dần);
- Đã hủy: `CANCELLED`.

Mỗi booking là một thẻ: ga đi → ga đến, thứ ngày giờ đi, mã tàu, các chỗ ("Toa 1 ·
A05, Toa 3 · B02"), số vé, tổng tiền, mã booking, dấu "Đã thanh toán" hoặc "Đã
hủy". Tab trống có câu gợi ý và nút "Tìm chuyến".

`/my-trips/{bookingId}` là chi tiết dạng hóa đơn: mã booking, chuyến, giờ đi → giờ
đến, tàu, bảng vé (mã vé, họ tên, CCCD chỉ hiện 4 số cuối, toa và chỗ, giá), tổng
tiền, thời điểm thanh toán. Booking sắp đi có nút "Hủy booking" (mục dưới). Nút "Xem
vé" thêm khi có vé điện tử (#34).

Booking không phải của khách đang đăng nhập, kể cả gõ tay id hay gửi thẳng `POST`
hủy, nhận trang 404 "Không tìm thấy booking." như booking không tồn tại, nên không lộ
mã hay thông tin của người khác (`docs/product/auth.md`).

Không cần filter hay tìm kiếm nâng cao.

## Hủy booking

Customer hủy được khi booking đang `CONFIRMED` và chuyến chưa khởi hành.

"Chưa khởi hành" nghĩa là ngày giờ khởi hành (`departureAt`) còn ở sau hiện tại
(D8, chốt 12/10/2026). Luật đặt và
hủy đều so như vậy, không dựa vào `Trip.status = COMPLETED`; trạng thái đó chỉ để
hiển thị. Nhóm Upcoming và Completed ở My Trips cũng chia theo cùng phép so này.

```
Booking   CONFIRMED -> CANCELLED
TripSeat  BOOKED    -> AVAILABLE
```

Phiên bản đầu không tính phí hủy, không hoàn tiền, không có quy định 24h hay 48h.
Vì thanh toán chỉ là mô phỏng, hủy chỉ đổi trạng thái và trả lại chỗ.

Việc hủy cũng phải nằm trong transaction.

Luật hủy chỉ nằm ở `BookingCancellationService`, dùng chung cho khách (Vé của tôi)
và admin (#35). Khách bấm "Hủy booking" thấy hộp xác nhận "N chỗ trên chuyến … sẽ mở
bán lại. Thanh toán chỉ là mô phỏng nên không có hoàn tiền."; hủy xong quay về trang
chi tiết với thông báo "Đã hủy booking BK…". Gửi hủy cho booking đã đi hoặc đã hủy
bị từ chối với "Không hủy được: chuyến đã khởi hành." hoặc "Booking đã hủy.", không
đổi gì trong database.

## Các lỗi phải xử lý tử tế

- Chuyến không tồn tại hoặc đã bị hủy.
- Chỗ vừa bị người khác đặt trong lúc mình đang thao tác.
- Booking không tồn tại.
- Booking không thuộc về Customer đang đăng nhập.
- Form không hợp lệ.

Mỗi trường hợp cần một thông báo người dùng hiểu được, không để văng stack trace
ra màn hình.

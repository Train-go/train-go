# Mô hình dữ liệu

Nguồn gốc: `SPEC.md` mục 9–23 và 42. Tài liệu này mô tả entity, quan hệ, trạng
thái và ràng buộc mà code phải tuân theo.

Schema thật nằm trong `app/src/main/resources/db/migration/` (V1 tạo bảng, V4
đơn giản hóa xuống 8 bảng, V5 thêm lý trình ga và giờ chuyến kiểu ngày-giờ, V6 bỏ ngày sinh hành khách và phụ thu chỗ) và được
Flyway tạo khi ứng dụng khởi động
(`docs/decisions/0005-quan-ly-schema-bang-flyway.md`). Tài liệu này giải thích ý
nghĩa; khi tên cột hay ràng buộc cụ thể khác nhau, file migration là bản đúng.
Entity Java nằm trong `com.voyagego.traingo.entity`.

Mọi số tiền lưu kiểu `long` (cột `BIGINT`), đơn vị đồng.

## Danh sách entity

`User`, `Station`, `Route`, `Train`, `Trip`, `TripSeat`, `Booking`, `Ticket`.

`SPEC.md` mục 42 còn liệt kê `Coach`, `Seat`, `BookingPassenger`. Dự án bỏ `Coach`
và `Seat` (mọi tàu dùng một cấu hình toa cố định trong code, xem
`docs/decisions/0009-bo-bang-toa-va-ghe.md`) và gộp `BookingPassenger` vào
`Ticket` (`docs/decisions/0008-gop-ve-vao-hanh-khach.md`).

MVP không có `Staff`, `CheckIn`, `RouteStation`, `TripStation`, `BookingSeat`,
`Payment`.

## Quan hệ tổng quát

```
Station <- Route -> Station         (ga đi, ga đến; không có ga trung gian)

Train                            (chỉ có mã và tên; cấu hình toa cố định nằm trong code)
 └── Trip                        (một lần chạy của tàu trên một Route; các chuyến của một tàu nối chặng)
 └── TripSeat                        (một chỗ của chuyến: giữ luôn số toa, mã chỗ và trạng thái)

User
 └── Booking
      ├── Trip
      └── Ticket                     (một hành khách trên một TripSeat)
           └── TripSeat
```

Điểm cốt lõi phải hiểu đúng: **Train là con tàu, Trip là một lần chạy**. Một
Train dùng cho nhiều Trip khác nhau. Vì thế trạng thái đặt chỗ không thể lưu trên
Train, mà phải lưu trên `TripSeat` của từng chuyến.

## User

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `fullName` | Bắt buộc, tối đa 100 ký tự |
| `email` | Không được trùng, dùng để đăng nhập |
| `passwordHash` | BCrypt hash, không bao giờ lưu mật khẩu gốc |
| `role` | `CUSTOMER` hoặc `ADMIN` |
| `createdAt` | |

## Station

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `name` | Bắt buộc, ví dụ `Ga Sài Gòn` |
| `code` | Không được trùng, ví dụ `SGN`. Chỉ gồm chữ cái không dấu và chữ số, tối đa 10 ký tự, luôn lưu chữ hoa |
| `city` | Ví dụ `TP. Hồ Chí Minh` |
| `address` | |
| `kmMark` | Lý trình: số km tính từ ga Hà Nội, ví dụ Huế là 688. Bắt buộc, không âm, không trùng |

Mọi danh sách ga xếp theo `kmMark`, tức từ Bắc vào Nam: trang Ga, ô chọn ga khi tìm
chuyến, form tuyến. Cột này lệch `SPEC.md` mục 9
(`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`).

Dữ liệu mẫu có 8 ga trên trục Bắc - Nam:

| Mã | Ga | Lý trình (km) |
| --- | --- | --- |
| `HNI` | Hà Nội | 0 |
| `THA` | Thanh Hóa | 175 |
| `VIN` | Vinh | 319 |
| `DHO` | Đồng Hới | 522 |
| `HUE` | Huế | 688 |
| `DNG` | Đà Nẵng | 791 |
| `NTR` | Nha Trang | 1315 |
| `SGN` | Sài Gòn | 1726 |

`latitude`, `longitude` chỉ cần khi làm Railway Map (tính năng optional), nên
chưa có trong schema. Khi làm bản đồ thì thêm bằng một migration mới.

## Route

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `originStationId` | Phải khác `destinationStationId`, database chặn |
| `destinationStationId` | |
| `distanceKm` | Khoảng cách theo km, lớn hơn 0 |

Mỗi cặp ga đi - ga đến chỉ có một tuyến; chiều ngược lại là một tuyến khác.

Không quản lý ga trung gian. Quyết định này giúp database, tìm chuyến, tính giá
và booking đều đơn giản hơn.

Form tuyến gợi ý khoảng cách bằng hiệu lý trình của hai ga; admin vẫn sửa được.
Danh sách tuyến xếp theo lý trình ga đi, rồi ga đến.

Dữ liệu mẫu có 16 tuyến: 7 chặng giữa hai ga kề nhau, mỗi chặng hai chiều, cộng
`HNI` ↔ `SGN`. Khách chỉ đặt được cặp ga có tuyến; không có vé nối chuyến (SPEC mục
32.1).

Chỉ xóa được ga khi không tuyến nào dùng nó (`docs/product/admin.md`).

## Train và cấu hình toa cố định

`Train`:

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `code` | Không được trùng, ví dụ `SE1` |
| `name` | Bắt buộc, ví dụ `Thống Nhất 1` |

Tàu không gắn với tuyến nào: tàu nào cũng chạy được tuyến nào (Phi chốt
20/10/2026). Thứ tự dựng dữ liệu vì thế là ga, rồi tuyến, rồi chuyến; tàu tạo lúc
nào cũng được, miễn là trước chuyến. Dữ liệu mẫu có 3 tàu `SE1`, `SE2`, `SE3`.

Database không có bảng toa hay ghế. Mọi tàu dùng chung **một cấu hình toa cố định**
bất biến trong code (`model/dto/CoachLayout`). Cấu hình có 4 toa, 96 chỗ:

| Toa | Loại (`coachType`) | Số chỗ mỗi toa | Mã chỗ | Cabin |
| --- | --- | --- | --- | --- |
| 1, 2 | `SEAT` | 32 | `A01` đến `A32` | không có |
| 3, 4 | `SLEEPER` | 16 | `B01` đến `B16` | (số thứ tự - 1) / 4 + 1, tức `B01`-`B04` là cabin 1 |

Số toa hiển thị dạng `Toa 01`. Toa `SEAT` là ghế, toa `SLEEPER` là giường, nên
không lưu loại chỗ riêng. Mỗi cabin 4 giường. Phiên bản đầu không phân biệt giường
tầng 1, 2, 3, không có VIP, không có phòng riêng, không có nhiều mức giá phức tạp.

Admin không thêm, sửa hay xóa toa. Muốn đổi cấu hình thì sửa hằng số trong code;
chuyến đã tạo giữ nguyên các chỗ đã sinh. Mọi ghế và giường trong cùng chuyến có cùng giá `Trip.basePrice`.

## Trip

| Trường | Ghi chú |
| --- | --- |
| `id`, `trainId`, `routeId` | |
| `departureAt` | Ngày và giờ khởi hành |
| `arrivalAt` | Ngày và giờ đến, phải sau `departureAt` (database chặn) |
| `basePrice` | Giá cơ bản của chuyến |
| `status` | `SCHEDULED`, `COMPLETED`, `CANCELLED` |

MVP không có trạng thái `BOARDING`, `DEPARTED`, `DELAYED`.

SPEC mục 11.2 cho Trip một ngày khởi hành và hai giờ. Dự án lưu hai mốc ngày-giờ
đầy đủ để chuyến từ 24 giờ trở lên (Hà Nội - Sài Gòn) lưu được
(`docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md`).

Chuyến được coi là đã khởi hành khi `departureAt` đã qua (D8, chốt 12/10/2026).
Mọi luật đặt và hủy so thời điểm này với hiện tại; `COMPLETED` chỉ để hiển thị và
không bắt buộc phải được gán. Chuyến không bị xóa, chỉ bị hủy (`CANCELLED`), và
không hủy được khi đã có booking `CONFIRMED` (D2).

### Lịch của tàu và luật nối chặng

Chuyến thuộc về tàu: admin thêm chuyến ở trang chi tiết tàu, không phải ở trang
tuyến. Các chuyến chưa hủy của một tàu, xếp theo `departureAt`, là lịch của tàu đó.

1. Tàu chưa có chuyến nào chưa hủy: chọn tuyến bất kỳ.
2. Tàu đã có chuyến: chuyến mới nối vào cuối lịch. Ga đi bắt buộc là ga đến của
   chuyến cuối, nên chỉ chọn được tuyến xuất phát từ ga đó.
3. Giờ đi phải sau giờ đến của chuyến cuối ít nhất 30 phút (thời gian quay đầu).
4. Vì chỉ nối vào cuối, hai chuyến của một tàu không bao giờ chồng giờ.

Service kiểm tra cả bốn điều trên, không tin dữ liệu form. Hủy một chuyến ở giữa
làm chuỗi bị đứt; trang tàu đánh dấu chỗ đó nhưng không chặn thao tác nào.

Dữ liệu mẫu: chỉ `SE1` có chuyến, mỗi ngày một chặng, khởi hành 08:00, 7 ngày xuống
Nam rồi 7 ngày lên Bắc. Thời gian chạy bằng 1 phút cho mỗi km (60 km/h) làm tròn 5
phút; giá cơ bản khoảng 1.000đ/km làm tròn 10.000đ.

Khi tạo Trip, hệ thống sinh 96 `TripSeat` từ cấu hình toa cố định ở trên, tất cả ở
trạng thái `AVAILABLE`, trong cùng transaction với việc tạo chuyến.

## TripSeat

| Trường | Ghi chú |
| --- | --- |
| `id`, `tripId` | |
| `coachNumber` | Số toa trong chuyến, 1 đến 4 |
| `coachType` | `SEAT` hoặc `SLEEPER` |
| `seatCode` | `A01`... cho ghế, `B01`... cho giường. Bộ ba `tripId + coachNumber + seatCode` phải là duy nhất |
| `cabinNumber` | Chỉ có với toa `SLEEPER`. Null với ghế |
| `status` | `AVAILABLE`, `BOOKED` |

Ràng buộc duy nhất trên `tripId + coachNumber + seatCode` phải được khai báo ở tầng database, vì
đây là lớp phòng thủ cuối cùng chống việc hai booking cùng chiếm một chỗ.

Không có trạng thái giữ chỗ: chỗ được đổi sang `BOOKED` lúc thanh toán, trong cùng
transaction với việc tạo booking (`docs/decisions/0010-bo-giu-cho.md`).

`SELECTED` chỉ là trạng thái giao diện khi người dùng vừa click, không lưu xuống
database.

## Booking

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `bookingCode` | Mã hiển thị cho người dùng, ví dụ `BK202608001` |
| `customerId`, `tripId` | Một booking chỉ thuộc một Trip |
| `totalPrice` | |
| `bookingStatus` | `CONFIRMED`, `CANCELLED` (`PENDING` vẫn được database cho phép nhưng không có luồng nào dùng) |
| `paymentStatus` | `PAID` (`UNPAID` vẫn được database cho phép nhưng không có luồng nào dùng) |
| `paidAt`, `createdAt` | |

Không có bảng `Payment` riêng; trạng thái thanh toán lưu ngay trong Booking.

Database chặn booking `CONFIRMED` mà chưa `PAID`, và booking `PAID` mà thiếu
`paidAt`.

Mã booking có dạng `BK` + năm tháng + số thứ tự 3 chữ số, ví dụ `BK202610001`.
Mã mới lấy số thứ tự tiếp theo từ những mã đã có trong tháng, không đếm lại từ
`001` (D5, chốt 12/10/2026, chi tiết ở `booking-flow.md`).

Không có trạng thái `COMPLETED`. Booking `CONFIRMED` mà chuyến đã khởi hành thì
giao diện tự xếp vào nhóm đã hoàn thành (D8).

## Ticket

Một dòng là một hành khách ngồi một chỗ trong một booking, và là vé điện tử của
chỗ đó. Dòng được tạo trong transaction thanh toán, cùng với booking, nên luôn có
mã vé.

| Trường | Ghi chú |
| --- | --- |
| `id`, `bookingId`, `tripSeatId` | Mỗi hành khách gắn đúng một TripSeat |
| `fullName` | |
| `identityNumber` | Số CCCD |
| `ticketPrice` | Giá tại thời điểm đặt |
| `ticketCode` | Bắt buộc, không được trùng. Mã booking đổi `BK` thành `TK` rồi thêm thứ tự hành khách, ví dụ `TK202610001-01` |
| `issuedAt` | Bắt buộc. Lúc booking được thanh toán |

`ticketPrice` phải được lưu lại tại thời điểm đặt. Admin sửa `basePrice` của Trip
về sau không được làm thay đổi giá của booking cũ.

Một TripSeat chỉ xuất hiện một lần trong cùng một booking, nhưng có thể xuất
hiện ở nhiều booking khác nhau: booking bị hủy thì chỗ được bán lại. Việc chỉ
một booking đã xác nhận giữ chỗ được bảo đảm bằng `TripSeat.status`, không phải
bằng ràng buộc unique trên bảng này.

Vé hiển thị: `ticketCode`, `bookingCode`, tên hành khách, tuyến, tàu, ngày giờ
khởi hành, toa, ghế hoặc giường. Các thông tin này đọc qua `Ticket` → `TripSeat`,
không chép thêm. Vé được xem trực tiếp trên website.

## Công thức giá

```
Ticket Price = Trip.basePrice
Booking.totalPrice = tổng Ticket.ticketPrice
```

Ví dụ: chuyến có `basePrice` 350.000đ thì ghế và giường đều 350.000đ;
đặt 2 chỗ có tổng tiền 700.000đ. Giá vé đã đặt được đóng băng trong `ticketPrice`,
kể cả vé cũ từng có phụ thu; migration V6 không tính lại vé hoặc tổng booking cũ.

Không có dynamic pricing, không có giá theo độ tuổi hay đối tượng.

## Ràng buộc nghiệp vụ phải được backend kiểm tra

1. Một Booking chỉ thuộc một Trip. Không có multi-trip, connecting train hay
   khứ hồi trong cùng một booking.
2. Một Booking có thể chứa nhiều hành khách.
3. Mỗi hành khách gắn đúng một TripSeat; một TripSeat không được gán cho hai
   hành khách.
4. Số hành khách phải bằng số chỗ đã chọn.
5. Một TripSeat chỉ thuộc về một booking đã xác nhận.
6. Trip có `status = CANCELLED` thì không được chọn chỗ, tạo booking hay thanh
   toán.
7. Trip trong quá khứ không được đặt.
8. Booking chỉ chuyển sang `CONFIRMED` khi `paymentStatus = PAID`.

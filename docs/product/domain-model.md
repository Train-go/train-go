# Mô hình dữ liệu

Nguồn gốc: `SPEC.md` mục 9–23 và 42. Tài liệu này mô tả entity, quan hệ, trạng
thái và ràng buộc mà code phải tuân theo.

## Danh sách entity

`User`, `Station`, `Route`, `Train`, `Coach`, `Seat`, `Trip`, `TripSeat`,
`Booking`, `BookingPassenger`, `Ticket`.

MVP không có `Staff`, `CheckIn`, `RouteStation`, `TripStation`, `BookingSeat`,
`Payment`.

## Quan hệ tổng quát

```
Station <- Route -> Station         (ga đi, ga đến; không có ga trung gian)

Train
 └── Coach
      └── Seat                       (cấu hình vật lý của đoàn tàu)

Trip (một lần chạy cụ thể của Train trên một Route vào một ngày)
 └── TripSeat -> Seat                (trạng thái chỗ theo từng chuyến)

User
 └── Booking
      ├── Trip
      └── BookingPassenger
           ├── TripSeat
           └── Ticket
```

Điểm cốt lõi phải hiểu đúng: **Train là cấu hình vật lý, Trip là một lần chạy**.
Một Train dùng cho nhiều Trip khác nhau. Vì thế trạng thái đặt chỗ không thể lưu
trên `Seat`, mà phải lưu trên `TripSeat`.

## Station

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `name` | Bắt buộc, ví dụ `Ga Sài Gòn` |
| `code` | Không được trùng, ví dụ `SGN` |
| `city` | Ví dụ `Hồ Chí Minh` |
| `address` | |
| `latitude`, `longitude` | Chỉ cần khi làm Railway Map (tính năng optional) |

## Route

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `originStationId` | Phải khác `destinationStationId` |
| `destinationStationId` | |
| `distance` | |

Không quản lý ga trung gian. Quyết định này giúp database, tìm chuyến, tính giá
và booking đều đơn giản hơn.

## Train, Coach, Seat

`Train` có nhiều `Coach`; `Coach` có nhiều `Seat`.

`Coach`:

| Trường | Ghi chú |
| --- | --- |
| `id`, `trainId` | |
| `coachNumber` | Ví dụ `01` |
| `coachType` | `SEAT` hoặc `SLEEPER` |
| `capacity` | Số chỗ trong toa |
| `priceModifier` | Cộng thêm vào giá cơ bản của chuyến; toa ghế thường là 0 |

`Seat` dùng chung cho cả ghế và giường:

| Trường | Ghi chú |
| --- | --- |
| `id`, `coachId` | |
| `code` | Ví dụ `A01` cho ghế, `B01` cho giường |
| `type` | `SEAT` hoặc `BED` |
| `cabinNumber` | Chỉ có ý nghĩa với toa `SLEEPER`; mỗi cabin 4 giường |

Admin không nhập từng ghế. Khi tạo Coach, hệ thống tự sinh Seat theo `capacity`.

Toa giường chia thành các cabin, mỗi cabin 4 giường. Phiên bản đầu không phân
biệt giường tầng 1, 2, 3, không có VIP, không có phòng riêng, không có nhiều mức
giá phức tạp.

## Trip

| Trường | Ghi chú |
| --- | --- |
| `id`, `trainId`, `routeId` | |
| `departureDate` | |
| `departureTime`, `arrivalTime` | |
| `basePrice` | Giá cơ bản của chuyến |
| `status` | `SCHEDULED`, `COMPLETED`, `CANCELLED` |

MVP không có trạng thái `BOARDING`, `DEPARTED`, `DELAYED`.

Khi tạo Trip, hệ thống sinh `TripSeat` cho toàn bộ `Seat` của Train đó, tất cả ở
trạng thái `AVAILABLE`. Train có 40 ghế thì Trip mới có 40 TripSeat.

## TripSeat

| Trường | Ghi chú |
| --- | --- |
| `id`, `tripId`, `seatId` | Cặp `tripId + seatId` phải là duy nhất |
| `status` | `AVAILABLE`, `HELD`, `BOOKED` |
| `heldBy` | Ai đang giữ chỗ |
| `heldUntil` | Giữ tới thời điểm nào |

Ràng buộc duy nhất trên `tripId + seatId` phải được khai báo ở tầng database, vì
đây là lớp phòng thủ cuối cùng chống việc hai booking cùng chiếm một chỗ.

`SELECTED` chỉ là trạng thái giao diện khi người dùng vừa click, không lưu xuống
database.

## Booking

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `bookingCode` | Mã hiển thị cho người dùng, ví dụ `BK202608001` |
| `customerId`, `tripId` | Một booking chỉ thuộc một Trip |
| `totalPrice` | |
| `bookingStatus` | `PENDING`, `CONFIRMED`, `CANCELLED` |
| `paymentStatus` | `UNPAID`, `PAID` |
| `paidAt`, `createdAt` | |

Không có bảng `Payment` riêng; trạng thái thanh toán lưu ngay trong Booking.

Không có trạng thái `COMPLETED`. Nếu booking `CONFIRMED` và trip `COMPLETED` thì
giao diện tự xếp vào nhóm đã hoàn thành.

## BookingPassenger

| Trường | Ghi chú |
| --- | --- |
| `id`, `bookingId`, `tripSeatId` | Mỗi hành khách gắn đúng một TripSeat |
| `fullName` | |
| `dateOfBirth` | |
| `identityNumber` | CCCD hoặc passport |
| `ticketPrice` | Giá tại thời điểm đặt |

`ticketPrice` phải được lưu lại tại thời điểm đặt. Admin sửa `basePrice` của Trip
về sau không được làm thay đổi giá của booking cũ.

## Ticket

Mỗi hành khách có một vé. Vé chứa: `ticketCode`, `bookingCode`, tên hành khách,
tuyến, tàu, ngày giờ khởi hành, toa, ghế hoặc giường. Vé được xem trực tiếp trên
website.

## Công thức giá

```
Ticket Price = Trip.basePrice + Coach.priceModifier
```

Ví dụ: chuyến có `basePrice` 350.000đ, toa ghế `priceModifier` = 0 thì vé
350.000đ; toa giường `priceModifier` = 200.000 thì vé 550.000đ.

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

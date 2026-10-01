# Mô hình dữ liệu

Nguồn gốc: `SPEC.md` mục 9–23 và 42. Tài liệu này mô tả entity, quan hệ, trạng
thái và ràng buộc mà code phải tuân theo.

Schema thật nằm ở `app/src/main/resources/db/migration/V1__create_schema.sql`
và được Flyway tạo khi ứng dụng khởi động
(`docs/decisions/0005-quan-ly-schema-bang-flyway.md`). Tài liệu này giải thích ý
nghĩa; khi tên cột hay ràng buộc cụ thể khác nhau, file migration là bản đúng.
Entity Java nằm trong `com.voyagego.traingo.<feature>.model`.

Mọi số tiền lưu kiểu `long` (cột `BIGINT`), đơn vị đồng.

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

Chỉ xóa được ga khi không tuyến nào dùng nó (`docs/product/admin.md`).

## Train, Coach, Seat

`Train` có nhiều `Coach`; `Coach` có nhiều `Seat`.

`Train`:

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `code` | Không được trùng, ví dụ `SE1`, `SNT2` |
| `name` | Bắt buộc, ví dụ `Thống Nhất` |

`Coach`:

| Trường | Ghi chú |
| --- | --- |
| `id`, `trainId` | |
| `coachNumber` | Số nguyên 1, 2, 3... trong một tàu, hiển thị `Toa 01`. Không trùng trong cùng tàu |
| `coachType` | `SEAT` hoặc `SLEEPER` |
| `capacity` | Số chỗ trong toa |
| `priceModifier` | Cộng thêm vào giá cơ bản của chuyến; toa ghế thường là 0 |

`Seat` dùng chung cho cả ghế và giường:

| Trường | Ghi chú |
| --- | --- |
| `id`, `coachId` | |
| `code` | Ví dụ `A01` cho ghế, `B01` cho giường. Không trùng trong cùng toa |
| `type` | `SEAT` hoặc `BED` |
| `cabinNumber` | Chỉ có ý nghĩa với toa `SLEEPER`; mỗi cabin 4 giường. Null với ghế |

Admin không nhập từng ghế. Khi tạo Coach, hệ thống tự sinh Seat theo `capacity`:

- Toa `SEAT` capacity 32 sinh `A01` đến `A32`, type `SEAT`.
- Toa `SLEEPER` capacity 16 sinh `B01` đến `B16`, type `BED`,
  cabin = (số thứ tự - 1) / 4 + 1, tức `B01`-`B04` là cabin 1.

Giới hạn khi thêm toa, chốt ngày 01/10/2026 để mã chỗ luôn có 2 chữ số: toa
`SEAT` từ 1 đến 80 chỗ; toa `SLEEPER` từ 4 đến 40 giường và chia hết cho 4.
Phiên bản đầu chỉ thêm toa, chưa sửa hay xóa toa đã tạo, vì đổi `capacity` sau
khi đã có `TripSeat` sẽ làm lệch các chuyến đã sinh chỗ.

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

SPEC chỉ cho Trip một ngày khởi hành và hai giờ, không có ngày đến. Vì vậy
`arrivalTime` nhỏ hơn `departureTime` nghĩa là đến vào ngày hôm sau, và mô hình
này không biểu diễn được chuyến kéo dài từ 24 giờ trở lên. Seed data chỉ dùng
tuyến dưới 24 giờ vì lý do đó.

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

Database cũng từ chối một TripSeat ở trạng thái `HELD` mà thiếu `heldBy` hoặc
`heldUntil`.

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

Database chặn booking `CONFIRMED` mà chưa `PAID`, và booking `PAID` mà thiếu
`paidAt`.

Mã booking trong seed data có dạng `BK` + năm tháng + số thứ tự, ví dụ
`BK202610001`. Khi viết phần sinh mã ở Giai đoạn 6, phải lấy số thứ tự tiếp
theo từ những mã đã có, không đếm lại từ `001`.

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

Một TripSeat chỉ xuất hiện một lần trong cùng một booking, nhưng có thể xuất
hiện ở nhiều booking khác nhau: booking bị hủy thì chỗ được bán lại. Việc chỉ
một booking đã xác nhận giữ chỗ được bảo đảm bằng `TripSeat.status`, không phải
bằng ràng buộc unique trên bảng này.

## Ticket

| Trường | Ghi chú |
| --- | --- |
| `id` | |
| `ticketCode` | Không được trùng. Seed dùng mã booking đổi `BK` thành `TK` rồi thêm thứ tự hành khách, ví dụ `TK202610001-01` |
| `bookingPassengerId` | Mỗi hành khách có đúng một vé |
| `issuedAt` | Lúc booking được thanh toán |

Mỗi hành khách có một vé. Vé hiển thị: `ticketCode`, `bookingCode`, tên hành
khách, tuyến, tàu, ngày giờ khởi hành, toa, ghế hoặc giường. Các thông tin này
đọc qua `BookingPassenger` → `TripSeat`, không chép vào bảng vé. Vé được xem
trực tiếp trên website.

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

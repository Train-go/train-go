# T11 Hành khách và xem lại booking

| Wave | Lane | Cỡ | Phụ thuộc | Mở khóa | Branch |
| --- | --- | --- | --- | --- | --- |
| 3 | B | M | T09, D5, D7 | T12 | `feat/t11-passengers-review` |

## Mục tiêu

Sau khi giữ chỗ (T09), khách nhập thông tin từng hành khách, mỗi người gắn đúng một chỗ
(`SPEC.md` mục 19, 20). Sau đó xem lại toàn bộ booking với giá từng vé và tổng tiền
(mục 24), rồi bấm "Tiếp tục thanh toán". Lúc đó hệ thống tạo booking `PENDING` với giá
đã đóng băng (D7).

## Phạm vi

Trong phạm vi: form hành khách, trang xem lại, tạo booking `PENDING` kèm hành khách, sinh
mã booking (D5).

Ngoài phạm vi: thanh toán (T12), giá theo độ tuổi hay đối tượng (`SPEC.md` mục 19).

## Database

```
users 1 ──< bookings >── 1 trips
               │
               └──< booking_passengers >── 1 trip_seats >── seats >── coaches

bookings:           booking_code UNIQUE, customer_id, trip_id, total_price,
                    booking_status = PENDING, payment_status = UNPAID, created_at
booking_passengers: booking_id, trip_seat_id, full_name, date_of_birth,
                    identity_number, ticket_price
                    UNIQUE (booking_id, trip_seat_id)
```

`ticket_price` = `trips.base_price` + `coaches.price_modifier` **tại lúc tạo booking**.
Admin sửa giá chuyến sau đó không được làm đổi booking cũ (`SPEC.md` mục 21).

## Model

Có sẵn: `Booking` (constructor tạo sẵn `PENDING` / `UNPAID`), `BookingPassenger`.

Mở rộng `booking/model/BookingDraft` của T09:

```java
private List<PassengerDraft> passengers;   // null cho tới khi nhập xong
private Long bookingId;                    // có sau khi tạo booking PENDING
```

Viết mới:

```java
public class PassengerDraft implements Serializable {
    private Long tripSeatId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String identityNumber;
}
```

Form, dùng `@Valid` cho cả danh sách:

```java
public class PassengersForm {
    @Valid
    private List<PassengerForm> passengers = new ArrayList<>();
}

public class PassengerForm {
    @NotNull private Long tripSeatId;
    @NotBlank @Size(max = 100) private String fullName;
    @NotNull @Past @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9]{6,20}",
            message = "Số CCCD hoặc hộ chiếu gồm 6 đến 20 chữ cái hoặc chữ số.")
    private String identityNumber;
}
```

DTO cho trang xem lại:

```java
record ReviewLine(String fullName, int coachNumber, String seatCode,
        long ticketPrice) { }

record BookingReview(String routeLabel, LocalDate departureDate,
        LocalTime departureTime, String trainCode, List<ReviewLine> lines,
        long totalPrice, LocalDateTime heldUntil) { }
```

## Repository

`TripSeatRepository`:

```java
@EntityGraph(attributePaths = {"seat", "seat.coach", "heldBy"})
List<TripSeat> findByIdIn(Collection<Long> ids);
```

`BookingRepository`, cho D5:

```java
Optional<Booking> findTopByBookingCodeStartingWithOrderByBookingCodeDesc(String prefix);
```

## Service

`booking/service/BookingCodeGenerator` (D5), nhận `Clock`:

- `String next()`: tiền tố `"BK" + yyyyMM` của tháng hiện tại; lấy mã lớn nhất có tiền tố
  đó; số thứ tự tiếp theo, đệm 3 chữ số. Ví dụ đã có `BK202610004` thì trả `BK202610005`.

`booking/service/BookingDraftService`, nhận `Clock`:

| Method | Việc |
| --- | --- |
| `void savePassengers(BookingDraft draft, PassengersForm form)` | Số hành khách bằng số chỗ; mỗi `tripSeatId` trong draft xuất hiện đúng một lần; còn hạn giữ chỗ. Sai thì ném `InvalidPassengersException` |
| `BookingReview review(BookingDraft draft)` | Tính giá từng chỗ từ database, không lấy từ form |
| `@Transactional Booking createPending(BookingDraft draft, long userId)` | Xem bên dưới |

`createPending`:

1. Nạp mọi `TripSeat` trong draft bằng `findByIdIn`.
2. Mỗi chỗ phải `HELD`, `heldBy` là người này, `heldUntil` còn sau hiện tại. Sai một chỗ là
   ném `HoldExpiredException`, không tạo gì.
3. Tính `ticketPrice` từng chỗ; `totalPrice` là tổng.
4. `new Booking(codeGenerator.next(), user, trip, totalPrice)` rồi lưu; lưu từng
   `BookingPassenger` theo đúng thứ tự trong draft.
5. Trả booking; controller ghi `bookingId` vào draft.

Hai người tạo booking cùng giây có thể ra cùng một mã; khóa unique `uk_bookings_code`
chặn người thứ hai. Controller bắt `DataIntegrityViolationException` và thử lại một lần.

## Controller

`booking/controller/BookingFlowController`, `@RequestMapping("/booking")`:

| Method | URL | Kết quả |
| --- | --- | --- |
| GET | `/booking/passengers` | View `booking/passengers`, một dòng cho mỗi chỗ (nhãn "Toa 01 · A05") |
| POST | `/booking/passengers` | Lỗi: hiện lại form. Xong: lưu vào draft, `redirect:/booking/review` |
| GET | `/booking/review` | View `booking/review` |
| POST | `/booking/review` | `createPending`, ghi `bookingId`, `redirect:/booking/payment` |

Mọi handler đầu tiên đọc draft bằng `BookingDraftStore`. Không có draft, hoặc đã hết hạn
giữ chỗ: redirect về `/trips/{tripId}` (hoặc `/` nếu không có chuyến) kèm "Hết thời gian
giữ chỗ, vui lòng chọn lại."

T12 sẽ thêm handler `/booking/payment` và `/booking/success` vào controller này. Hai
người thêm hai nhóm method khác nhau, conflict nếu có thì giữ cả hai.

## View

Khung khách, stepper đang ở "Hành khách" rồi "Xem lại":

- `templates/booking/passengers.html`: mỗi hành khách một card: nhãn chỗ, ô họ tên, ngày
  sinh (`type="date"`), CCCD hoặc hộ chiếu. Tên field theo chỉ số:
  `th:field="*{passengers[__${i}__].fullName}"`. Đồng hồ đếm ngược dùng
  `hold-countdown.js` (T09) với `th:data-held-until="${draft.heldUntil}"`.
- `templates/booking/review.html`: tuyến, ngày giờ đi, tàu, bảng hành khách (tên, toa, chỗ,
  giá), tổng tiền; nút "Quay lại" về `/booking/passengers` (draft giữ nguyên dữ liệu đã
  nhập) và nút "Tiếp tục thanh toán" (form `POST`).

## Test bắt buộc

Theo `AGENTS.md`: số hành khách bằng số chỗ, công thức giá và giá được đóng băng.

- Unit test `BookingDraftServiceTest`: 3 chỗ mà 2 hành khách bị từ chối; một `tripSeatId`
  lặp hai lần bị từ chối.
- Unit test giá: chuyến `basePrice` 650.000, toa giường phụ thu 200.000, vé 850.000; tổng
  của 2 vé ghế 350.000 là 700.000.
- Unit test `createPending`: chỗ đã hết hạn giữ thì ném `HoldExpiredException`, không lưu
  booking nào.
- Unit test `BookingCodeGeneratorTest`: chưa có mã nào trong tháng ra `...001`; đã có
  `BK202610004` ra `BK202610005`.

## Xong khi

- [ ] Giữ 2 chỗ (T09), nhập 2 hành khách, xem lại đúng giá và tổng.
- [ ] Bỏ trống họ tên, ngày sinh ở tương lai, CCCD có dấu cách: báo lỗi đúng ô, dữ liệu
      khác vẫn còn.
- [ ] Bấm "Tiếp tục thanh toán": database có 1 booking `PENDING` / `UNPAID` mã
      `BK<năm tháng><số>` và 2 dòng `booking_passengers` với giá đúng.
- [ ] Đợi quá 5 phút rồi bấm: báo hết thời gian, không có booking nào được tạo.
- [ ] Test bắt buộc xanh. Kiểm tra trang ở 375px. Một dòng `docs/WORKLOG.md`.

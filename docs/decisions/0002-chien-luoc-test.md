# 0002 Chiến lược test

Date: 2026-08-23

## Status

Accepted

## Context

Nhóm chưa quen cách test trong Spring Boot. `AGENTS.md` và `docs/WORKFLOW.md` yêu
cầu chỉ được coi là hoàn thành khi có bằng chứng chạy được, không phải khi mô tả
xong. Cần một quy tắc đủ cụ thể để mọi người biết viết loại test nào, đặt ở đâu,
và chạy bằng lệnh gì.

Nghiệp vụ rủi ro nhất của dự án — giữ chỗ, xác nhận booking trong transaction,
phân quyền — là loại lỗi mà đọc code rất khó thấy.

## Decision

**Không tuyên bố hoàn thành khi chưa chạy test.** Trước khi báo xong một phần
việc, phải chạy `.\mvnw.cmd test` và dẫn kết quả thật. Phân biệt rõ: đã sửa code,
đã biên dịch, đã chạy test, test đã xanh — bốn việc khác nhau.

**Test theo tầng**, chọn tầng thấp nhất chứng minh được hành vi:

| Loại | Annotation | Dùng cho | Tốc độ |
| --- | --- | --- | --- |
| Unit | không có, chỉ JUnit + Mockito | Quy tắc nghiệp vụ trong service | mili giây |
| Repository | `@DataJpaTest` | Query tự viết, ràng buộc unique, quan hệ entity | vài trăm ms |
| Controller | `@WebMvcTest` | Routing, validation, phân quyền, status code | vài trăm ms |
| Toàn hệ thống | `@SpringBootTest` | Luồng MVP xuyên suốt | vài giây |

`@SpringBootTest` chỉ dùng cho một đến hai test luồng chính. Viết mọi thứ bằng
`@SpringBootTest` sẽ làm bộ test chậm tới mức không ai muốn chạy.

**Nghiệp vụ bắt buộc phải có test**, không được bỏ qua:

- Giữ chỗ: chuyển `AVAILABLE → HELD`, chặn người thứ hai giữ cùng một chỗ, xử lý
  hết hạn giữ chỗ.
- Xác nhận booking: bốn thay đổi trạng thái phải cùng thành công hoặc cùng thất
  bại; không được tồn tại booking `CONFIRMED` mà chỗ vẫn `AVAILABLE`.
- Hủy booking: trả chỗ về `AVAILABLE`.
- Quy tắc số hành khách bằng số chỗ đã chọn.
- Công thức giá `basePrice + priceModifier` và việc giá được đóng băng trong
  booking.
- Phân quyền: Customer bị chặn khi gõ tay URL `/admin/**`; Customer bị chặn khi
  mở booking của người khác.

CRUD admin đơn giản không bắt buộc có test riêng.

**Bộ test không được phụ thuộc Docker.** Test chạy trên H2 in-memory khai báo
trong `app/src/test/resources/application.properties`.

**Quy ước đặt tên.** Class test kết thúc bằng `Test` để Surefire nhận. Tên method
mô tả hành vi mong đợi, ví dụ `confirmBooking_shouldFail_whenSeatAlreadyBooked`.
File test đặt trong `src/test/java` với đúng cây package của class được test.

## Alternatives Considered

1. **Testcontainers chạy MySQL thật cho integration test.** Chính xác nhất, bắt
   được khác biệt dialect giữa H2 và MySQL. Bị loại vì nặng, chậm, và bắt buộc
   Docker Desktop phải bật mỗi lần chạy test.
2. **Đặt mục tiêu coverage theo phần trăm.** Bị loại vì dễ đẻ ra test vô nghĩa
   cho getter/setter trong khi nghiệp vụ khó vẫn không được kiểm tra.
3. **Chỉ test thủ công qua trình duyệt.** Bị loại vì lỗi transaction và lỗi phân
   quyền gần như không thể phát hiện ổn định bằng thao tác tay.

## Consequences

Positive:

- Chạy test không cần Docker nên không có lý do để bỏ qua.
- Ba nghiệp vụ nguy hiểm nhất được khóa lại bằng test trước khi code lớn dần.
- Tên test mô tả hành vi nên đọc danh sách test là hiểu được quy tắc nghiệp vụ.

Tradeoffs:

- H2 ở chế độ MySQL không phản ánh hết hành vi MySQL thật, đặc biệt về kiểu dữ
  liệu và khóa. Lỗi loại này sẽ chỉ lộ ra khi chạy ứng dụng thật với MySQL.
- Không có watch mode như `vitest --watch`; vòng lặp sửa code rồi chạy lại lệnh
  chậm hơn thói quen bên JavaScript.

## Follow-Up

- Nếu về sau gặp lỗi chỉ xuất hiện trên MySQL, xem xét bổ sung Testcontainers cho
  riêng nhóm test repository.
- Bổ sung một test `@SpringBootTest` chạy trọn luồng MVP ngay khi luồng đặt vé
  hoàn chỉnh.

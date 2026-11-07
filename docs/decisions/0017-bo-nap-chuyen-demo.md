# 0017 Bộ nạp chuyến demo chạy lúc khởi động

Date: 2026-11-07

## Status

Accepted. Phi chọn hướng này ngày 07/11/2026 thay cho migration Flyway nạp chuyến theo ngày.

## Context

Seed chuyến của `V5__north_south_line.sql` tính ngày theo `CURRENT_DATE` tại lúc Flyway chạy. Một
database tạo ngày 22/10 có chuyến từ 18/10 đến 07/11; sang 07/11 chỉ còn một chuyến đã hủy sau thời điểm
hiện tại, nên trang tìm chuyến không hiện gì. Migration chỉ chạy một lần trên mỗi database, nên thêm một
migration cùng kiểu chỉ hoãn lỗi này một tuần.

## Decision

- `DemoTripSeeder` (`service/`) là một `ApplicationRunner`. Mỗi lần khởi động nó bổ sung chuyến cho 14 ngày
  tính từ hôm nay, bỏ qua giờ đi đã qua và chuyến đã có (cùng tàu, tuyến, giờ đi), nên chạy lại không tạo
  trùng.
- Ba tàu SE1, SE2, SE3 mỗi ngày chạy khứ hồi một cặp ga: Hà Nội - Thanh Hóa (06:00, 14:00), Huế - Đà Nẵng
  (08:00, 15:00), Nha Trang - Sài Gòn (07:00, 16:00). Thiếu tàu (hoặc tàu đã xóa mềm) hoặc thiếu tuyến thì bỏ
  qua tàu đó.
- Giờ chạy và giá theo công thức của V5: 1 phút mỗi km làm tròn 5 phút, khoảng 1.000 đồng mỗi km làm tròn
  10.000 đồng. Mỗi chuyến có đủ ghế theo `CoachLayout`, tất cả còn trống.
- Chuyến được lưu thẳng bằng `TripRepository` và `TripSeatRepository`, không qua `TripCreationService`: dịch
  vụ đó nối chặng từ ga cuối của tàu và đòi giờ đi ở tương lai, còn bộ nạp đặt từng ngày độc lập.
- Bật mặc định. `traingo.demo-trips.enabled=false` tắt hẳn; môi trường không phải máy local phải tắt.

## Consequences

- Ai chạy dự án cũng luôn có chuyến trong hai tuần tới, không phải seed tay.
- Dữ liệu chuyến demo không còn nằm trong migration; migration vẫn chỉ giữ schema và seed cố định.
- Chuyến cuối của mỗi tàu luôn kết thúc ở ga A của cặp; admin thêm chặng cho tàu đó sẽ nối từ ga này.
- Booking không được nạp. Muốn dữ liệu booking cho chuyến mới thì làm riêng.

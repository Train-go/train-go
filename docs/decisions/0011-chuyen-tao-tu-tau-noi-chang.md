# 0011 Một trục Bắc - Nam, chuyến tạo từ tàu và nối chặng

Date: 2026-10-20

## Status

Accepted. Điều chỉnh `SPEC.md` mục 9 (Station) và 11.2 (Trip). Thay cách tạo
chuyến trong `docs/product/admin.md` và hướng dẫn T07. Dữ liệu mẫu của `V2` và `V3`
được thay toàn bộ.

## Context

Dữ liệu cũ có 8 tàu, mỗi tàu chạy cố định một cặp ga, và ga nằm rải rác (có cả
Lào Cai trên nhánh lên biên giới). Phi thấy cách nghĩ "chuyến đi theo tuyến" này sai
với lý thuyết. SPEC mục 11.2 định nghĩa Trip là "một lần chạy cụ thể của Train",
và mục 38 tạo Trip "dựa trên Train, Route...". Cách nghĩ cũ làm layout admin không
dựng được.

Phi chốt ngày 20/10/2026:

- Ga và tuyến là dữ liệu nền, xác định trước: 8 ga trên trục Bắc - Nam, không lấy
  vùng biên.
- Chuyến được tạo từ tàu. Tàu chạy nối chặng: chuyến sau xuất phát từ ga mà chuyến
  trước vừa đến.
- Vẫn giữ một chuyến chỉ có 2 ga như SPEC mục 10 và 35. Không làm ga trung gian,
  không bán vé theo đoạn.
- Tàu không gắn với tuyến nào.

## Decision

- **Lý trình ga.** `stations.km_mark` là số km tính từ ga Hà Nội. Cột bắt buộc,
  không trùng, không âm. Mọi danh sách ga xếp theo lý trình, tức Bắc → Nam. Danh
  sách tuyến xếp theo lý trình của ga đi rồi ga đến.
- **Giờ chuyến kiểu ngày-giờ.** `trips.departure_at` và `trips.arrival_at`
  (`DATETIME`) thay cho `departure_date`, `departure_time`, `arrival_time`. Database
  chặn `arrival_at <= departure_at`. Chuyến từ 24 giờ trở lên (Hà Nội - Sài Gòn
  khoảng 29 giờ) lưu được. Bỏ quy ước "giờ đến nhỏ hơn giờ đi là hôm sau".
- **Chuyến thuộc về tàu.** Admin thêm chuyến ở trang chi tiết tàu. Luật nối chặng
  do service kiểm tra, không tin dữ liệu form:
  1. Tàu chưa có chuyến nào (không tính chuyến đã hủy): chọn tuyến bất kỳ.
  2. Tàu đã có chuyến: chuyến mới nối vào **cuối** lịch. Ga đi bị khóa bằng ga
     đến của chuyến cuối cùng chưa hủy, nên chỉ chọn được tuyến xuất phát từ ga đó.
  3. Giờ đi phải sau giờ đến của chuyến cuối ít nhất 30 phút (thời gian quay đầu,
     hằng số trong code).
  4. Vì chỉ nối vào cuối, một tàu không bao giờ có hai chuyến chồng giờ.
- **Hủy chuyến** giữ luật D2. Hủy một chuyến ở giữa làm chuỗi bị đứt. Trang tàu
  đánh dấu "chuỗi bị đứt" ở chỗ đó để admin biết, nhưng không chặn thao tác nào.
  Luật nối chặng chỉ áp cho chuyến tạo mới.
- **Giữ nguyên:** tuyến là một cặp ga, chiều ngược lại là tuyến khác; 1 booking = 1
  chuyến; giá vé = `basePrice + priceModifier`; 4 toa, 96 chỗ.
- **Dữ liệu mẫu** (migration `V5__north_south_line.sql`): 8 ga HNI, THA, VIN, DHO,
  HUE, DNG, NTR, SGN (lý trình 0 đến 1726 km); 16 tuyến (7 chặng kề nhau mỗi chiều,
  cộng HNI ↔ SGN); 3 tàu SE1, SE2, SE3. Chỉ SE1 có chuyến: mỗi ngày một chặng, khởi
  hành 08:00, 7 ngày xuống Nam rồi 7 ngày lên Bắc. V5 xóa toàn bộ booking, vé,
  chuyến, tuyến, tàu và ga cũ; giữ người dùng.

## Alternatives Considered

1. **Một chuyến đi suốt, có ga dừng và bán vé theo đoạn.** Giống tàu thật nhất.
   Nhưng phải thêm bảng ga dừng, tính chỗ trống theo đoạn, đổi tìm chuyến, tính giá
   và khóa chỗ. SPEC mục 10, 35, 42, 48 và 54 loại hẳn ga trung gian. Không chọn.
2. **Nối chặng chỉ là gợi ý.** Form điền sẵn nhưng admin được đổi ga đi. Linh hoạt
   hơn, nhưng tạo được tàu "dịch chuyển" giữa hai ga không có chuyến nối. Không chọn.
3. **Giữ ngày đi và hai giờ như SPEC.** Không lưu được chuyến Hà Nội - Sài Gòn.
   Không chọn.

## Consequences

Positive:

- Màn hình admin có trục rõ ràng: ga và tuyến xếp theo lý trình, mỗi tàu có một
  dòng thời gian liền mạch.
- Tạo chuyến khó sai hơn: ga đi và giờ sớm nhất điền sẵn từ chuyến trước.
- Truy vấn theo giờ đơn giản hơn (một cột thay vì ngày cộng giờ).

Negative:

- Lệch `SPEC.md` mục 9 (thêm `kmMark`) và mục 11.2 (ngày-giờ thay cho ngày đi và
  hai giờ). Nhóm nên báo giảng viên hoặc ghi chú khi nộp.
- Khách chỉ mua được vé cho cặp ga có tuyến. Không có vé nối chuyến (SPEC mục
  32.1). Ví dụ: không có tuyến Hà Nội → Huế thì không đặt được Hà Nội → Huế, dù SE1
  có chạy qua đó bằng nhiều chặng.
- `V5` xóa dữ liệu demo cũ trên mọi máy chạy nó. Muốn ngày chuyến mới thì reset
  database như cũ (`docs/RUNBOOK.md`).

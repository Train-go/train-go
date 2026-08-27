# TrainGo — Tài liệu phân tích nghiệp vụ

**Phiên bản 1.0 · Ngày 24/08/2026**

Tài liệu này mô tả toàn bộ dự án TrainGo theo trình tự làm việc của một nhóm
phân tích nghiệp vụ: từ bản đồ tài liệu, tới dữ liệu và quan hệ giữa các bảng,
rồi tới đặc tả từng tính năng, thiết kế màn hình và kế hoạch kiểm chứng.

**Đây là bản tổng hợp, không phải bản gốc.** Khi tài liệu này mâu thuẫn với
`SPEC.md` hoặc `docs/product/`, hai nguồn đó đúng và tài liệu này sai. Sửa bản
gốc trước, rồi chạy lại lệnh ở mục [Cách bảo trì tài liệu](#04-cách-bảo-trì-tài-liệu).

---

## Mục lục

**Phần I — Bối cảnh**

- [0.1 Mục đích và đối tượng đọc](#01-mục-đích-và-đối-tượng-đọc)
- [0.2 Quy ước đánh mã](#02-quy-ước-đánh-mã)
- [0.3 Bản đồ tài liệu dự án](#03-bản-đồ-tài-liệu-dự-án)
- [0.4 Cách bảo trì tài liệu](#04-cách-bảo-trì-tài-liệu)
- [1. Tổng quan sản phẩm](#1-tổng-quan-sản-phẩm)
- [2. Các bên liên quan và actor](#2-các-bên-liên-quan-và-actor)
- [3. Thuật ngữ](#3-thuật-ngữ)

**Phần II — Quy trình nghiệp vụ**

- [4. Bản đồ quy trình tổng thể](#4-bản-đồ-quy-trình-tổng-thể)
- [5. Quy trình Admin chuẩn bị dữ liệu](#5-quy-trình-admin-chuẩn-bị-dữ-liệu)
- [6. Quy trình Customer đặt vé](#6-quy-trình-customer-đặt-vé)
- [7. Quy trình hủy booking](#7-quy-trình-hủy-booking)

**Phần III — Dữ liệu**

- [8. Mô hình dữ liệu tổng quan](#8-mô-hình-dữ-liệu-tổng-quan)
- [9. Sơ đồ quan hệ thực thể](#9-sơ-đồ-quan-hệ-thực-thể)
- [10. Từ điển dữ liệu](#10-từ-điển-dữ-liệu)
- [11. Ma trận quan hệ giữa các bảng](#11-ma-trận-quan-hệ-giữa-các-bảng)
- [12. Vòng đời trạng thái](#12-vòng-đời-trạng-thái)
- [13. Ràng buộc toàn vẹn dữ liệu](#13-ràng-buộc-toàn-vẹn-dữ-liệu)

**Phần IV — Yêu cầu và tính năng**

- [14. Yêu cầu chức năng](#14-yêu-cầu-chức-năng)
- [15. Yêu cầu phi chức năng](#15-yêu-cầu-phi-chức-năng)
- [16. Quy tắc nghiệp vụ](#16-quy-tắc-nghiệp-vụ)
- [17. Danh mục use case](#17-danh-mục-use-case)
- [18. Đặc tả use case nhóm Xác thực](#18-đặc-tả-use-case-nhóm-xác-thực)
- [19. Đặc tả use case nhóm Đặt vé](#19-đặc-tả-use-case-nhóm-đặt-vé)
- [20. Đặc tả use case nhóm Quản lý booking của Customer](#20-đặc-tả-use-case-nhóm-quản-lý-booking-của-customer)
- [21. Đặc tả use case nhóm Quản trị](#21-đặc-tả-use-case-nhóm-quản-trị)

**Phần V — Thiết kế giao diện**

- [22. Hệ thống giao diện](#22-hệ-thống-giao-diện)
- [23. Đặc tả màn hình](#23-đặc-tả-màn-hình)

**Phần VI — Xây dựng và kiểm chứng**

- [24. Kiến trúc kỹ thuật](#24-kiến-trúc-kỹ-thuật)
- [25. Kế hoạch xây dựng](#25-kế-hoạch-xây-dựng)
- [26. Chiến lược kiểm thử](#26-chiến-lược-kiểm-thử)
- [27. Ma trận truy vết](#27-ma-trận-truy-vết)
- [28. Rủi ro và điểm chưa chốt](#28-rủi-ro-và-điểm-chưa-chốt)

---

# Phần I — Bối cảnh

## 0.1 Mục đích và đối tượng đọc

Tài liệu phục vụ ba nhóm người, mỗi nhóm đọc một phần khác nhau:

| Người đọc | Cần gì | Đọc mục |
| --- | --- | --- |
| Thành viên nhóm nhận việc mới | Hiểu nghiệp vụ trước khi code | 1, 6, 16, và use case của phần mình làm |
| Người review hoặc chấm bài | Nắm phạm vi và mức hoàn thành | 1, 14, 25, 27 |
| Người quay lại sau vài tuần | Nhớ lại vì sao làm như vậy | 3, 12, 13, 28 |

Tài liệu **không** thay thế code. Khi cần biết hệ thống thực sự làm gì, đọc code
và test; tài liệu chỉ nói hệ thống *phải* làm gì.

## 0.2 Quy ước đánh mã

Mọi thứ có thể tham chiếu chéo đều được đánh mã, để ma trận truy vết ở mục 27
nối được các phần với nhau:

| Tiền tố | Nghĩa | Ví dụ |
| --- | --- | --- |
| `EN-` | Thực thể dữ liệu | `EN-08` TripSeat |
| `FR-` | Yêu cầu chức năng | `FR-12` Giữ chỗ tạm thời |
| `NFR-` | Yêu cầu phi chức năng | `NFR-03` Bảo mật |
| `BR-` | Quy tắc nghiệp vụ | `BR-07` Một chỗ chỉ thuộc một booking |
| `UC-` | Use case | `UC-08` Chọn chỗ và giữ chỗ |
| `SC-` | Màn hình | `SC-06` Chọn toa và chỗ |

Trong đặc tả use case, luồng thay thế đánh `A1`, `A2`; luồng ngoại lệ đánh `E1`,
`E2`. Số bước trong luồng chính là số nguyên; bước rẽ nhánh ghi rõ quay về bước
nào.

## 0.3 Bản đồ tài liệu dự án

Repository chứa nhiều loại tài liệu với thẩm quyền khác nhau. Thứ tự dưới đây là
thứ tự thẩm quyền: khi hai file mâu thuẫn, file ở trên đúng.

| Thứ tự | File | Vai trò | Ai sửa |
| --- | --- | --- | --- |
| 1 | `SPEC.md` | Yêu cầu gốc của môn học. Bản gốc tuyệt đối | Không sửa trừ khi đề bài đổi |
| 2 | `docs/decisions/` | Quyết định kỹ thuật đã chốt, mọi việc sau phải tuân theo | Thêm ADR mới, không sửa ADR cũ |
| 3 | `docs/product/` | Bản chắt lọc của spec để làm việc hằng ngày | Sửa khi hiểu rõ hơn về nghiệp vụ |
| 4 | `docs/plans/active/` | Việc đang làm, tiến độ, quyết định tạm thời | Cập nhật liên tục khi làm |
| 5 | `docs/RUNBOOK.md` | Cách chạy ứng dụng, database, test | Cập nhật khi đã kiểm chứng thật |
| 6 | `docs/session/` | Bản nháp và tài liệu tổng hợp — kể cả file này | Sinh lại từ nguồn ở trên |

Chi tiết từng thư mục:

**`docs/product/`** — năm file, mỗi file một mặt của sản phẩm:

| File | Nội dung |
| --- | --- |
| `overview.md` | Phạm vi, người dùng, luồng chính, danh sách màn hình |
| `domain-model.md` | Entity, quan hệ, trạng thái, ràng buộc dữ liệu |
| `booking-flow.md` | Giữ chỗ, thanh toán, transaction, hủy vé |
| `admin.md` | Chức năng quản trị và quy tắc dữ liệu vận hành |
| `auth.md` | Đăng ký, đăng nhập, phân quyền, quy tắc bảo mật |

**`docs/decisions/`** — ba quyết định đã chốt:

| ADR | Chốt điều gì |
| --- | --- |
| `0001-cau-truc-du-an-va-tech-stack.md` | Code nằm trong `app/`, package theo feature, Spring Boot 4.1.1, MySQL bằng Docker |
| `0002-chien-luoc-test.md` | Bốn tầng test, nghiệp vụ bắt buộc có test, H2 cho test |
| `0003-he-thong-giao-dien.md` | Tên TrainGo, Bootstrap phục vụ từ ứng dụng, layout fragment, `line-height` cho tiếng Việt |

**`docs/plans/`** — bộ nhớ làm việc. `active/` chứa kế hoạch đang chạy, chuyển
sang `completed/` sau khi kết quả đã được kiểm chứng.

**`docs/session/`** — nơi này. Chứa bản nháp plan và tài liệu tổng hợp. Quy trình
mô tả trong `docs/session/README.md`.

**`AGENTS.md`** và **`docs/WORKFLOW.md`** — quy tắc làm việc: khi nào cần plan,
khi nào phải dừng lại hỏi, và điều kiện để được nói "đã xong".

## 0.4 Cách bảo trì tài liệu

Tài liệu này có hai bản: Markdown để đọc và sửa, HTML để xem và in.

```powershell
# Sau khi sửa tai-lieu-phan-tich-traingo.md
python docs/session/render-doc.py
```

Lệnh sinh lại `tai-lieu-phan-tich-traingo.html` từ Markdown. **Không sửa tay file
HTML** — mọi thay đổi sẽ mất ở lần sinh sau.

Bản HTML tự chứa hoàn toàn: không gọi CDN, không cần mạng, mở trực tiếp bằng
trình duyệt từ ổ đĩa. Bấm `Ctrl+P` được bản in đã ẩn mục lục và ngắt trang theo
chương.

Khi nội dung gốc trong `docs/product/` hoặc `SPEC.md` thay đổi, phải sửa tài liệu
này cho khớp. Đây là chi phí đã biết của việc gom tất cả vào một chỗ.

---

## 1. Tổng quan sản phẩm

### 1.1 Bối cảnh

Đặt vé tàu ở Việt Nam hiện đòi hỏi người dùng hoặc ra ga, hoặc dùng website với
luồng đặt vé rườm rà và sơ đồ chỗ khó hiểu. TrainGo mô phỏng lại quy trình đó ở
phạm vi vừa đủ cho một đồ án môn học, tập trung vào phần khó nhất và cũng là phần
người dùng quan tâm nhất: **chọn đúng chỗ mình muốn, và chắc chắn chỗ đó là của
mình**.

### 1.2 Mục tiêu

| Mã | Mục tiêu | Đo bằng gì |
| --- | --- | --- |
| G-01 | Customer đặt được vé từ đầu đến cuối không cần trợ giúp | Chạy trọn luồng trên trình duyệt với MySQL thật |
| G-02 | Không bao giờ có hai người cùng một chỗ trên một chuyến | Ràng buộc unique ở database + test cho nhánh thất bại |
| G-03 | Admin chuẩn bị đủ dữ liệu vận hành để chuyến có thể bán vé | Tạo được Station → Route → Train → Coach → Trip |
| G-04 | Dữ liệu không bao giờ ở trạng thái nửa vời | Transaction bao trọn bốn thay đổi khi xác nhận booking |
| G-05 | Dùng được trên điện thoại | Kiểm tra ở 375px, 768px, 1280px |

### 1.3 Sản phẩm là gì

TrainGo là website đặt vé tàu: tìm chuyến, chọn toa, chọn ghế hoặc giường trên sơ
đồ trực quan, nhập thông tin hành khách, thanh toán mô phỏng, nhận vé điện tử.

Hệ thống **không** mô phỏng toàn bộ hoạt động ngành đường sắt. Mọi thứ không phục
vụ trực tiếp cho việc đặt vé đều nằm ngoài phạm vi.

### 1.4 Phạm vi

**Trong phạm vi MVP:**

Xác thực, phân quyền, quản lý ga / tuyến / tàu / toa / chuyến, sinh ghế tự động,
`TripSeat`, tìm chuyến, chọn toa và chỗ trực quan, giữ chỗ, thông tin hành khách,
review, thanh toán mô phỏng, vé điện tử, My Trips, hủy booking.

**Optional — chỉ làm sau khi MVP ổn định:**

QR code trên vé, quét QR, bản đồ đường sắt (Leaflet + OpenStreetMap), xuất vé
PDF. Thứ tự ưu tiên: vé trên web → QR code → PDF. Không bắt đầu trước khi Giai
đoạn 7 xong.

Nếu làm QR, mã chỉ chứa `ticketCode` hoặc `ticketId`, **tuyệt đối không** chứa
CCCD, passport, thông tin thanh toán hay thông tin cá nhân.

**Ngoài phạm vi:**

Staff và cổng nhân viên, kiểm vé, check-in, thanh toán ngân hàng thật, payment
gateway, hoàn tiền thật, ga trung gian, `RouteStation`, `TripStation`, GPS, theo
dõi tàu real-time, multi-train hoặc round-trip trong một booking, dynamic
pricing, giá theo độ tuổi hoặc đối tượng, loyalty, voucher, khuyến mãi, đặt đồ
ăn, thông báo email và SMS, hóa đơn điện tử, kế toán, analytics nâng cao, filter
tìm kiếm nâng cao, OAuth, quên mật khẩu, đặt lại mật khẩu.

### 1.5 Giả định và phụ thuộc

| Loại | Nội dung |
| --- | --- |
| Giả định | Mỗi chuyến chỉ chạy giữa hai ga, không dừng ở ga trung gian |
| Giả định | Người dùng có tài khoản trước khi đặt vé; không có đặt vé cho khách vãng lai |
| Giả định | Một booking chỉ cho một chiều, một chuyến |
| Phụ thuộc | MySQL 8.4 chạy bằng Docker ở môi trường dev |
| Phụ thuộc | Bootstrap 5.3.3 đã được tải về repository, không phụ thuộc CDN lúc chạy |
| Ràng buộc | Nhóm ba người, mới với Java và Spring Boot |
| Ràng buộc | Giao diện và tài liệu tiếng Việt; định danh trong code tiếng Anh |

---

## 2. Các bên liên quan và actor

### 2.1 Các bên liên quan

| Bên liên quan | Quan tâm điều gì |
| --- | --- |
| Giảng viên | Đủ phạm vi đề bài, có bằng chứng chạy được, tài liệu rõ ràng |
| Ba thành viên nhóm | Chia việc không đụng nhau, hiểu được phần người khác viết |
| Người dùng cuối giả định | Đặt được vé nhanh, thấy rõ chỗ mình chọn |

### 2.2 Actor của hệ thống

| Actor | Là ai | Quyền |
| --- | --- | --- |
| **Khách** | Người chưa đăng nhập | Xem landing, tìm chuyến, xem danh sách và chi tiết chuyến, đăng ký, đăng nhập |
| **CUSTOMER** | Người đặt vé, đã đăng nhập | Toàn bộ luồng đặt vé, My Trips, vé điện tử, hủy booking **của chính mình** |
| **ADMIN** | Người quản lý dữ liệu vận hành | Dashboard, quản lý ga, tuyến, tàu, toa, chuyến, booking, xem danh sách tài khoản |
| **Hệ thống** | Tác nhân tự động | Sinh Seat khi tạo Coach, sinh TripSeat khi tạo Trip, sinh Ticket khi xác nhận booking, thu hồi chỗ hết hạn giữ |

Không có role `STAFF`. Không có luồng kiểm vé hay check-in hành khách.

### 2.3 Ma trận quyền

| Chức năng | Khách | CUSTOMER | ADMIN |
| --- | --- | --- | --- |
| Xem landing, tìm chuyến | ✅ | ✅ | ✅ |
| Xem danh sách và chi tiết chuyến | ✅ | ✅ | ✅ |
| Đăng ký, đăng nhập | ✅ | — | — |
| Chọn chỗ, giữ chỗ | ❌ | ✅ | ✅ |
| Tạo booking, thanh toán | ❌ | ✅ | ✅ |
| Xem vé của mình | ❌ | ✅ | — |
| Xem booking của người khác | ❌ | ❌ | ✅ |
| Hủy booking của mình | ❌ | ✅ | ✅ |
| Quản lý ga, tuyến, tàu, toa, chuyến | ❌ | ❌ | ✅ |
| Xem danh sách tài khoản | ❌ | ❌ | ✅ |

Ô ❌ nghĩa là backend phải chặn, không phải chỉ ẩn nút trên giao diện. Xem
[BR-30](#16-quy-tắc-nghiệp-vụ) và [BR-31](#16-quy-tắc-nghiệp-vụ).

---

## 3. Thuật ngữ

| Thuật ngữ | Nghĩa trong dự án này |
| --- | --- |
| **Train** | Đoàn tàu vật lý, gồm các toa và ghế. Dùng lại cho nhiều chuyến |
| **Trip** | Một lần chạy cụ thể của một Train trên một Route vào một ngày |
| **Coach** | Toa tàu. Có loại `SEAT` hoặc `SLEEPER` |
| **Seat** | Một chỗ vật lý trong toa. Dùng chung cho cả ghế và giường |
| **TripSeat** | Trạng thái của một chỗ **trong một chuyến cụ thể**. Đây là nơi lưu chỗ đã bán hay chưa |
| **Booking** | Một lần đặt vé, thuộc một Customer và một Trip, có thể gồm nhiều hành khách |
| **BookingPassenger** | Một hành khách trong booking, gắn với đúng một TripSeat |
| **Ticket** | Vé điện tử của một hành khách |
| **Giữ chỗ (seat holding)** | Chuyển chỗ sang trạng thái `HELD` trong 5 phút để người dùng hoàn tất booking |
| **Thanh toán mô phỏng** | Đổi trạng thái thanh toán mà không có giao dịch tiền thật |
| **Ga trung gian** | Ga nằm giữa ga đi và ga đến. **Ngoài phạm vi** dự án |

### Điều quan trọng nhất phải hiểu đúng

**Train là cấu hình vật lý, Trip là một lần chạy.**

Một đoàn tàu chạy nhiều chuyến khác nhau vào những ngày khác nhau. Ghế `A01` của
tàu SE2 có thể trống ở chuyến ngày 24/08 nhưng đã bán ở chuyến ngày 25/08.

Vì vậy trạng thái đặt chỗ **không thể** lưu trên `Seat` — nó phải lưu trên
`TripSeat`, tức là chỗ đó *trong chuyến đó*.

Hiểu sai điểm này thì toàn bộ phần đặt chỗ sẽ sai, và lỗi chỉ lộ ra khi hai người
cùng đặt một ghế ở hai chuyến khác nhau của cùng một tàu.

---

# Phần II — Quy trình nghiệp vụ

## 4. Bản đồ quy trình tổng thể

Hệ thống có ba quy trình lớn, chạy theo thứ tự phụ thuộc dữ liệu:

```
┌─────────────────────────────────────────────────────────────┐
│ QUY TRÌNH 1 — ADMIN CHUẨN BỊ DỮ LIỆU                        │
│ Station -> Route -> Train -> Coach (+Seat) -> Trip (+TripSeat)│
│ Kết quả: có chuyến bán được vé                              │
└──────────────────────────┬──────────────────────────────────┘
                           │ điều kiện cần
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ QUY TRÌNH 2 — CUSTOMER ĐẶT VÉ                               │
│ Tìm -> Chọn chuyến -> Chọn toa -> Chọn chỗ -> Giữ chỗ       │
│   -> Hành khách -> Review -> Thanh toán -> Vé               │
│ Kết quả: Booking CONFIRMED, TripSeat BOOKED, có Ticket      │
└──────────────────────────┬──────────────────────────────────┘
                           │ sau khi đã có booking
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ QUY TRÌNH 3 — HỦY BOOKING                                   │
│ Kiểm tra điều kiện -> Đổi trạng thái -> Trả chỗ về AVAILABLE │
│ Kết quả: Booking CANCELLED, TripSeat AVAILABLE              │
└─────────────────────────────────────────────────────────────┘
```

Quy trình 2 là **quan trọng nhất của toàn dự án**. Mọi tính năng khác chỉ được
làm sau khi quy trình này chạy thông từ đầu đến cuối.

---

## 5. Quy trình Admin chuẩn bị dữ liệu

### 5.1 Sơ đồ

```
[ADMIN]
   │
   ├─ 1. Tạo Station ────────────────► EN-02 Station
   │      Bắt buộc: name, code (không trùng)
   │
   ├─ 2. Tạo Route ──────────────────► EN-03 Route
   │      Cần: 2 Station đã có
   │      Ràng buộc: ga đi ≠ ga đến  (BR-02)
   │
   ├─ 3. Tạo Train ──────────────────► EN-04 Train
   │      Chưa cần Route
   │
   ├─ 4. Cấu hình Coach ─────────────► EN-05 Coach
   │      Cần: Train đã có
   │      Nhập: coachType, capacity, priceModifier
   │         │
   │         └─[HỆ THỐNG] sinh Seat ─► EN-06 Seat  (BR-04)
   │              capacity=16, SEAT  -> A01..A16
   │              capacity=16, SLEEPER -> B01..B16, 4 cabin
   │
   └─ 5. Tạo Trip ───────────────────► EN-07 Trip
          Cần: Train (đã có Coach + Seat) và Route
          Nhập: departureDate, departureTime, arrivalTime, basePrice
             │
             └─[HỆ THỐNG] sinh TripSeat ─► EN-08 TripSeat  (BR-05)
                  Mỗi Seat của Train -> 1 TripSeat, status = AVAILABLE
                  Train 40 ghế -> Trip mới có 40 TripSeat
```

### 5.2 Vì sao thứ tự này bắt buộc

Mỗi bước cần kết quả của bước trước:

| Bước | Không có bước trước thì | Hệ thống phải |
| --- | --- | --- |
| Tạo Route | Không có ga để chọn | Chặn tạo, chỉ sang trang quản lý ga |
| Cấu hình Coach | Không biết toa thuộc tàu nào | Chỉ vào được từ trang chi tiết tàu |
| Tạo Trip | Tàu không có ghế nào, chuyến sinh ra 0 TripSeat | Cảnh báo tàu chưa có toa |

Sai thứ tự không làm hệ thống sập, nhưng tạo ra chuyến không bán được vé — lỗi
này rất khó thấy vì mọi thứ trông vẫn bình thường.

### 5.3 Hai bước tự động của hệ thống

Đây là hai chỗ hệ thống sinh dữ liệu thay người dùng, và cũng là hai chỗ hay bị
hiểu nhầm nhất:

**Sinh Seat khi tạo Coach.** Admin nhập `capacity = 16`, hệ thống tạo 16 bản ghi
`Seat` với mã `A01` đến `A16`. Admin **không nhập tay từng ghế**. Với toa
`SLEEPER`, hệ thống chia thành các cabin 4 giường và gán `cabinNumber`.

**Sinh TripSeat khi tạo Trip.** Hệ thống duyệt toàn bộ `Seat` của `Train` được
chọn và tạo một `TripSeat` cho mỗi ghế, tất cả ở trạng thái `AVAILABLE`.

Sau khi tạo, giao diện phải báo rõ đã sinh bao nhiêu bản ghi. Không báo thì Admin
không biết bước tự động có chạy đúng không.

---

## 6. Quy trình Customer đặt vé

### 6.1 Sơ đồ luồng chính

```
[KHÁCH / CUSTOMER]
   │
   ▼
┌──────────────────┐
│ Nhập điều kiện   │  ga đi, ga đến, ngày, số khách
│ tìm chuyến       │  Ràng buộc: BR-10, BR-11, BR-12
└────────┬─────────┘
         ▼
┌──────────────────┐
│ Danh sách chuyến │  hiển thị: mã tàu, giờ, thời gian, giá thấp nhất,
│                  │  số chỗ còn lại
└────────┬─────────┘
         ▼
┌──────────────────┐
│ Chi tiết chuyến  │  danh sách toa, số chỗ còn, giá theo toa
└────────┬─────────┘
         │  ── cần đăng nhập từ đây ──►  [chưa đăng nhập] → /login
         ▼
┌──────────────────┐
│ Chọn toa và chỗ  │  sơ đồ trực quan
└────────┬─────────┘
         ▼
┌──────────────────┐
│ GIỮ CHỖ 5 PHÚT   │  TripSeat: AVAILABLE -> HELD
│ ⏱ đếm ngược      │  ghi heldBy, heldUntil        (BR-13, BR-14)
└────────┬─────────┘
         ▼
┌──────────────────┐
│ Nhập hành khách  │  mỗi hành khách 1 chỗ         (BR-16, BR-17)
└────────┬─────────┘
         ▼
┌──────────────────┐
│ Review           │  giá từng vé + tổng tiền      (BR-19)
└────────┬─────────┘
         ▼
┌══════════════════┐
║ THANH TOÁN       ║  ┌─ TRANSACTION ─────────────────────┐
║ (mô phỏng)       ║  │ paymentStatus  UNPAID  -> PAID    │
║                  ║  │ bookingStatus  PENDING -> CONFIRMED│
║                  ║  │ TripSeat       HELD    -> BOOKED  │
║                  ║  │ sinh Ticket cho từng hành khách   │
║                  ║  └───────────────────────────────────┘
└────────┬═════════┘     Bốn việc: cùng thành công hoặc cùng thất bại (BR-20)
         ▼
┌──────────────────┐
│ Booking thành    │  hiển thị bookingCode
│ công             │
└────────┬─────────┘
         ▼
┌──────────────────┐
│ Vé điện tử       │  một vé cho mỗi hành khách
└──────────────────┘
```

Stepper hiển thị trên giao diện: `Trip → Seat → Passenger → Review → Payment → Ticket`.

### 6.2 Điểm cần đăng nhập

Khách chưa đăng nhập vẫn tìm và xem chuyến được. Hệ thống chỉ yêu cầu đăng nhập
khi bắt đầu **chọn chỗ**, vì từ bước đó trở đi mọi thao tác đều gắn với một người
cụ thể: `heldBy` của TripSeat, `customerId` của Booking.

Sau khi đăng nhập xong, hệ thống phải đưa người dùng **quay lại đúng chuyến họ
đang xem**, không đá về trang chủ.

### 6.3 Đồng hồ giữ chỗ

Đồng hồ bắt đầu chạy khi chỗ đầu tiên được giữ, và tiếp tục chạy qua ba màn hình:
chọn chỗ, nhập hành khách, review. Nó chỉ dừng khi thanh toán thành công hoặc hết
giờ.

Mỗi màn hình trong ba màn hình đó phải hiển thị thời gian còn lại. Người dùng
không được rơi vào tình huống bấm thanh toán rồi mới biết chỗ đã mất.

### 6.4 Điều gì xảy ra khi hết giờ

```
TripSeat: HELD ─────────────► AVAILABLE
Booking:  PENDING ──────────► giữ nguyên PENDING hoặc bị bỏ
Người dùng: bị đưa về bước chọn chỗ, kèm thông báo giải thích
```

> **Chưa quyết:** chỗ hết hạn `HELD` được trả về `AVAILABLE` bằng cách nào —
> kiểm tra lười khi có người đọc lại sơ đồ ghế, hay một job chạy định kỳ. Phải
> chốt trước khi bắt đầu Giai đoạn 4 và ghi vào plan. Không tự chọn ngầm trong
> code.

### 6.5 Các nhánh không thành công

| Tình huống | Xảy ra ở bước nào | Hệ thống làm gì |
| --- | --- | --- |
| Không tìm thấy chuyến | Sau khi tìm | Hiện khối rỗng, gợi ý đổi ngày |
| Chuyến bị hủy khi đang xem | Chi tiết chuyến trở đi | Chặn tiếp tục, thông báo rõ |
| Chỗ vừa bị người khác giữ | Chọn chỗ | Cập nhật sơ đồ, thông báo, cho chọn lại |
| Hết 5 phút giữ chỗ | Bất kỳ bước nào sau khi giữ | Trả chỗ, đưa về bước chọn chỗ |
| Số hành khách khác số chỗ | Nhập hành khách | Chặn tiếp tục |
| Chỗ bị mất ngay lúc thanh toán | Thanh toán | Rollback toàn bộ transaction, báo lỗi |

---

## 7. Quy trình hủy booking

### 7.1 Sơ đồ

```
[CUSTOMER hoặc ADMIN]
   │
   ▼
┌────────────────────────┐
│ Yêu cầu hủy booking    │
└──────────┬─────────────┘
           ▼
┌────────────────────────┐      không đạt
│ Kiểm tra điều kiện     ├───────────────► Từ chối, giải thích lý do
│ - booking CONFIRMED    │
│ - chuyến chưa khởi hành│
│ - đúng chủ sở hữu      │  (BR-31, chỉ với CUSTOMER)
└──────────┬─────────────┘
           ▼ đạt
┌════════════════════════┐
║ TRANSACTION            ║
║ Booking  CONFIRMED     ║
║       -> CANCELLED     ║
║ TripSeat BOOKED        ║
║       -> AVAILABLE     ║
└──────────┬═════════════┘
           ▼
┌────────────────────────┐
│ Chỗ trở lại thị trường │  người khác đặt được ngay
└────────────────────────┘
```

### 7.2 Quy tắc

Phiên bản đầu **không** tính phí hủy, **không** hoàn tiền, **không** có quy định
24h hay 48h. Vì thanh toán chỉ là mô phỏng, hủy chỉ đổi trạng thái và trả lại
chỗ.

Việc hủy cũng phải nằm trong transaction: không được để tồn tại booking đã
`CANCELLED` mà chỗ vẫn `BOOKED`.

Ticket đã sinh **không bị xóa**. Vé vẫn xem được nhưng hiển thị dấu đã hủy — để
người dùng còn tra cứu lại lịch sử.

---
# Phần III — Dữ liệu

## 8. Mô hình dữ liệu tổng quan

### 8.1 Danh sách thực thể

| Mã | Thực thể | Vai trò | Ai tạo |
| --- | --- | --- | --- |
| `EN-01` | `User` | Tài khoản đăng nhập, phân vai trò | Người dùng tự đăng ký; Admin từ seed |
| `EN-02` | `Station` | Ga tàu | Admin |
| `EN-03` | `Route` | Tuyến giữa hai ga | Admin |
| `EN-04` | `Train` | Đoàn tàu vật lý | Admin |
| `EN-05` | `Coach` | Toa của một tàu | Admin |
| `EN-06` | `Seat` | Chỗ vật lý trong toa | **Hệ thống sinh** khi tạo Coach |
| `EN-07` | `Trip` | Một lần chạy cụ thể | Admin |
| `EN-08` | `TripSeat` | Trạng thái chỗ theo từng chuyến | **Hệ thống sinh** khi tạo Trip |
| `EN-09` | `Booking` | Một lần đặt vé | Customer |
| `EN-10` | `BookingPassenger` | Một hành khách trong booking | Customer |
| `EN-11` | `Ticket` | Vé điện tử | **Hệ thống sinh** khi xác nhận booking |

MVP **không có**: `Staff`, `CheckIn`, `RouteStation`, `TripStation`,
`BookingSeat`, `Payment`.

Ba thực thể do hệ thống sinh (`Seat`, `TripSeat`, `Ticket`) là ba chỗ dễ sai
nhất, vì không ai nhìn thấy chúng được tạo ra. Cả ba đều phải có test.

### 8.2 Ba nhóm dữ liệu

Các thực thể chia thành ba nhóm với vòng đời rất khác nhau:

| Nhóm | Thực thể | Đặc điểm |
| --- | --- | --- |
| **Dữ liệu nền** | Station, Route, Train, Coach, Seat | Ít thay đổi. Tạo một lần, dùng lâu dài |
| **Dữ liệu vận hành** | Trip, TripSeat | Sinh theo ngày. Số lượng lớn nhất trong hệ thống |
| **Dữ liệu giao dịch** | Booking, BookingPassenger, Ticket | Sinh theo hành vi người dùng. Không được sửa sau khi xác nhận |

Phân biệt ba nhóm này quan trọng khi quyết định quy tắc xóa: xóa dữ liệu nền đang
được tham chiếu sẽ phá hỏng dữ liệu giao dịch đã hoàn tất.

### 8.3 Vì sao không lưu trạng thái chỗ trên Seat

Đây là quyết định thiết kế quan trọng nhất của toàn bộ mô hình dữ liệu.

```
SAI                                 ĐÚNG
Seat                                Seat                TripSeat
 ├── code    A01                     ├── code   A01      ├── tripId
 ├── coachId                         └── coachId         ├── seatId
 └── status  BOOKED  ◄── sai ở đây                       └── status  BOOKED
```

Nếu lưu `status` trên `Seat`, ghế `A01` của tàu SE2 chỉ có một trạng thái duy
nhất cho mọi chuyến. Đặt ghế đó cho chuyến ngày 24/08 sẽ làm nó biến mất khỏi
chuyến ngày 25/08 — dù đó là hai lần chạy hoàn toàn khác nhau.

`TripSeat` tách trạng thái ra theo từng chuyến, nên một ghế có thể trống ở chuyến
này và đã bán ở chuyến kia.

---

## 9. Sơ đồ quan hệ thực thể

### 9.1 Sơ đồ tổng

```
                      ┌──────────────┐
                      │   EN-01      │
                      │    User      │
                      │──────────────│
                      │ id      (PK) │
                      │ email        │
                      │ role         │
                      └──────┬───────┘
                             │ 1
                             │
                             │ N
   ┌──────────────┐   ┌──────▼───────┐        ┌──────────────┐
   │   EN-02      │   │   EN-09      │  N   1 │   EN-07      │
   │  Station     │   │   Booking    ├────────►    Trip      │
   │──────────────│   │──────────────│        │──────────────│
   │ id      (PK) │   │ id      (PK) │        │ id      (PK) │
   │ code     (U) │   │ bookingCode  │        │ trainId (FK) │
   │ name         │   │ customerId FK│        │ routeId (FK) │
   └───┬──────┬───┘   │ tripId   (FK)│        │ departureDate│
       │      │       │ totalPrice   │        │ basePrice    │
     1 │      │ 1     │ bookingStatus│        │ status       │
       │      │       │ paymentStatus│        └───┬──────┬───┘
       │origin│dest   └──────┬───────┘          1 │      │ 1
       │      │              │ 1                  │      │
     N │      │ N            │                    │      │
   ┌───▼──────▼───┐          │ N                  │      │ N
   │   EN-03      │   ┌──────▼────────┐           │  ┌───▼──────────┐
   │   Route      │   │   EN-10       │           │  │   EN-08      │
   │──────────────│   │BookingPassenger│    1   1 │  │  TripSeat    │
   │ id      (PK) │   │───────────────│           │  │──────────────│
   │ originSt  FK │   │ id       (PK) │           │  │ id      (PK) │
   │ destSt    FK │   │ bookingId  FK │───────────┼─►│ tripId  (FK) │
   │ distance     │   │ tripSeatId FK │           │  │ seatId  (FK) │
   └──────▲───────┘   │ fullName      │           │  │ status       │
          │ N         │ identityNumber│           │  │ heldBy       │
          │           │ ticketPrice   │           │  │ heldUntil    │
          │ 1         └──────┬────────┘           │  └──────┬───────┘
          │                  │ 1                  │         │ N
   ┌──────┴───────┐          │                    │         │
   │   EN-07 Trip │          │ 1                  │         │ 1
   │  (xem ở trên)│   ┌──────▼────────┐           │  ┌──────▼───────┐
   └──────┬───────┘   │   EN-11       │           │  │   EN-06      │
          │ N         │   Ticket      │           │  │    Seat      │
          │           │───────────────│           │  │──────────────│
          │ 1         │ id       (PK) │           │  │ id      (PK) │
   ┌──────▼───────┐   │ ticketCode (U)│           │  │ coachId (FK) │
   │   EN-04      │   │ passengerId FK│           │  │ code         │
   │   Train      │   └───────────────┘           │  │ type         │
   │──────────────│                               │  │ cabinNumber  │
   │ id      (PK) │                               │  └──────▲───────┘
   │ trainCode (U)│                               │         │ N
   └──────┬───────┘                               │         │
          │ 1                                     │         │ 1
          │                                       │  ┌──────┴───────┐
          │ N                                     │  │   EN-05      │
   ┌──────▼───────┐            1                  │  │   Coach      │
   │   EN-05 Coach├─────────────────────────────────►│──────────────│
   │  (xem bên →) │                     N            │ id      (PK) │
   └──────────────┘                                  │ trainId (FK) │
                                                     │ coachType    │
   (U) = unique     (PK) = khóa chính                │ capacity     │
   (FK) = khóa ngoại                                 │ priceModifier│
                                                     └──────────────┘
```

### 9.2 Sơ đồ rút gọn theo hướng phụ thuộc

Dễ nhìn hơn khi cần nhớ nhanh:

```
Station ──┬─► Route ──┐
          │           │
          └───────────┤
                      ▼
Train ──► Coach ──► Seat        Trip ◄── Route
  │                   │          │
  └───────────────────┼──────────┘
                      ▼
                   TripSeat
                      ▲
                      │
User ──► Booking ──► BookingPassenger ──► Ticket
             │
             └──► Trip
```

Đọc sơ đồ này từ trên xuống chính là thứ tự Admin phải tạo dữ liệu, và cũng là
thứ tự xây dựng phần mềm ở [mục 25](#25-kế-hoạch-xây-dựng).

---

## 10. Từ điển dữ liệu

Kiểu dữ liệu dưới đây là **đề xuất của phân tích**, không có trong `SPEC.md`.
Chúng chọn theo MySQL 8.4 và có thể điều chỉnh khi hiện thực bằng JPA entity.

Ký hiệu: `PK` khóa chính · `FK` khóa ngoại · `U` unique · `NN` không được null.

---

### 10.1 `EN-01` User

Tài khoản đăng nhập. Một `User` có thể có nhiều `Booking`.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `email` | VARCHAR(150) | NN | U | Dùng để đăng nhập. Phải đúng định dạng email |
| `password` | VARCHAR(255) | NN | | **Đã hash**. Không bao giờ lưu bản rõ (BR-28) |
| `fullName` | VARCHAR(150) | NN | | Họ tên hiển thị |
| `role` | ENUM | NN | `CUSTOMER`, `ADMIN` | Vai trò. Đăng ký qua giao diện luôn ra `CUSTOMER` |
| `createdAt` | DATETIME | NN | mặc định hiện tại | Thời điểm tạo |

**Ghi chú:** không có trường `status` hay `enabled` — phiên bản đầu không khóa
tài khoản. Không có `phone`, `address` vì không dùng tới.

---

### 10.2 `EN-02` Station

Ga tàu. Là dữ liệu nền, thay đổi rất ít.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `name` | VARCHAR(150) | NN | | Tên ga, ví dụ `Ga Sài Gòn` |
| `code` | VARCHAR(10) | NN | U | Mã ga, ví dụ `SGN`. Không được trùng (BR-01) |
| `city` | VARCHAR(100) | | | Tỉnh hoặc thành phố, ví dụ `Hồ Chí Minh` |
| `address` | VARCHAR(255) | | | Địa chỉ đầy đủ |
| `latitude` | DECIMAL(10,7) | | | Chỉ cần khi làm Railway Map (optional) |
| `longitude` | DECIMAL(10,7) | | | Chỉ cần khi làm Railway Map (optional) |

**Quy tắc xóa:** chỉ xóa được ga chưa được `Route` nào tham chiếu (BR-33).

---

### 10.3 `EN-03` Route

Tuyến giữa hai ga. **Không quản lý ga trung gian** — quyết định này làm database,
tìm chuyến, tính giá và booking đều đơn giản hơn.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `originStationId` | BIGINT | NN | FK → Station | Ga đi |
| `destinationStationId` | BIGINT | NN | FK → Station | Ga đến. Phải khác ga đi (BR-02) |
| `distance` | INT | | | Khoảng cách, đơn vị km |

**Ghi chú:** `Route` chỉ mô tả một chiều. Tuyến Sài Gòn → Nha Trang và Nha Trang
→ Sài Gòn là hai bản ghi khác nhau.

---

### 10.4 `EN-04` Train

Đoàn tàu vật lý. Dùng lại cho nhiều `Trip`.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `trainCode` | VARCHAR(20) | NN | U | Mã tàu, ví dụ `SE2` |
| `name` | VARCHAR(150) | | | Tên hiển thị |

**Ghi chú:** `Train` không gắn với `Route`. Cùng một tàu chạy được nhiều tuyến
khác nhau ở những chuyến khác nhau.

---

### 10.5 `EN-05` Coach

Toa của một tàu.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `trainId` | BIGINT | NN | FK → Train | Toa thuộc tàu nào |
| `coachNumber` | VARCHAR(10) | NN | | Số toa, ví dụ `01` |
| `coachType` | ENUM | NN | `SEAT`, `SLEEPER` | Toa ghế hay toa giường |
| `capacity` | INT | NN | > 0 | Số chỗ. Quyết định số `Seat` được sinh |
| `priceModifier` | DECIMAL(12,2) | NN | mặc định 0 | Cộng thêm vào `basePrice` của chuyến |

**Ghi chú:** toa ghế thường có `priceModifier = 0`; toa giường thường dương, ví
dụ `200000`.

Với `coachType = SLEEPER`, `capacity` nên chia hết cho 4 vì mỗi cabin 4 giường.
Nếu không chia hết, cabin cuối sẽ thiếu giường — hệ thống nên cảnh báo.

---

### 10.6 `EN-06` Seat

Chỗ vật lý trong toa. **Do hệ thống sinh**, Admin không nhập tay (BR-04).

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `coachId` | BIGINT | NN | FK → Coach | Ghế thuộc toa nào |
| `code` | VARCHAR(10) | NN | | Mã chỗ, `A01` cho ghế, `B01` cho giường |
| `type` | ENUM | NN | `SEAT`, `BED` | Ghế hay giường |
| `cabinNumber` | INT | | | Chỉ có ý nghĩa với toa `SLEEPER`. Mỗi cabin 4 giường |

**Quy tắc sinh:**

```
coachType = SEAT,    capacity = 16  ->  A01..A16,  type = SEAT,  cabinNumber = null
coachType = SLEEPER, capacity = 16  ->  B01..B16,  type = BED,
                                        cabinNumber: B01-B04 = 1, B05-B08 = 2, ...
```

Phiên bản đầu **không** phân biệt giường tầng 1, 2, 3; không có VIP; không có
phòng riêng.

---

### 10.7 `EN-07` Trip

Một lần chạy cụ thể của một `Train` trên một `Route`.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `trainId` | BIGINT | NN | FK → Train | Tàu chạy chuyến này |
| `routeId` | BIGINT | NN | FK → Route | Tuyến của chuyến |
| `departureDate` | DATE | NN | | Ngày khởi hành |
| `departureTime` | TIME | NN | | Giờ khởi hành |
| `arrivalTime` | TIME | NN | | Giờ đến. Nhỏ hơn giờ đi nghĩa là sang ngày hôm sau |
| `basePrice` | DECIMAL(12,2) | NN | > 0 | Giá cơ bản của chuyến |
| `status` | ENUM | NN | `SCHEDULED`, `COMPLETED`, `CANCELLED` | Trạng thái chuyến |

MVP **không có** trạng thái `BOARDING`, `DEPARTED`, `DELAYED`.

**Ghi chú quan trọng:** sửa `basePrice` **không** làm thay đổi giá của booking đã
tạo, vì giá đã được đóng băng trong `BookingPassenger.ticketPrice` (BR-19).

---

### 10.8 `EN-08` TripSeat

Trạng thái của một chỗ trong một chuyến. **Bảng quan trọng nhất hệ thống** — mọi
lỗi trùng chỗ đều xảy ra ở đây.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `tripId` | BIGINT | NN | FK → Trip, **U với seatId** | Chuyến nào |
| `seatId` | BIGINT | NN | FK → Seat, **U với tripId** | Chỗ nào |
| `status` | ENUM | NN | `AVAILABLE`, `HELD`, `BOOKED` | Trạng thái chỗ |
| `heldBy` | BIGINT | | FK → User | Ai đang giữ. Null khi không ai giữ |
| `heldUntil` | DATETIME | | | Giữ tới thời điểm nào. Null khi không ai giữ |

**Ràng buộc unique `(tripId, seatId)` phải khai ở tầng database**, không chỉ ở
tầng code. Đây là lớp phòng thủ cuối cùng chống hai booking cùng chiếm một chỗ
(BR-06). Code có thể sai, database thì không.

**`SELECTED` không phải trạng thái của bảng này.** Nó chỉ tồn tại trên giao diện
khi người dùng vừa click, không lưu xuống database.

---

### 10.9 `EN-09` Booking

Một lần đặt vé.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `bookingCode` | VARCHAR(20) | NN | U | Mã hiển thị cho người dùng, ví dụ `BK202608001` |
| `customerId` | BIGINT | NN | FK → User | Người đặt |
| `tripId` | BIGINT | NN | FK → Trip | Một booking chỉ thuộc một chuyến (BR-15) |
| `totalPrice` | DECIMAL(12,2) | NN | | Tổng tiền, bằng tổng `ticketPrice` của các hành khách |
| `bookingStatus` | ENUM | NN | `PENDING`, `CONFIRMED`, `CANCELLED` | Trạng thái booking |
| `paymentStatus` | ENUM | NN | `UNPAID`, `PAID` | Trạng thái thanh toán |
| `paidAt` | DATETIME | | | Thời điểm thanh toán. Null khi chưa trả |
| `createdAt` | DATETIME | NN | mặc định hiện tại | Thời điểm tạo |

**Không có bảng `Payment` riêng.** Trạng thái thanh toán lưu ngay trong Booking,
vì thanh toán chỉ là mô phỏng, không có giao dịch, không có mã tham chiếu ngân
hàng.

**Không có trạng thái `COMPLETED`.** Nếu booking `CONFIRMED` và chuyến
`COMPLETED` thì giao diện tự xếp vào nhóm đã hoàn thành, không cần lưu thêm.

---

### 10.10 `EN-10` BookingPassenger

Một hành khách trong booking. Gắn với đúng một `TripSeat`.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `bookingId` | BIGINT | NN | FK → Booking | Thuộc booking nào |
| `tripSeatId` | BIGINT | NN | FK → TripSeat, U | Chỗ của hành khách này. Một chỗ không gán cho hai người (BR-17) |
| `fullName` | VARCHAR(150) | NN | | Họ tên hành khách |
| `dateOfBirth` | DATE | NN | | Ngày sinh |
| `identityNumber` | VARCHAR(30) | NN | | CCCD hoặc số passport |
| `ticketPrice` | DECIMAL(12,2) | NN | | **Giá tại thời điểm đặt**, đóng băng (BR-19) |

**Vì sao lưu `ticketPrice` ở đây** thay vì tính lại từ Trip và Coach: Admin sửa
`basePrice` về sau không được làm thay đổi giá của booking cũ. Giá phải là ảnh
chụp tại thời điểm giao dịch.

Phiên bản đầu **không** có giá theo người lớn / trẻ em, người cao tuổi, sinh
viên.

---

### 10.11 `EN-11` Ticket

Vé điện tử. Mỗi hành khách một vé. **Do hệ thống sinh** khi booking được xác
nhận.

| Trường | Kiểu | Bắt buộc | Ràng buộc | Mô tả |
| --- | --- | --- | --- | --- |
| `id` | BIGINT | NN | PK, auto | Định danh |
| `ticketCode` | VARCHAR(30) | NN | U | Mã vé, ví dụ `TK202608001-1` |
| `bookingPassengerId` | BIGINT | NN | FK → BookingPassenger, U | Vé của hành khách nào |
| `issuedAt` | DATETIME | NN | | Thời điểm phát hành |

Các thông tin còn lại trên vé — tên hành khách, tuyến, tàu, ngày giờ khởi hành,
toa, chỗ — đọc qua quan hệ chứ không nhân bản vào bảng này.

**Nếu về sau làm QR code:** mã chỉ chứa `ticketCode` hoặc `id`, tuyệt đối không
chứa CCCD, passport hay thông tin cá nhân.

---

## 11. Ma trận quan hệ giữa các bảng

### 11.1 Bảng quan hệ

| Từ | Tới | Kiểu | Bắt buộc | Ý nghĩa nghiệp vụ |
| --- | --- | --- | --- | --- |
| `Route` | `Station` (origin) | N → 1 | Có | Nhiều tuyến cùng xuất phát từ một ga |
| `Route` | `Station` (destination) | N → 1 | Có | Nhiều tuyến cùng kết thúc ở một ga |
| `Coach` | `Train` | N → 1 | Có | Một tàu nhiều toa |
| `Seat` | `Coach` | N → 1 | Có | Một toa nhiều chỗ |
| `Trip` | `Train` | N → 1 | Có | Một tàu chạy nhiều chuyến |
| `Trip` | `Route` | N → 1 | Có | Một tuyến có nhiều chuyến |
| `TripSeat` | `Trip` | N → 1 | Có | Một chuyến có nhiều chỗ |
| `TripSeat` | `Seat` | N → 1 | Có | Một ghế xuất hiện ở nhiều chuyến |
| `TripSeat` | `User` (heldBy) | N → 1 | Không | Ai đang giữ chỗ tạm |
| `Booking` | `User` | N → 1 | Có | Một người nhiều booking |
| `Booking` | `Trip` | N → 1 | Có | Một booking một chuyến duy nhất |
| `BookingPassenger` | `Booking` | N → 1 | Có | Một booking nhiều hành khách |
| `BookingPassenger` | `TripSeat` | 1 → 1 | Có | Mỗi hành khách đúng một chỗ |
| `Ticket` | `BookingPassenger` | 1 → 1 | Có | Mỗi hành khách một vé |

### 11.2 Hai quan hệ 1–1 cần chú ý

`BookingPassenger` → `TripSeat` và `Ticket` → `BookingPassenger` đều là 1–1, thể
hiện bằng ràng buộc unique trên khóa ngoại.

Nếu quên khai unique, hệ thống vẫn chạy nhưng cho phép hai hành khách cùng một
chỗ, hoặc một hành khách hai vé. Cả hai lỗi đều chỉ lộ ra khi dữ liệu đã sai.

### 11.3 Đường đi từ vé về chuyến

Khi hiển thị vé, hệ thống phải đi qua chuỗi quan hệ này:

```
Ticket
  └─► BookingPassenger
        ├─► Booking ──► Trip ──┬─► Train
        │                      └─► Route ──┬─► Station (đi)
        │                                  └─► Station (đến)
        └─► TripSeat ──► Seat ──► Coach
```

Chuỗi khá dài. Khi hiện thực, cân nhắc `JOIN FETCH` để tránh N+1 query — nhưng
chỉ tối ưu sau khi đo được vấn đề thật, không tối ưu trước.

---

## 12. Vòng đời trạng thái

### 12.1 TripSeat

```
              ┌──────────────┐
    ┌────────►│  AVAILABLE   │◄──────────┐
    │         └──────┬───────┘           │
    │                │                   │
    │      Customer chọn chỗ             │ hủy booking
    │      ghi heldBy, heldUntil         │ (BOOKED -> AVAILABLE)
    │                ▼                   │
    │         ┌──────────────┐           │
    │         │     HELD     │           │
    └─────────┤              │           │
  hết 5 phút  └──────┬───────┘           │
  heldUntil          │                   │
  đã qua      thanh toán thành công      │
                     ▼                   │
              ┌──────────────┐           │
              │    BOOKED    ├───────────┘
              └──────────────┘
```

| Từ | Tới | Điều kiện | Ai kích hoạt |
| --- | --- | --- | --- |
| `AVAILABLE` | `HELD` | Chỗ đang trống, chuyến hợp lệ | Customer chọn chỗ |
| `HELD` | `BOOKED` | Trong transaction thanh toán, còn hạn giữ | Customer thanh toán |
| `HELD` | `AVAILABLE` | `heldUntil` đã qua | Hệ thống |
| `BOOKED` | `AVAILABLE` | Booking bị hủy | Customer hoặc Admin |

**Chuyển trạng thái không hợp lệ:** `AVAILABLE` → `BOOKED` (bỏ qua giữ chỗ),
`BOOKED` → `HELD`, `CANCELLED` → bất kỳ. Backend phải từ chối.

### 12.2 Booking

```
      tạo booking
           │
           ▼
   ┌───────────────┐    thanh toán thành công   ┌───────────────┐
   │    PENDING    ├───────────────────────────►│   CONFIRMED   │
   └───────┬───────┘                            └───────┬───────┘
           │                                            │
           │ hết giờ giữ chỗ                            │ Customer hoặc Admin hủy
           │ hoặc người dùng bỏ                         │ chuyến chưa khởi hành
           ▼                                            ▼
   ┌───────────────┐                            ┌───────────────┐
   │   CANCELLED   │◄───────────────────────────┤   CANCELLED   │
   └───────────────┘                            └───────────────┘
```

| Từ | Tới | Điều kiện |
| --- | --- | --- |
| — | `PENDING` | Customer bắt đầu tạo booking |
| `PENDING` | `CONFIRMED` | `paymentStatus = PAID` (BR-21) |
| `PENDING` | `CANCELLED` | Hết giờ giữ chỗ hoặc người dùng bỏ dở |
| `CONFIRMED` | `CANCELLED` | Chuyến chưa khởi hành, đúng chủ sở hữu |

`CANCELLED` là trạng thái cuối. Không có đường quay lại.

### 12.3 Payment

```
   ┌──────────┐   bấm xác nhận thanh toán   ┌──────────┐
   │  UNPAID  ├────────────────────────────►│   PAID   │
   └──────────┘   ghi paidAt                └──────────┘
```

Không có `REFUNDED`, không có `FAILED` — thanh toán mô phỏng luôn thành công trừ
khi chính booking không hợp lệ. Hủy booking **không** đổi `paymentStatus` về
`UNPAID`, vì tiền không hoàn thật.

### 12.4 Trip

```
   ┌─────────────┐   chuyến đã chạy xong   ┌─────────────┐
   │  SCHEDULED  ├────────────────────────►│  COMPLETED  │
   └──────┬──────┘                         └─────────────┘
          │
          │ Admin hủy chuyến
          ▼
   ┌─────────────┐
   │  CANCELLED  │
   └─────────────┘
```

Chuyến `CANCELLED` không cho chọn chỗ, tạo booking hay thanh toán (BR-23).

---

## 13. Ràng buộc toàn vẹn dữ liệu

### 13.1 Ràng buộc phải khai ở tầng database

Đây là những ràng buộc **không được** chỉ kiểm tra trong code, vì code có thể bị
bỏ qua trong một nhánh nào đó:

| Bảng | Ràng buộc | Chống lỗi gì |
| --- | --- | --- |
| `TripSeat` | UNIQUE `(tripId, seatId)` | Hai bản ghi cho cùng một chỗ trong cùng chuyến |
| `BookingPassenger` | UNIQUE `tripSeatId` | Hai hành khách cùng một chỗ |
| `Ticket` | UNIQUE `bookingPassengerId` | Một hành khách hai vé |
| `Ticket` | UNIQUE `ticketCode` | Trùng mã vé |
| `Booking` | UNIQUE `bookingCode` | Trùng mã booking |
| `User` | UNIQUE `email` | Hai tài khoản cùng email |
| `Station` | UNIQUE `code` | Hai ga cùng mã |
| `Train` | UNIQUE `trainCode` | Hai tàu cùng mã |

### 13.2 Hai trạng thái tuyệt đối không được tồn tại

```
❌ Booking.bookingStatus = CONFIRMED  và  TripSeat.status = AVAILABLE
❌ TripSeat.status = BOOKED           và  Booking chưa được xác nhận
```

Cả hai đều là hậu quả của transaction không bao trọn. Phải có test khẳng định
rollback (BR-20).

### 13.3 Quy tắc xóa

> **Chưa quyết:** quy tắc xóa khi dữ liệu đã được tham chiếu — chặn xóa hay xóa
> mềm. `SPEC.md` mới chỉ nói ga "xóa nếu chưa được sử dụng"; các thực thể còn lại
> chưa có quy tắc. Cần chốt trước khi làm CRUD admin ở Giai đoạn 2.

Đề xuất của phân tích, chờ chốt:

| Thực thể | Đề xuất | Lý do |
| --- | --- | --- |
| `Station` | Chặn xóa nếu có `Route` tham chiếu | `SPEC.md` đã nói |
| `Route` | Chặn xóa nếu có `Trip` | Xóa sẽ làm chuyến mất tuyến |
| `Train` | Chặn xóa nếu có `Trip` | Như trên |
| `Coach`, `Seat` | Chặn xóa nếu `TripSeat` liên quan đã `BOOKED` | Xóa sẽ phá vé đã bán |
| `Trip` | Chặn xóa nếu có `Booking`; hủy chuyến thay vì xóa | Giữ lịch sử giao dịch |
| `Booking` | Không bao giờ xóa, chỉ chuyển `CANCELLED` | Dữ liệu giao dịch |

### 13.4 Dữ liệu bắt buộc phải kiểm tra ở backend

Không tin validation ở frontend (NFR-02). Tối thiểu:

| Dữ liệu | Kiểm tra |
| --- | --- |
| Email | Đúng định dạng, chưa tồn tại |
| Mật khẩu | Đủ độ dài tối thiểu |
| Ga đi, ga đến | Bắt buộc, phải khác nhau, phải tồn tại |
| Ngày đi | Không nhỏ hơn ngày hiện tại |
| Số hành khách | Lớn hơn 0, bằng số chỗ đã chọn |
| `capacity` của toa | Lớn hơn 0 |
| `basePrice` | Lớn hơn 0 |
| CCCD / passport | Bắt buộc, không rỗng |
| Booking khi truy cập | Thuộc về người đang đăng nhập (BR-31) |

---
# Phần IV — Yêu cầu và tính năng

## 14. Yêu cầu chức năng

Cột **Ưu tiên** dùng thang MoSCoW: `M` bắt buộc có, `S` nên có, `C` có thì tốt.
Toàn bộ `M` hợp thành MVP.

### 14.1 Xác thực và phân quyền

| Mã | Yêu cầu | Ưu tiên | Use case |
| --- | --- | --- | --- |
| `FR-01` | Khách đăng ký tài khoản bằng email và mật khẩu | M | UC-01 |
| `FR-02` | Người dùng đăng nhập bằng email và mật khẩu, dùng session | M | UC-02 |
| `FR-03` | Người dùng đăng xuất, session bị hủy | M | UC-03 |
| `FR-04` | Hệ thống phân quyền theo vai trò `CUSTOMER` và `ADMIN` | M | UC-01..UC-25 |

### 14.2 Tìm và xem chuyến

| Mã | Yêu cầu | Ưu tiên | Use case |
| --- | --- | --- | --- |
| `FR-05` | Người dùng tìm chuyến theo ga đi, ga đến, ngày, số hành khách | M | UC-04 |
| `FR-06` | Hệ thống hiển thị danh sách chuyến phù hợp kèm số chỗ còn và giá thấp nhất | M | UC-05 |
| `FR-07` | Người dùng xem chi tiết một chuyến với danh sách toa | M | UC-06 |
| `FR-08` | Landing hiển thị tuyến phổ biến, click là điền sẵn ô tìm kiếm | S | UC-04 |

### 14.3 Chọn chỗ và giữ chỗ

| Mã | Yêu cầu | Ưu tiên | Use case |
| --- | --- | --- | --- |
| `FR-09` | Customer chọn toa trong chuyến | M | UC-07 |
| `FR-10` | Hệ thống vẽ sơ đồ ghế cho toa `SEAT` | M | UC-08 |
| `FR-11` | Hệ thống vẽ sơ đồ giường theo cabin cho toa `SLEEPER` | M | UC-08 |
| `FR-12` | Hệ thống giữ chỗ tạm thời trong 5 phút khi Customer chọn | M | UC-08 |
| `FR-13` | Giao diện đếm ngược thời gian giữ chỗ | M | UC-08 |
| `FR-14` | Hệ thống trả chỗ hết hạn giữ về `AVAILABLE` | M | UC-08 |

### 14.4 Booking và thanh toán

| Mã | Yêu cầu | Ưu tiên | Use case |
| --- | --- | --- | --- |
| `FR-15` | Customer nhập thông tin từng hành khách, mỗi người một chỗ | M | UC-09 |
| `FR-16` | Hệ thống hiển thị màn hình review với giá từng vé và tổng tiền | M | UC-10 |
| `FR-17` | Customer thanh toán mô phỏng, hệ thống xác nhận booking trong một transaction | M | UC-11 |
| `FR-18` | Hệ thống sinh vé điện tử cho từng hành khách | M | UC-11, UC-12 |

### 14.5 Quản lý booking của Customer

| Mã | Yêu cầu | Ưu tiên | Use case |
| --- | --- | --- | --- |
| `FR-19` | Customer xem danh sách booking của mình, chia Upcoming / Completed / Cancelled | M | UC-13 |
| `FR-20` | Customer xem chi tiết một booking | M | UC-14 |
| `FR-21` | Customer xem vé điện tử của mình | M | UC-12 |
| `FR-22` | Customer hủy booking của mình khi chuyến chưa khởi hành | M | UC-15 |

### 14.6 Quản trị

| Mã | Yêu cầu | Ưu tiên | Use case |
| --- | --- | --- | --- |
| `FR-23` | Admin xem dashboard bốn chỉ số tổng quan | M | UC-16 |
| `FR-24` | Admin quản lý ga: xem, thêm, sửa, xóa | M | UC-17 |
| `FR-25` | Admin quản lý tuyến: xem, thêm, sửa, xóa | M | UC-18 |
| `FR-26` | Admin quản lý tàu: xem, thêm, sửa | M | UC-19 |
| `FR-27` | Admin cấu hình toa, hệ thống tự sinh ghế | M | UC-20 |
| `FR-28` | Admin quản lý chuyến, hệ thống tự sinh TripSeat | M | UC-21 |
| `FR-29` | Admin xem danh sách và chi tiết booking, tìm theo mã | M | UC-22, UC-23 |
| `FR-30` | Admin hủy booking | M | UC-24 |
| `FR-31` | Admin xem danh sách tài khoản, chỉ đọc | M | UC-25 |

### 14.7 Optional

| Mã | Yêu cầu | Ưu tiên | Điều kiện |
| --- | --- | --- | --- |
| `FR-32` | Vé có QR code chứa `ticketCode` | C | Sau Giai đoạn 7 |
| `FR-33` | Xuất vé ra PDF | C | Sau FR-32 |
| `FR-34` | Bản đồ đường sắt hiển thị vị trí ga | C | Sau Giai đoạn 7 |

---

## 15. Yêu cầu phi chức năng

### `NFR-01` Responsive

Các trang Customer phải dùng được trên desktop, tablet và mobile.

Ưu tiên: landing, tìm kiếm, chọn chỗ, My Trips, vé.

**Cách kiểm chứng:** mở ở 375px, 768px, 1280px; không trang nào được cuộn ngang ở
cấp trang. Sơ đồ ghế được cuộn ngang **trong khung riêng của nó**.

### `NFR-02` Validation ở backend

Backend phải tự kiểm tra dữ liệu, không tin validation ở frontend. Dùng Bean
Validation: `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`.

**Cách kiểm chứng:** gửi request thẳng tới endpoint bằng công cụ ngoài trình
duyệt với dữ liệu sai, hệ thống phải từ chối.

### `NFR-03` Bảo mật

- Mật khẩu phải được hash trước khi lưu (BR-28).
- Customer không vào được trang Admin (BR-30).
- Customer không xem được booking của người khác (BR-31).
- Backend kiểm tra **cả** vai trò lẫn quyền sở hữu.

**Cách kiểm chứng:** đăng nhập bằng tài khoản Customer rồi gõ tay URL
`/admin/stations` và `/my-trips/<id của người khác>`. Cả hai phải bị chặn. Bắt
buộc có test tự động.

### `NFR-04` Nhất quán dữ liệu

Xác nhận booking, hủy booking và giữ chỗ phải chạy trong transaction.

**Cách kiểm chứng:** test khẳng định rollback khi một bước trong transaction thất
bại.

### `NFR-05` Xử lý lỗi

Mỗi tình huống lỗi phải có thông báo người dùng hiểu được, không để văng stack
trace ra màn hình:

- Chuyến không tồn tại hoặc đã bị hủy.
- Chỗ vừa bị người khác đặt trong lúc mình đang thao tác.
- Hết thời gian giữ chỗ.
- Booking không tồn tại.
- Booking không thuộc về Customer đang đăng nhập.
- Form không hợp lệ.

### `NFR-06` Chạy được khi không có mạng ngoài

Toàn bộ tài nguyên giao diện phải phục vụ từ chính ứng dụng, không phụ thuộc CDN.

**Cách kiểm chứng:** ngắt wifi, tải lại trang bỏ qua cache. Trang phải giữ nguyên
giao diện.

Lý do: sản phẩm được chấm qua một buổi demo. Mất mạng giữa buổi mà giao diện vỡ
trắng là rủi ro không đáng chấp nhận.

---

## 16. Quy tắc nghiệp vụ

Quy tắc là thứ hệ thống phải bảo đảm bất kể người dùng thao tác thế nào. Mỗi quy
tắc ghi rõ **hệ thống làm gì khi bị vi phạm**.

### 16.1 Dữ liệu nền

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-01` | Mã ga (`Station.code`) không được trùng | Từ chối, báo lỗi ngay dưới ô mã |
| `BR-02` | Ga đi phải khác ga đến trong một `Route` | Từ chối tạo tuyến |
| `BR-03` | Mã tàu (`Train.trainCode`) không được trùng | Từ chối, báo lỗi |
| `BR-04` | Khi tạo `Coach`, hệ thống tự sinh đủ `Seat` theo `capacity`. Admin không nhập tay từng ghế | Không áp dụng — đây là hành vi bắt buộc của hệ thống |
| `BR-05` | Khi tạo `Trip`, hệ thống sinh `TripSeat` cho toàn bộ `Seat` của tàu, tất cả ở `AVAILABLE` | Không áp dụng — hành vi bắt buộc |
| `BR-06` | Cặp `(tripId, seatId)` phải duy nhất, khai ở **tầng database** | Database từ chối insert; ứng dụng phải bắt và báo chỗ đã có người |

### 16.2 Tìm chuyến

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-10` | Ga đi và ga đến đều bắt buộc và phải khác nhau | Không thực hiện tìm kiếm, báo lỗi dưới form |
| `BR-11` | Ngày đi không được nhỏ hơn ngày hiện tại | Không tìm kiếm, báo lỗi |
| `BR-12` | Số hành khách phải lớn hơn 0 | Không tìm kiếm, báo lỗi |

### 16.3 Chọn chỗ và giữ chỗ

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-07` | Một `TripSeat` chỉ thuộc về một booking đã xác nhận | Từ chối booking thứ hai, thông báo chỗ đã có người |
| `BR-08` | Không giữ được chỗ đang `HELD` bởi người khác hoặc đã `BOOKED` | Từ chối, cập nhật lại sơ đồ ghế |
| `BR-09` | Backend luôn đọc lại trạng thái `TripSeat` từ database, **không tin** trạng thái trình duyệt gửi lên | Không áp dụng — đây là cách hiện thực bắt buộc |
| `BR-13` | Thời gian giữ chỗ là **5 phút**, ghi vào `heldBy` và `heldUntil` | Không áp dụng — hằng số nghiệp vụ |
| `BR-14` | Chỗ quá `heldUntil` phải trở về `AVAILABLE` | Người giữ mất chỗ, bị đưa về bước chọn chỗ |

### 16.4 Hành khách và giá

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-15` | Một `Booking` chỉ thuộc một `Trip`. Không có multi-trip, connecting train hay khứ hồi trong cùng booking | Từ chối |
| `BR-16` | Số hành khách phải bằng số chỗ đã chọn | Chặn sang bước tiếp theo |
| `BR-17` | Mỗi hành khách gắn đúng một `TripSeat`; một `TripSeat` không được gán cho hai hành khách | Từ chối, database cũng chặn bằng unique |
| `BR-18` | Giá vé = `Trip.basePrice` + `Coach.priceModifier` | Không áp dụng — công thức duy nhất, không có dynamic pricing |
| `BR-19` | `ticketPrice` được đóng băng vào `BookingPassenger` tại thời điểm đặt | Không áp dụng — sửa `basePrice` sau đó không đổi giá booking cũ |

### 16.5 Xác nhận booking

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-20` | Bốn thay đổi khi xác nhận — `paymentStatus`, `bookingStatus`, `TripSeat.status`, sinh `Ticket` — phải nằm trong **một** transaction | Rollback toàn bộ, không để dữ liệu nửa vời |
| `BR-21` | `Booking` chỉ chuyển sang `CONFIRMED` khi `paymentStatus = PAID` | Từ chối chuyển trạng thái |
| `BR-22` | Trước khi commit, backend kiểm tra lại: chuyến còn hợp lệ, các `TripSeat` vẫn do chính Customer này giữ, và chưa hết hạn giữ | Rollback, báo lỗi cụ thể |

### 16.6 Điều kiện của chuyến

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-23` | `Trip` có `status = CANCELLED` không được chọn chỗ, tạo booking hay thanh toán | Chặn ở mọi bước, thông báo chuyến đã hủy |
| `BR-24` | `Trip` trong quá khứ không được đặt | Không hiện trong kết quả tìm; nếu vào bằng URL thì chặn |

### 16.7 Hủy booking

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-25` | Chỉ hủy được booking đang `CONFIRMED` và chuyến chưa khởi hành | Từ chối, giải thích lý do |
| `BR-26` | Hủy booking trả toàn bộ `TripSeat` liên quan về `AVAILABLE` | Không áp dụng — hành vi bắt buộc |
| `BR-27` | Việc hủy nằm trong transaction. Không tính phí hủy, không hoàn tiền | Rollback nếu một bước thất bại |

### 16.8 Bảo mật và phân quyền

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-28` | Mật khẩu phải được hash trước khi lưu | Không áp dụng — không có đường lưu bản rõ |
| `BR-29` | Đăng ký qua giao diện luôn tạo tài khoản `CUSTOMER` | Không cho chọn vai trò ở form đăng ký |
| `BR-30` | Backend kiểm tra vai trò, **không chỉ** ẩn menu. Customer gõ tay `/admin/**` phải bị chặn | Trả 403 |
| `BR-31` | Backend kiểm tra quyền sở hữu. Customer chỉ xem và hủy được booking của chính mình | Trả 403, **không hiện nội dung** |

Hai lỗi phân quyền hay gặp nhất trong đồ án là chỉ ẩn nút trên giao diện, và quên
kiểm tra quyền sở hữu khi truy cập theo id. **Cả hai đều phải có test.**

### 16.9 Quản trị

| Mã | Quy tắc | Khi vi phạm |
| --- | --- | --- |
| `BR-32` | Admin **không** đổi chỗ sau khi đã đặt, **không** sửa trạng thái thanh toán thủ công, **không** hoàn tiền, **không** đổi hành khách. Chỉ được hủy booking | Giao diện không cung cấp chức năng đó |
| `BR-33` | Chỉ xóa được ga chưa được `Route` nào dùng | Chặn xóa, chỉ ra nơi đang dùng |
| `BR-34` | Sửa `Trip.basePrice` không làm thay đổi giá của booking đã tạo | Không áp dụng — hệ quả của BR-19 |
| `BR-35` | Tài khoản `ADMIN` tạo bằng seed data, không đăng ký qua giao diện | Form đăng ký không có tùy chọn vai trò |

---

## 17. Danh mục use case

### 17.1 Sơ đồ use case

```
                          ┌──────────────────────────────────────┐
                          │            HỆ THỐNG TrainGo          │
                          │                                      │
   ┌─────────┐            │  ┌────────────────────────────────┐  │
   │  KHÁCH  ├────────────┼─►│ UC-01 Đăng ký                  │  │
   │(chưa    ├────────────┼─►│ UC-02 Đăng nhập                │  │
   │ đăng    ├────────────┼─►│ UC-04 Tìm chuyến               │  │
   │ nhập)   ├────────────┼─►│ UC-05 Xem danh sách chuyến     │  │
   └─────────┘            │  │ UC-06 Xem chi tiết chuyến      │  │
                          │  └────────────────────────────────┘  │
                          │                                      │
   ┌─────────┐            │  ┌────────────────────────────────┐  │
   │CUSTOMER ├────────────┼─►│ UC-03 Đăng xuất                │  │
   │         │            │  │ UC-07 Chọn toa                 │  │
   │(kế thừa │            │  │ UC-08 Chọn chỗ và giữ chỗ      │  │
   │ quyền   ├────────────┼─►│ UC-09 Nhập hành khách          │  │
   │ của     │            │  │ UC-10 Xem lại booking          │  │
   │ Khách)  ├────────────┼─►│ UC-11 Thanh toán mô phỏng      │  │
   │         │            │  │ UC-12 Xem vé điện tử           │  │
   │         ├────────────┼─►│ UC-13 Xem danh sách booking    │  │
   │         │            │  │ UC-14 Xem chi tiết booking     │  │
   │         ├────────────┼─►│ UC-15 Hủy booking              │  │
   └─────────┘            │  └────────────────────────────────┘  │
                          │                                      │
   ┌─────────┐            │  ┌────────────────────────────────┐  │
   │  ADMIN  ├────────────┼─►│ UC-16 Xem dashboard            │  │
   │         ├────────────┼─►│ UC-17 Quản lý ga               │  │
   │         ├────────────┼─►│ UC-18 Quản lý tuyến            │  │
   │         ├────────────┼─►│ UC-19 Quản lý tàu              │  │
   │         ├────────────┼─►│ UC-20 Cấu hình toa             │  │
   │         ├────────────┼─►│ UC-21 Quản lý chuyến           │  │
   │         ├────────────┼─►│ UC-22 Xem danh sách booking    │  │
   │         ├────────────┼─►│ UC-23 Xem chi tiết booking     │  │
   │         ├────────────┼─►│ UC-24 Hủy booking (admin)      │  │
   │         ├────────────┼─►│ UC-25 Xem danh sách tài khoản  │  │
   └─────────┘            │  └────────────────────────────────┘  │
                          │                                      │
   ┌─────────┐            │  ┌────────────────────────────────┐  │
   │ HỆ      ├────────────┼─►│ Sinh Seat        (trong UC-20) │  │
   │ THỐNG   ├────────────┼─►│ Sinh TripSeat    (trong UC-21) │  │
   │(tự động)├────────────┼─►│ Sinh Ticket      (trong UC-11) │  │
   │         ├────────────┼─►│ Thu hồi chỗ hết hạn (UC-08)    │  │
   └─────────┘            │  └────────────────────────────────┘  │
                          └──────────────────────────────────────┘
```

### 17.2 Bảng danh mục

| Mã | Tên | Actor chính | Độ phức tạp | Màn hình |
| --- | --- | --- | --- | --- |
| [UC-01](#uc-01-đăng-ký-tài-khoản) | Đăng ký tài khoản | Khách | Thấp | SC-03 |
| [UC-02](#uc-02-đăng-nhập) | Đăng nhập | Khách | Thấp | SC-02 |
| [UC-03](#uc-03-đăng-xuất) | Đăng xuất | CUSTOMER, ADMIN | Thấp | mọi trang |
| [UC-04](#uc-04-tìm-chuyến) | Tìm chuyến | Khách | Thấp | SC-01 |
| [UC-05](#uc-05-xem-danh-sách-chuyến) | Xem danh sách chuyến | Khách | Trung bình | SC-04 |
| [UC-06](#uc-06-xem-chi-tiết-chuyến) | Xem chi tiết chuyến | Khách | Thấp | SC-05 |
| [UC-07](#uc-07-chọn-toa) | Chọn toa | CUSTOMER | Thấp | SC-06 |
| [UC-08](#uc-08-chọn-chỗ-và-giữ-chỗ) | Chọn chỗ và giữ chỗ | CUSTOMER | **Cao** | SC-06 |
| [UC-09](#uc-09-nhập-thông-tin-hành-khách) | Nhập thông tin hành khách | CUSTOMER | Trung bình | SC-07 |
| [UC-10](#uc-10-xem-lại-booking) | Xem lại booking | CUSTOMER | Thấp | SC-08 |
| [UC-11](#uc-11-thanh-toán-mô-phỏng) | Thanh toán mô phỏng | CUSTOMER | **Cao** | SC-09, SC-10 |
| [UC-12](#uc-12-xem-vé-điện-tử) | Xem vé điện tử | CUSTOMER | Thấp | SC-13 |
| [UC-13](#uc-13-xem-danh-sách-booking) | Xem danh sách booking | CUSTOMER | Trung bình | SC-11 |
| [UC-14](#uc-14-xem-chi-tiết-booking) | Xem chi tiết booking | CUSTOMER | Trung bình | SC-12 |
| [UC-15](#uc-15-hủy-booking) | Hủy booking | CUSTOMER | **Cao** | SC-12 |
| [UC-16](#uc-16-xem-dashboard) | Xem dashboard | ADMIN | Thấp | SC-14 |
| [UC-17](#uc-17-quản-lý-ga) | Quản lý ga | ADMIN | Thấp | SC-15 |
| [UC-18](#uc-18-quản-lý-tuyến) | Quản lý tuyến | ADMIN | Thấp | SC-16 |
| [UC-19](#uc-19-quản-lý-tàu) | Quản lý tàu | ADMIN | Thấp | SC-17 |
| [UC-20](#uc-20-cấu-hình-toa) | Cấu hình toa | ADMIN | Trung bình | SC-18 |
| [UC-21](#uc-21-quản-lý-chuyến) | Quản lý chuyến | ADMIN | Trung bình | SC-19 |
| [UC-22](#uc-22-xem-danh-sách-booking-admin) | Xem danh sách booking (admin) | ADMIN | Thấp | SC-20 |
| [UC-23](#uc-23-xem-chi-tiết-booking-admin) | Xem chi tiết booking (admin) | ADMIN | Thấp | SC-21 |
| [UC-24](#uc-24-hủy-booking-admin) | Hủy booking (admin) | ADMIN | Trung bình | SC-21 |
| [UC-25](#uc-25-xem-danh-sách-tài-khoản) | Xem danh sách tài khoản | ADMIN | Thấp | SC-22 |

Ba use case **Cao** — UC-08, UC-11, UC-15 — là nơi tập trung gần như toàn bộ rủi
ro dữ liệu của dự án. Chúng phải được làm cẩn thận nhất và có test đầy đủ nhất.

### 17.3 Cách đọc một đặc tả

Mỗi use case dưới đây có cùng cấu trúc:

| Mục | Nghĩa |
| --- | --- |
| **Actor** | Ai thực hiện |
| **Mô tả** | Một câu về mục đích |
| **Tiền điều kiện** | Phải đúng trước khi bắt đầu, nếu không thì use case không chạy được |
| **Kích hoạt** | Hành động khởi động use case |
| **Luồng chính** | Các bước khi mọi thứ suôn sẻ |
| **Luồng thay thế** | `A1`, `A2` — vẫn đạt mục tiêu nhưng đi đường khác |
| **Luồng ngoại lệ** | `E1`, `E2` — không đạt mục tiêu, hệ thống phải xử lý tử tế |
| **Hậu điều kiện** | Trạng thái hệ thống sau khi kết thúc thành công |
| **Quy tắc liên quan** | Các `BR-` phải được bảo đảm |
| **Dữ liệu tác động** | Bảng nào bị đọc hoặc ghi |

---
## 18. Đặc tả use case nhóm Xác thực

### UC-01 Đăng ký tài khoản

| Mục | Nội dung |
| --- | --- |
| **Actor** | Khách (chưa đăng nhập) |
| **Mô tả** | Tạo tài khoản `CUSTOMER` để có thể đặt vé |
| **Tiền điều kiện** | Người dùng chưa đăng nhập |
| **Kích hoạt** | Bấm "Đăng ký" trên navbar hoặc từ trang đăng nhập |
| **Hậu điều kiện** | Có một bản ghi `User` mới với `role = CUSTOMER`, mật khẩu đã hash |

**Luồng chính**

1. Hệ thống hiển thị form: họ tên, email, mật khẩu, nhập lại mật khẩu.
2. Khách nhập thông tin và bấm "Đăng ký".
3. Hệ thống kiểm tra: các trường bắt buộc không rỗng, email đúng định dạng, mật
   khẩu đủ độ dài, hai lần nhập mật khẩu khớp nhau.
4. Hệ thống kiểm tra email chưa tồn tại trong `User`.
5. Hệ thống hash mật khẩu (BR-28) và lưu `User` với `role = CUSTOMER` (BR-29).
6. Hệ thống chuyển sang trang đăng nhập kèm thông báo đăng ký thành công.

**Luồng ngoại lệ**

- `E1` — **Email đã tồn tại.** Ở bước 4, hệ thống giữ nguyên trang, hiện lỗi ngay
  dưới ô email, giữ lại các giá trị đã nhập trừ mật khẩu. Quay về bước 2.
- `E2` — **Dữ liệu không hợp lệ.** Ở bước 3, hệ thống hiện lỗi dưới đúng ô sai.
  Quay về bước 2.
- `E3` — **Hai mật khẩu không khớp.** Hiện lỗi dưới ô nhập lại. Quay về bước 2.

**Quy tắc liên quan:** BR-28, BR-29 · **Dữ liệu:** ghi `User`

**Không làm:** xác thực email, đăng nhập Google, OAuth, chọn vai trò khi đăng ký.

---

### UC-02 Đăng nhập

| Mục | Nội dung |
| --- | --- |
| **Actor** | Khách (chưa đăng nhập) |
| **Mô tả** | Xác thực để dùng các chức năng cần đăng nhập |
| **Tiền điều kiện** | Đã có tài khoản |
| **Kích hoạt** | Bấm "Đăng nhập", hoặc bị chuyển hướng khi truy cập trang cần quyền |
| **Hậu điều kiện** | Session được tạo, hệ thống biết `User` và `role` hiện tại |

**Luồng chính**

1. Hệ thống hiển thị form email và mật khẩu.
2. Khách nhập và bấm "Đăng nhập".
3. Hệ thống tìm `User` theo email, so khớp mật khẩu với bản hash.
4. Hệ thống tạo session và ghi nhận vai trò.
5. Hệ thống chuyển hướng: về trang người dùng định vào trước đó nếu có, ngược lại
   về trang chủ.

**Luồng thay thế**

- `A1` — **Bị chuyển hướng từ một trang cần quyền.** Ở bước 5, hệ thống đưa người
  dùng trở lại đúng trang đó thay vì trang chủ. Quan trọng với luồng đặt vé: người
  dùng đang chọn chỗ mà bị đá về trang chủ sẽ phải làm lại từ đầu.
- `A2` — **Đăng nhập bằng tài khoản ADMIN.** Bước 5 chuyển về `/admin`.

**Luồng ngoại lệ**

- `E1` — **Sai email hoặc mật khẩu.** Hệ thống hiện **một thông báo chung**:
  "Email hoặc mật khẩu không đúng". Không nói cái nào sai, để không lộ email nào
  đã tồn tại trong hệ thống. Quay về bước 2.

**Quy tắc liên quan:** BR-28 · **Dữ liệu:** đọc `User`

**Không làm:** quên mật khẩu, đặt lại mật khẩu, ghi nhớ đăng nhập lâu dài.

---

### UC-03 Đăng xuất

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER, ADMIN |
| **Mô tả** | Kết thúc phiên làm việc |
| **Tiền điều kiện** | Đang đăng nhập |
| **Kích hoạt** | Bấm "Đăng xuất" trên navbar |
| **Hậu điều kiện** | Session bị hủy; các trang cần quyền không truy cập được nữa |

**Luồng chính**

1. Người dùng bấm "Đăng xuất".
2. Hệ thống hủy session.
3. Hệ thống chuyển về trang chủ với navbar ở trạng thái chưa đăng nhập.

**Luồng ngoại lệ**

- `E1` — **Đang giữ chỗ khi đăng xuất.** Các `TripSeat` đang `HELD` bởi người này
  vẫn giữ nguyên cho tới khi `heldUntil` hết hạn, rồi tự trở về `AVAILABLE`
  (BR-14). Hệ thống không cần trả chỗ ngay lúc đăng xuất, nhưng nên cảnh báo
  người dùng rằng chỗ đang giữ sẽ mất.

**Quy tắc liên quan:** BR-14 · **Dữ liệu:** không ghi

---

## 19. Đặc tả use case nhóm Đặt vé

### UC-04 Tìm chuyến

| Mục | Nội dung |
| --- | --- |
| **Actor** | Khách, CUSTOMER |
| **Mô tả** | Nhập điều kiện để tìm các chuyến phù hợp |
| **Tiền điều kiện** | Không có |
| **Kích hoạt** | Điền form tìm chuyến trên landing và bấm "Tìm chuyến" |
| **Hậu điều kiện** | Hệ thống chuyển sang danh sách chuyến kèm điều kiện tìm kiếm |

**Luồng chính**

1. Hệ thống hiển thị form: ga đi, ga đến, ngày đi, số hành khách.
2. Hệ thống điền sẵn ngày hiện tại và chặn chọn ngày quá khứ.
3. Người dùng chọn ga đi, ga đến, ngày, số hành khách.
4. Người dùng bấm "Tìm chuyến".
5. Hệ thống kiểm tra: ga đi và ga đến bắt buộc và khác nhau (BR-10), ngày không
   ở quá khứ (BR-11), số hành khách lớn hơn 0 (BR-12).
6. Hệ thống chuyển sang UC-05 với các điều kiện đã nhập.

**Luồng thay thế**

- `A1` — **Chọn tuyến phổ biến.** Người dùng bấm một card ở khối "Tuyến phổ
  biến"; hệ thống điền sẵn ga đi và ga đến vào form. Về bước 3 để chọn ngày.
- `A2` — **Đổi chiều.** Người dùng bấm nút đổi chiều, hệ thống hoán đổi giá trị
  hai ô ga. Về bước 3.

**Luồng ngoại lệ**

- `E1` — **Ga đi trùng ga đến.** Hệ thống không tìm kiếm, hiện lỗi dưới form.
  Quay về bước 3.
- `E2` — **Ngày ở quá khứ.** Hệ thống không tìm kiếm, hiện lỗi. Quay về bước 3.
- `E3` — **Thiếu trường bắt buộc.** Hiện lỗi dưới ô còn trống. Quay về bước 3.

**Quy tắc liên quan:** BR-10, BR-11, BR-12 · **Dữ liệu:** đọc `Station`

**Không làm:** filter theo giá, loại toa, giờ, thời gian di chuyển; sorting nâng cao.

---

### UC-05 Xem danh sách chuyến

| Mục | Nội dung |
| --- | --- |
| **Actor** | Khách, CUSTOMER |
| **Mô tả** | Xem các chuyến khớp điều kiện tìm kiếm để chọn một chuyến |
| **Tiền điều kiện** | Đã có điều kiện tìm kiếm hợp lệ từ UC-04 |
| **Kích hoạt** | Kết thúc UC-04 |
| **Hậu điều kiện** | Người dùng chọn được một chuyến, hoặc quay lại đổi điều kiện tìm |

**Luồng chính**

1. Hệ thống tìm các `Trip` thỏa mãn: đúng `Route` giữa hai ga đã chọn, đúng
   `departureDate`, `status = SCHEDULED`, và chưa khởi hành (BR-24).
2. Với mỗi chuyến, hệ thống tính **số chỗ còn lại** — đếm `TripSeat` có
   `status = AVAILABLE` — và **giá thấp nhất** — `basePrice` cộng
   `priceModifier` nhỏ nhất trong các toa của tàu.
3. Hệ thống hiển thị mỗi chuyến với: mã tàu, ga đi, ga đến, giờ khởi hành, giờ
   đến, thời gian di chuyển, giá thấp nhất, số chỗ còn lại.
4. Người dùng bấm "Chọn chuyến" trên một chuyến.
5. Hệ thống chuyển sang UC-06.

**Luồng thay thế**

- `A1` — **Chuyến không còn đủ chỗ cho số hành khách đã nhập.** Hệ thống vẫn hiển
  thị chuyến nhưng vô hiệu nút chọn và gắn nhãn giải thích.
- `A2` — **Đổi điều kiện tìm.** Người dùng bấm "Đổi tìm kiếm", hệ thống quay về
  UC-04 với các giá trị cũ đã điền sẵn.

**Luồng ngoại lệ**

- `E1` — **Không có chuyến nào.** Hệ thống hiện khối rỗng nói rõ không tìm thấy
  chuyến cho tuyến và ngày đó, kèm gợi ý đổi ngày. Không hiện danh sách trống
  không giải thích.
- `E2` — **Tham số tìm kiếm không hợp lệ khi vào bằng URL trực tiếp.** Hệ thống
  đưa về landing kèm thông báo, không hiện trang lỗi.

**Quy tắc liên quan:** BR-23, BR-24 · **Dữ liệu:** đọc `Trip`, `Route`,
`Station`, `Train`, `Coach`, `TripSeat`

---

### UC-06 Xem chi tiết chuyến

| Mục | Nội dung |
| --- | --- |
| **Actor** | Khách, CUSTOMER |
| **Mô tả** | Xem thông tin đầy đủ của một chuyến và các toa để quyết định |
| **Tiền điều kiện** | Chuyến tồn tại |
| **Kích hoạt** | Bấm "Chọn chuyến" ở UC-05, hoặc vào bằng URL |
| **Hậu điều kiện** | Người dùng chuyển sang bước chọn chỗ, hoặc quay lại danh sách |

**Luồng chính**

1. Hệ thống tải `Trip` cùng `Train`, `Route`, hai `Station`.
2. Hệ thống liệt kê các `Coach` của tàu, mỗi toa kèm: số toa, loại toa, số chỗ
   còn trống trong chuyến này, và giá vé của toa = `basePrice + priceModifier`
   (BR-18).
3. Hệ thống hiển thị thông tin chuyến và danh sách toa.
4. Người dùng bấm "Chọn chỗ".
5. Nếu chưa đăng nhập, hệ thống chuyển sang UC-02 và quay lại đây sau khi đăng
   nhập xong (UC-02 `A1`). Nếu đã đăng nhập, chuyển sang UC-07.

**Luồng ngoại lệ**

- `E1` — **Chuyến không tồn tại.** Hệ thống hiện trang 404 tử tế có nút về trang
  chủ.
- `E2` — **Chuyến đã bị hủy** (`status = CANCELLED`). Hệ thống hiện thông báo rõ
  và ẩn nút chọn chỗ (BR-23).
- `E3` — **Chuyến đã khởi hành.** Hệ thống hiện thông báo và ẩn nút chọn chỗ
  (BR-24).
- `E4` — **Tàu chưa có toa nào.** Hệ thống hiện thông báo chuyến chưa mở bán. Đây
  là dấu hiệu Admin tạo Trip cho tàu chưa cấu hình toa.

**Quy tắc liên quan:** BR-18, BR-23, BR-24 · **Dữ liệu:** đọc `Trip`, `Train`,
`Coach`, `TripSeat`, `Route`, `Station`

---

### UC-07 Chọn toa

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Chọn toa để xem sơ đồ chỗ của toa đó |
| **Tiền điều kiện** | Đã đăng nhập; chuyến hợp lệ và còn chỗ |
| **Kích hoạt** | Vào màn hình chọn chỗ, hoặc bấm sang toa khác |
| **Hậu điều kiện** | Sơ đồ chỗ của toa đang chọn được hiển thị |

**Luồng chính**

1. Hệ thống hiển thị danh sách toa dạng tab, kèm loại toa và số chỗ còn.
2. Hệ thống chọn sẵn toa đầu tiên còn chỗ.
3. Người dùng bấm một toa khác.
4. Hệ thống tải trạng thái `TripSeat` của toa đó và vẽ lại sơ đồ (UC-08).

**Luồng thay thế**

- `A1` — **Đã chọn chỗ ở toa trước rồi đổi toa.** Các chỗ đang giữ **vẫn được
  giữ**, không bị trả về. Panel "Đã chọn" hiển thị chỗ ở nhiều toa khác nhau. Đây
  là hành vi đúng: một booking có thể gồm chỗ ở các toa khác nhau của cùng chuyến.

**Luồng ngoại lệ**

- `E1` — **Toa đã hết chỗ.** Tab vẫn hiện nhưng bị vô hiệu, có nhãn "Hết chỗ".

**Quy tắc liên quan:** không có quy tắc riêng · **Dữ liệu:** đọc `Coach`, `Seat`,
`TripSeat`

---

### UC-08 Chọn chỗ và giữ chỗ

**Đây là use case phức tạp nhất hệ thống.** Mọi lỗi trùng chỗ đều bắt nguồn từ
đây.

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Chọn chỗ trên sơ đồ và giữ tạm trong 5 phút để kịp hoàn tất booking |
| **Tiền điều kiện** | Đã đăng nhập; chuyến `SCHEDULED` và chưa khởi hành; toa đã chọn |
| **Kích hoạt** | Bấm vào một ô chỗ trên sơ đồ |
| **Hậu điều kiện** | Các chỗ đã chọn ở trạng thái `HELD` với `heldBy` là người này và `heldUntil` là 5 phút sau |

**Luồng chính**

1. Hệ thống vẽ sơ đồ chỗ của toa đang chọn, mỗi ô mang trạng thái `AVAILABLE`,
   `HELD` hoặc `BOOKED` với ký hiệu phân biệt rõ.
2. Người dùng bấm một ô đang `AVAILABLE`.
3. Hệ thống gửi yêu cầu giữ chỗ tới backend.
4. Backend **đọc lại** `TripSeat` từ database (BR-09) và kiểm tra: chuyến vẫn
   hợp lệ, chỗ vẫn `AVAILABLE`.
5. Backend đổi `status` sang `HELD`, ghi `heldBy` = người dùng hiện tại và
   `heldUntil` = thời điểm hiện tại cộng 5 phút (BR-13).
6. Giao diện đánh dấu ô là đã chọn, thêm vào panel "Đã chọn", và bắt đầu đếm ngược
   nếu đây là chỗ đầu tiên.
7. Người dùng lặp bước 2 tới bước 6 cho đủ số hành khách.
8. Người dùng bấm "Tiếp tục", hệ thống chuyển sang UC-09.

**Luồng thay thế**

- `A1` — **Bỏ chọn một chỗ.** Người dùng bấm lại ô đã chọn hoặc bấm dấu xóa ở
  panel. Backend trả `TripSeat` về `AVAILABLE`, xóa `heldBy` và `heldUntil`.
- `A2` — **Đổi sang toa khác.** Xem UC-07 `A1`. Chỗ đã giữ không bị mất.
- `A3` — **Chọn đủ số hành khách.** Hệ thống chặn chọn thêm và giải thích đã đủ
  chỗ (BR-16).

**Luồng ngoại lệ**

- `E1` — **Chỗ vừa bị người khác giữ.** Ở bước 4, backend thấy `status` không còn
  là `AVAILABLE`. Hệ thống từ chối, cập nhật lại sơ đồ theo trạng thái thật, và
  báo cho người dùng chỗ vừa có người chọn (BR-08). Quay về bước 2.
- `E2` — **Hết thời gian giữ chỗ.** `heldUntil` đã qua. Hệ thống trả toàn bộ chỗ
  đang giữ về `AVAILABLE` (BR-14), hiện thông báo, và đưa người dùng về bước 1
  với sơ đồ đã làm mới.
- `E3` — **Chuyến bị hủy trong lúc đang chọn.** Hệ thống chặn thao tác, trả chỗ,
  đưa về UC-06 kèm thông báo (BR-23).
- `E4` — **Bấm "Tiếp tục" khi chưa chọn đủ chỗ.** Hệ thống chặn, nói rõ còn thiếu
  mấy chỗ (BR-16).
- `E5` — **Vi phạm ràng buộc database.** Nếu hai yêu cầu giữ chỗ vào cùng lúc và
  cùng qua được bước 4, ràng buộc unique `(tripId, seatId)` hoặc cập nhật có điều
  kiện sẽ chặn một trong hai (BR-06). Backend phải bắt lỗi này và xử lý như `E1`,
  **không** để lỗi database văng ra màn hình.

**Quy tắc liên quan:** BR-06, BR-08, BR-09, BR-13, BR-14, BR-16, BR-23 ·
**Dữ liệu:** đọc `Trip`, `Coach`, `Seat`; đọc và ghi `TripSeat`

**Ghi chú kỹ thuật:** phần này dùng JavaScript thuần gọi endpoint
`@RestController` dưới `/api/...` trả JSON. Phiên bản đầu **không** dùng
WebSocket; giao diện chỉ hỏi lại trạng thái khi cần.

---

### UC-09 Nhập thông tin hành khách

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Nhập thông tin từng hành khách, mỗi người gắn với một chỗ đã giữ |
| **Tiền điều kiện** | Đã giữ đủ số chỗ bằng số hành khách; chỗ chưa hết hạn giữ |
| **Kích hoạt** | Bấm "Tiếp tục" ở UC-08 |
| **Hậu điều kiện** | Thông tin hành khách đã nhập đủ và hợp lệ, sẵn sàng cho bước review |

**Luồng chính**

1. Hệ thống hiển thị một khối form cho mỗi chỗ đã giữ, ghi rõ chỗ nào thuộc form
   nào, kèm đồng hồ đếm ngược thời gian giữ chỗ.
2. Người dùng nhập họ tên, ngày sinh, CCCD hoặc passport cho từng hành khách.
3. Người dùng bấm "Tiếp tục".
4. Hệ thống kiểm tra: mọi trường bắt buộc không rỗng, số form bằng số chỗ đã giữ
   (BR-16).
5. Hệ thống lưu tạm thông tin và chuyển sang UC-10.

**Luồng thay thế**

- `A1` — **Quay lại đổi chỗ.** Người dùng bấm "Quay lại"; hệ thống về UC-08, giữ
  nguyên các chỗ đang `HELD` và giữ lại thông tin đã nhập nếu chỗ không đổi.

**Luồng ngoại lệ**

- `E1` — **Thiếu trường bắt buộc.** Hệ thống hiện lỗi dưới đúng ô sai, **không
  mất** dữ liệu đã nhập ở các ô khác. Quay về bước 2.
- `E2` — **Hết thời gian giữ chỗ khi đang nhập.** Hệ thống hiện cảnh báo, trả chỗ
  về `AVAILABLE` (BR-14), và đưa về UC-08. Thông tin đã nhập nên được giữ lại để
  người dùng không phải gõ lại nếu chọn được chỗ mới.
- `E3` — **Số hành khách khác số chỗ.** Không thể xảy ra từ giao diện, nhưng
  backend vẫn phải kiểm tra vì request có thể được gửi thẳng (BR-16, NFR-02).

**Quy tắc liên quan:** BR-16, BR-17, BR-14 · **Dữ liệu:** đọc `TripSeat`; chuẩn
bị `BookingPassenger`

**Không làm:** giá theo người lớn / trẻ em, người cao tuổi, sinh viên; chính sách
giảm giá.

---

### UC-10 Xem lại booking

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Kiểm tra toàn bộ thông tin trước khi trả tiền |
| **Tiền điều kiện** | Đã nhập đủ thông tin hành khách; chỗ chưa hết hạn giữ |
| **Kích hoạt** | Bấm "Tiếp tục" ở UC-09 |
| **Hậu điều kiện** | Người dùng xác nhận thông tin đúng và chuyển sang thanh toán |

**Luồng chính**

1. Hệ thống hiển thị: tuyến, ngày giờ đi, tàu; danh sách hành khách kèm toa, chỗ
   và giá từng vé; tổng tiền; đồng hồ đếm ngược.
2. Hệ thống tính giá từng vé theo công thức `basePrice + priceModifier` (BR-18)
   và tổng tiền bằng tổng các vé.
3. Người dùng kiểm tra và bấm "Thanh toán".
4. Hệ thống chuyển sang UC-11.

**Luồng thay thế**

- `A1` — **Quay lại sửa thông tin hành khách.** Bấm "Quay lại" về UC-09, dữ liệu
  đã nhập được giữ nguyên.

**Luồng ngoại lệ**

- `E1` — **Hết thời gian giữ chỗ.** Hệ thống chặn nút thanh toán, giải thích, đưa
  về UC-08 (BR-14).
- `E2` — **Một chỗ đã bị mất.** Hệ thống nêu rõ chỗ nào và đưa về UC-08.

**Quy tắc liên quan:** BR-18, BR-19, BR-14 · **Dữ liệu:** đọc `Trip`, `Coach`,
`TripSeat`

---

### UC-11 Thanh toán mô phỏng

**Use case rủi ro nhất về dữ liệu.** Bốn thay đổi trạng thái phải cùng thành công
hoặc cùng thất bại.

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Xác nhận booking bằng thanh toán mô phỏng và nhận vé |
| **Tiền điều kiện** | Đã qua review; các chỗ vẫn `HELD` bởi chính người này và chưa hết hạn |
| **Kích hoạt** | Bấm "Xác nhận thanh toán" |
| **Hậu điều kiện** | `Booking` `CONFIRMED` và `PAID`; các `TripSeat` `BOOKED`; mỗi hành khách có một `Ticket` |

**Luồng chính**

1. Hệ thống hiển thị mã booking, số vé, tổng tiền, và ghi chú rõ đây là thanh
   toán mô phỏng, không có giao dịch thật.
2. Người dùng bấm "Xác nhận thanh toán".
3. Giao diện vô hiệu nút và hiện trạng thái đang xử lý, để chặn bấm hai lần.
4. Backend **mở transaction** và kiểm tra lại (BR-22):
   - chuyến vẫn `SCHEDULED` và chưa khởi hành;
   - mọi `TripSeat` liên quan vẫn `HELD`;
   - `heldBy` đúng là người đang đăng nhập;
   - `heldUntil` chưa qua.
5. Trong cùng transaction, backend thực hiện bốn việc (BR-20):
   - `Booking.paymentStatus`: `UNPAID` → `PAID`, ghi `paidAt`;
   - `Booking.bookingStatus`: `PENDING` → `CONFIRMED` (BR-21);
   - mọi `TripSeat`: `HELD` → `BOOKED`, xóa `heldBy` và `heldUntil`;
   - sinh một `Ticket` cho mỗi `BookingPassenger`.
6. Backend commit transaction.
7. Hệ thống chuyển sang trang booking thành công, hiển thị `bookingCode`.

**Luồng thay thế**

- `A1` — **Người dùng bấm Hủy bỏ.** Hệ thống không tạo booking, trả chỗ về
  `AVAILABLE`, đưa về danh sách chuyến.

**Luồng ngoại lệ**

Mọi ngoại lệ dưới đây đều **rollback toàn bộ transaction**. Không có ngoại lệ nào
được để lại dữ liệu nửa vời.

- `E1` — **Một chỗ không còn `HELD`.** Ở bước 4, rollback, hiện thông báo nêu rõ
  chỗ nào, đưa về UC-08.
- `E2` — **`heldUntil` đã qua.** Rollback, trả chỗ về `AVAILABLE`, đưa về UC-08
  (BR-14).
- `E3` — **`heldBy` không phải người đang đăng nhập.** Rollback và trả 403. Đây là
  dấu hiệu request bị giả mạo.
- `E4` — **Chuyến bị hủy giữa chừng.** Rollback, thông báo, đưa về UC-05 (BR-23).
- `E5` — **Lỗi khi ghi database.** Rollback, hiện thông báo lỗi chung, ghi log để
  điều tra. Người dùng không mất chỗ đang giữ nếu chưa hết hạn.
- `E6` — **Người dùng bấm hai lần.** Bước 3 chặn ở giao diện; backend cũng phải
  chịu được: yêu cầu thứ hai thấy booking đã `CONFIRMED` thì trả về kết quả cũ,
  không tạo booking thứ hai.

**Quy tắc liên quan:** BR-20, BR-21, BR-22, BR-23, BR-14 · **Dữ liệu:** ghi
`Booking`, `BookingPassenger`, `TripSeat`, `Ticket`

**Hai trạng thái tuyệt đối không được tồn tại sau use case này:**

```
❌ Booking CONFIRMED  và  TripSeat vẫn AVAILABLE
❌ TripSeat BOOKED    và  Booking chưa được xác nhận
```

**Không làm:** kết nối ngân hàng, cổng thanh toán, form nhập thẻ, callback,
webhook, hoàn tiền thật.

---

### UC-12 Xem vé điện tử

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Xem vé của một hành khách trên website |
| **Tiền điều kiện** | Booking đã `CONFIRMED`; vé thuộc về người đang đăng nhập |
| **Kích hoạt** | Bấm "Xem vé" từ trang thành công, My Trips, hoặc chi tiết booking |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống tải `Ticket` cùng `BookingPassenger`, `Booking`, `Trip`, `Train`,
   `Route`, hai `Station`, `TripSeat`, `Seat`, `Coach`.
2. Hệ thống kiểm tra vé thuộc booking của người đang đăng nhập (BR-31).
3. Hệ thống hiển thị: mã vé, mã booking, tên hành khách, tuyến, tàu, ngày giờ
   khởi hành, toa, ghế hoặc giường, giá vé.

**Luồng thay thế**

- `A1` — **In vé.** Người dùng bấm in; bản in ẩn navbar, footer và các nút.
- `A2` — **Booking đã hủy.** Vé vẫn xem được nhưng hiển thị dấu đã hủy rõ ràng.

**Luồng ngoại lệ**

- `E1` — **Vé của người khác.** Hệ thống trả 403 và **không hiển thị bất kỳ nội
  dung nào của vé** (BR-31).
- `E2` — **Vé không tồn tại.** Trả 404 tử tế.

**Quy tắc liên quan:** BR-31 · **Dữ liệu:** đọc `Ticket` và toàn bộ chuỗi quan hệ

**Optional về sau:** QR code chỉ chứa `ticketCode` hoặc `id`, tuyệt đối không
chứa CCCD, passport hay thông tin thanh toán. Xuất PDF làm sau cùng.

---
## 20. Đặc tả use case nhóm Quản lý booking của Customer

### UC-13 Xem danh sách booking

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Xem toàn bộ booking của chính mình, chia theo ba nhóm |
| **Tiền điều kiện** | Đã đăng nhập |
| **Kích hoạt** | Bấm "Chuyến của tôi" trên navbar |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống lấy các `Booking` có `customerId` bằng người đang đăng nhập (BR-31).
2. Hệ thống xếp booking vào ba nhóm:
   - **Sắp đi** — `bookingStatus = CONFIRMED` và chuyến chưa khởi hành;
   - **Đã hoàn thành** — `CONFIRMED` và `Trip.status = COMPLETED`;
   - **Đã hủy** — `bookingStatus = CANCELLED`.
3. Với mỗi booking, hệ thống hiển thị: mã booking, tuyến, ngày đi, tàu, toa, các
   chỗ, trạng thái booking và trạng thái thanh toán.
4. Người dùng chọn một hành động: xem chi tiết (UC-14), xem vé (UC-12), hoặc hủy
   (UC-15).

**Luồng thay thế**

- `A1` — **Chuyển tab.** Người dùng bấm sang nhóm khác; hệ thống hiển thị danh
  sách của nhóm đó.

**Luồng ngoại lệ**

- `E1` — **Chưa có booking nào.** Hệ thống hiện khối rỗng kèm nút "Tìm chuyến
  ngay", không hiện bảng trống.
- `E2` — **Một nhóm rỗng.** Hiện thông báo riêng cho nhóm đó, ví dụ "Bạn chưa có
  chuyến nào sắp đi".

**Quy tắc liên quan:** BR-31 · **Dữ liệu:** đọc `Booking`, `Trip`, `Route`,
`Station`, `Train`, `TripSeat`, `Seat`, `Coach`

**Không làm:** filter, tìm kiếm nâng cao.

---

### UC-14 Xem chi tiết booking

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Xem đầy đủ một booking cùng danh sách hành khách và vé |
| **Tiền điều kiện** | Đã đăng nhập; booking thuộc về người này |
| **Kích hoạt** | Bấm "Xem chi tiết" ở UC-13, hoặc vào bằng URL |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống tải `Booking` theo id.
2. Hệ thống kiểm tra `booking.customerId` khớp người đang đăng nhập (BR-31).
3. Hệ thống hiển thị: mã booking, trạng thái booking và thanh toán, thông tin
   chuyến, danh sách hành khách kèm toa, chỗ, giá vé, tổng tiền, thời điểm đặt.
4. Mỗi hành khách có nút xem vé; booking đủ điều kiện thì có nút hủy.

**Luồng ngoại lệ**

- `E1` — **Booking của người khác.** Hệ thống trả **403, không phải 404**, và
  tuyệt đối không hiển thị nội dung booking (BR-31).
- `E2` — **Booking không tồn tại.** Trả 404 tử tế.
- `E3` — **Booking đã hủy.** Vẫn hiển thị nhưng ẩn nút hủy, gắn nhãn đã hủy.
- `E4` — **Chuyến đã khởi hành.** Ẩn nút hủy (BR-25).

**Quy tắc liên quan:** BR-25, BR-31 · **Dữ liệu:** đọc `Booking`,
`BookingPassenger`, `Ticket`, `TripSeat`, `Seat`, `Coach`, `Trip`, `Route`,
`Station`, `Train`

**Ghi chú:** đây là chỗ hay quên kiểm tra quyền sở hữu nhất trong đồ án. Bắt buộc
có test.

---

### UC-15 Hủy booking

| Mục | Nội dung |
| --- | --- |
| **Actor** | CUSTOMER |
| **Mô tả** | Hủy booking của mình và trả chỗ về thị trường |
| **Tiền điều kiện** | Booking `CONFIRMED`; chuyến chưa khởi hành; booking thuộc về người này |
| **Kích hoạt** | Bấm "Hủy booking" ở UC-13 hoặc UC-14 |
| **Hậu điều kiện** | `Booking` `CANCELLED`; mọi `TripSeat` liên quan `AVAILABLE` |

**Luồng chính**

1. Người dùng bấm "Hủy booking".
2. Hệ thống hiện hộp thoại xác nhận, nói rõ hành động không thể hoàn tác và
   không có hoàn tiền.
3. Người dùng xác nhận.
4. Backend **mở transaction** và kiểm tra: booking thuộc người này (BR-31),
   `bookingStatus = CONFIRMED`, chuyến chưa khởi hành (BR-25).
5. Trong cùng transaction (BR-27):
   - `Booking.bookingStatus`: `CONFIRMED` → `CANCELLED`;
   - mọi `TripSeat` của booking: `BOOKED` → `AVAILABLE` (BR-26).
6. Backend commit.
7. Hệ thống hiện thông báo đã hủy và cập nhật lại danh sách.

**Luồng thay thế**

- `A1` — **Người dùng đóng hộp thoại.** Không có gì thay đổi.

**Luồng ngoại lệ**

Mọi ngoại lệ đều rollback toàn bộ transaction.

- `E1` — **Booking không thuộc người này.** Trả 403 (BR-31).
- `E2` — **Booking không ở trạng thái `CONFIRMED`.** Từ chối và giải thích: đã hủy
  rồi, hoặc chưa thanh toán.
- `E3` — **Chuyến đã khởi hành.** Từ chối và giải thích (BR-25).
- `E4` — **Lỗi khi ghi database.** Rollback, giữ nguyên booking, báo lỗi.

**Quy tắc liên quan:** BR-25, BR-26, BR-27, BR-31 · **Dữ liệu:** ghi `Booking`,
`TripSeat`

**Ghi chú:** `Ticket` **không bị xóa**. Vé vẫn tra cứu được nhưng hiển thị dấu đã
hủy, để giữ lịch sử.

**Không làm:** phí hủy, hoàn tiền, quy định 24h hay 48h.

---

## 21. Đặc tả use case nhóm Quản trị

### UC-16 Xem dashboard

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem bốn chỉ số tổng quan của hệ thống |
| **Tiền điều kiện** | Đã đăng nhập với vai trò `ADMIN` |
| **Kích hoạt** | Vào `/admin` |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống kiểm tra vai trò `ADMIN` (BR-30).
2. Hệ thống tính bốn con số: tổng booking, tổng chuyến, tổng người dùng, tổng
   doanh thu (tổng `totalPrice` của các booking `CONFIRMED` và `PAID`).
3. Hệ thống hiển thị bốn thẻ số và danh sách booking gần đây.

**Luồng ngoại lệ**

- `E1` — **Người dùng không phải ADMIN.** Trả 403 (BR-30).
- `E2` — **Hệ thống chưa có dữ liệu.** Hiển thị số `0`, không hiện trang trắng.

**Quy tắc liên quan:** BR-30 · **Dữ liệu:** đọc `Booking`, `Trip`, `User`

**Không làm:** biểu đồ phức tạp, analytics nâng cao.

---

### UC-17 Quản lý ga

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem, thêm, sửa, xóa ga tàu |
| **Tiền điều kiện** | Đã đăng nhập với vai trò `ADMIN` |
| **Kích hoạt** | Vào `/admin/stations` |
| **Hậu điều kiện** | Dữ liệu `Station` phản ánh thao tác vừa thực hiện |

**Luồng chính**

1. Hệ thống hiển thị bảng ga: mã, tên, thành phố, thao tác.
2. Admin bấm "Thêm ga"; hệ thống mở form: tên, mã, thành phố, địa chỉ.
3. Admin nhập và lưu.
4. Hệ thống kiểm tra: tên không rỗng, mã không rỗng và chưa tồn tại (BR-01).
5. Hệ thống lưu và cập nhật bảng.

**Luồng thay thế**

- `A1` — **Sửa ga.** Bấm "Sửa", form mở với dữ liệu hiện tại. Kiểm tra như bước 4,
  trừ việc mã trùng với chính nó thì hợp lệ.
- `A2` — **Xóa ga.** Bấm "Xóa", hệ thống hỏi xác nhận rồi kiểm tra ga chưa được
  `Route` nào dùng (BR-33) trước khi xóa.

**Luồng ngoại lệ**

- `E1` — **Mã ga đã tồn tại.** Từ chối, hiện lỗi dưới ô mã, không đóng form
  (BR-01).
- `E2` — **Tên để trống.** Hiện lỗi dưới ô tên.
- `E3` — **Ga đang được tuyến sử dụng.** Chặn xóa và nói rõ đang được dùng ở đâu
  (BR-33).
- `E4` — **Chưa có ga nào.** Hiện khối rỗng kèm nút thêm.

**Quy tắc liên quan:** BR-01, BR-30, BR-33 · **Dữ liệu:** đọc và ghi `Station`;
đọc `Route` khi xóa

---

### UC-18 Quản lý tuyến

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem, thêm, sửa, xóa tuyến giữa hai ga |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN`; đã có ít nhất hai ga |
| **Kích hoạt** | Vào `/admin/routes` |
| **Hậu điều kiện** | Dữ liệu `Route` phản ánh thao tác vừa thực hiện |

**Luồng chính**

1. Hệ thống hiển thị bảng tuyến: ga đi, ga đến, khoảng cách, thao tác.
2. Admin bấm "Thêm tuyến"; form có hai ô chọn ga và ô khoảng cách.
3. Admin chọn và lưu.
4. Hệ thống kiểm tra ga đi khác ga đến (BR-02) và cả hai ga đều tồn tại.
5. Hệ thống lưu và cập nhật bảng.

**Luồng thay thế**

- `A1` — **Sửa tuyến.** Như bước 2 đến 5 với dữ liệu hiện tại.
- `A2` — **Xóa tuyến.** Xác nhận rồi kiểm tra tuyến chưa có `Trip` nào.

**Luồng ngoại lệ**

- `E1` — **Ga đi trùng ga đến.** Từ chối, hiện lỗi (BR-02).
- `E2` — **Chưa có ga nào trong hệ thống.** Chặn tạo tuyến, chỉ Admin sang trang
  quản lý ga.
- `E3` — **Tuyến đang có chuyến.** Chặn xóa, nói rõ số chuyến đang dùng.

**Quy tắc liên quan:** BR-02, BR-30 · **Dữ liệu:** đọc và ghi `Route`; đọc
`Station`, `Trip`

**Không làm:** ga trung gian, `RouteStation`.

---

### UC-19 Quản lý tàu

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem, thêm, sửa tàu |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN` |
| **Kích hoạt** | Vào `/admin/trains` |
| **Hậu điều kiện** | Dữ liệu `Train` phản ánh thao tác vừa thực hiện |

**Luồng chính**

1. Hệ thống hiển thị bảng tàu: mã tàu, tên, số toa, thao tác.
2. Admin bấm "Thêm tàu"; form có mã tàu và tên.
3. Admin nhập và lưu.
4. Hệ thống kiểm tra mã tàu chưa tồn tại (BR-03).
5. Hệ thống lưu và cập nhật bảng.

**Luồng thay thế**

- `A1` — **Sửa tàu.** Như trên với dữ liệu hiện tại.
- `A2` — **Vào chi tiết tàu.** Bấm "Chi tiết" chuyển sang UC-20.

**Luồng ngoại lệ**

- `E1` — **Mã tàu đã tồn tại.** Từ chối, hiện lỗi (BR-03).
- `E2` — **Tàu đang có chuyến.** Chặn xóa.

**Quy tắc liên quan:** BR-03, BR-30 · **Dữ liệu:** đọc và ghi `Train`; đọc
`Coach`, `Trip`

---

### UC-20 Cấu hình toa

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN; **Hệ thống** sinh `Seat` |
| **Mô tả** | Thêm toa cho tàu; hệ thống tự sinh đủ chỗ theo sức chứa |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN`; tàu đã tồn tại |
| **Kích hoạt** | Vào `/admin/trains/:id` và bấm "Thêm toa" |
| **Hậu điều kiện** | Có `Coach` mới và đủ `Seat` tương ứng `capacity` |

**Luồng chính**

1. Hệ thống hiển thị danh sách toa hiện có của tàu, mỗi toa kèm danh sách mã chỗ.
2. Admin bấm "Thêm toa"; form có: số toa, loại toa (`SEAT` / `SLEEPER`),
   `capacity`, `priceModifier`.
3. Admin nhập và lưu.
4. Hệ thống kiểm tra `capacity` lớn hơn 0 và `priceModifier` không âm.
5. Hệ thống lưu `Coach`.
6. **Hệ thống sinh `Seat`** theo `capacity` (BR-04):
   - `SEAT` → mã `A01`, `A02`, ..., `type = SEAT`, `cabinNumber` để trống;
   - `SLEEPER` → mã `B01`, `B02`, ..., `type = BED`, gán `cabinNumber` theo nhóm
     4 giường.
7. Hệ thống hiển thị lại toa vừa tạo **kèm danh sách mã chỗ vừa sinh**, để Admin
   thấy bước tự động đã chạy đúng.

**Luồng thay thế**

- `A1` — **Sửa toa.** Cho sửa `coachNumber` và `priceModifier`. **Không cho sửa
  `capacity`** nếu toa đã có `TripSeat` được đặt, vì sẽ làm lệch số chỗ của các
  chuyến đang bán.
- `A2` — **Xóa toa.** Chỉ khi chưa có `TripSeat` nào của toa này ở trạng thái
  `BOOKED`.

**Luồng ngoại lệ**

- `E1` — **`capacity` không hợp lệ.** Từ chối, hiện lỗi.
- `E2` — **Toa `SLEEPER` có `capacity` không chia hết cho 4.** Hệ thống cảnh báo
  cabin cuối sẽ không đủ 4 giường, cho Admin quyết định tiếp tục hay sửa.
- `E3` — **Toa đã có chỗ được đặt.** Chặn xóa và chặn sửa `capacity`.

**Quy tắc liên quan:** BR-04, BR-30 · **Dữ liệu:** ghi `Coach`, `Seat`; đọc
`TripSeat` khi sửa hoặc xóa

**Ghi chú:** không có route `/admin/coaches` riêng. Toa chỉ quản lý bên trong
trang chi tiết tàu.

---

### UC-21 Quản lý chuyến

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN; **Hệ thống** sinh `TripSeat` |
| **Mô tả** | Tạo chuyến từ tàu và tuyến; hệ thống sinh chỗ cho chuyến |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN`; đã có tàu (đã cấu hình toa) và tuyến |
| **Kích hoạt** | Vào `/admin/trips` và bấm "Thêm chuyến" |
| **Hậu điều kiện** | Có `Trip` mới và đủ `TripSeat` ở trạng thái `AVAILABLE` |

**Luồng chính**

1. Hệ thống hiển thị bảng chuyến: tàu, tuyến, ngày, giờ đi, giờ đến, giá cơ bản,
   trạng thái.
2. Admin bấm "Thêm chuyến"; form có: chọn tàu, chọn tuyến, ngày khởi hành, giờ
   khởi hành, giờ đến, giá cơ bản.
3. Admin nhập và lưu.
4. Hệ thống kiểm tra: tàu và tuyến tồn tại, `basePrice` lớn hơn 0.
5. Hệ thống lưu `Trip` với `status = SCHEDULED`.
6. **Hệ thống sinh `TripSeat`** cho toàn bộ `Seat` của tàu, tất cả ở `AVAILABLE`
   (BR-05).
7. Hệ thống thông báo **đã sinh bao nhiêu chỗ**, để Admin biết bước tự động chạy
   đúng.

**Luồng thay thế**

- `A1` — **Sửa chuyến.** Cho sửa giờ và `basePrice`. Sửa `basePrice` **không**
  ảnh hưởng booking đã tạo (BR-34).
- `A2` — **Hủy chuyến.** Đổi `status` sang `CANCELLED`. Từ đó chuyến không đặt
  được nữa (BR-23). Các booking đã có của chuyến cần được xử lý — xem ghi chú.

**Luồng ngoại lệ**

- `E1` — **Tàu chưa có toa nào.** Cảnh báo chuyến sẽ có 0 chỗ và không bán được
  vé. Nên chặn tạo.
- `E2` — **Chưa có tàu hoặc tuyến.** Chặn tạo, chỉ Admin sang trang tương ứng.
- `E3` — **Ngày khởi hành ở quá khứ.** Cảnh báo vì chuyến quá khứ không đặt được
  (BR-24).
- `E4` — **Chuyến đã có booking.** Chặn xóa; chỉ cho hủy chuyến.

**Quy tắc liên quan:** BR-05, BR-23, BR-24, BR-30, BR-34 · **Dữ liệu:** ghi
`Trip`, `TripSeat`; đọc `Train`, `Coach`, `Seat`, `Route`

> **Chưa quyết:** khi Admin hủy một chuyến đã có booking `CONFIRMED`, hệ thống xử
> lý các booking đó thế nào — tự hủy hàng loạt, hay chặn không cho hủy chuyến.
> `SPEC.md` không nói. Cần chốt trước khi làm chức năng hủy chuyến.

---

### UC-22 Xem danh sách booking (admin)

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem toàn bộ booking trong hệ thống và tìm theo mã |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN` |
| **Kích hoạt** | Vào `/admin/bookings` |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống hiển thị bảng booking: mã, khách hàng, chuyến, tổng tiền, trạng thái
   booking, trạng thái thanh toán.
2. Admin nhập mã booking vào ô tìm kiếm.
3. Hệ thống lọc theo mã và hiển thị kết quả.
4. Admin bấm một dòng để sang UC-23.

**Luồng ngoại lệ**

- `E1` — **Không tìm thấy mã.** Hiện khối rỗng nói rõ đã tìm mã nào.
- `E2` — **Người dùng không phải ADMIN.** Trả 403 (BR-30).

**Quy tắc liên quan:** BR-30 · **Dữ liệu:** đọc `Booking`, `User`, `Trip`

**Khác UC-13 ở chỗ:** Admin xem được booking của **mọi người**, không chỉ của
mình.

---

### UC-23 Xem chi tiết booking (admin)

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem đầy đủ một booking bất kỳ trong hệ thống |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN` |
| **Kích hoạt** | Bấm một dòng ở UC-22 |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống kiểm tra vai trò `ADMIN` (BR-30).
2. Hệ thống hiển thị: thông tin khách hàng, chuyến, danh sách hành khách kèm chỗ
   và giá, tổng tiền, trạng thái thanh toán, thời điểm đặt.
3. Nếu booking đủ điều kiện hủy, hệ thống hiện nút hủy (UC-24).

**Luồng ngoại lệ**

- `E1` — **Booking không tồn tại.** Trả 404.
- `E2` — **Người dùng không phải ADMIN.** Trả 403.

**Quy tắc liên quan:** BR-30, BR-32 · **Dữ liệu:** đọc `Booking`,
`BookingPassenger`, `TripSeat`, `Seat`, `Coach`, `Trip`, `User`

**Admin KHÔNG được:** đổi chỗ sau khi đã đặt, sửa trạng thái thanh toán thủ công,
hoàn tiền, đổi thông tin hành khách (BR-32). Giao diện không cung cấp các chức
năng đó.

---

### UC-24 Hủy booking (admin)

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Hủy một booking bất kỳ và trả chỗ về thị trường |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN`; booking `CONFIRMED`; chuyến chưa khởi hành |
| **Kích hoạt** | Bấm "Hủy booking" ở UC-23 |
| **Hậu điều kiện** | `Booking` `CANCELLED`; `TripSeat` liên quan `AVAILABLE` |

**Luồng chính**

Giống UC-15 từ bước 2 trở đi, **trừ việc không kiểm tra quyền sở hữu** — Admin
hủy được booking của bất kỳ ai.

1. Admin bấm "Hủy booking".
2. Hệ thống hỏi xác nhận, nêu rõ đang hủy booking của khách hàng nào.
3. Backend mở transaction, kiểm tra `bookingStatus = CONFIRMED` và chuyến chưa
   khởi hành (BR-25).
4. Trong cùng transaction: `Booking` → `CANCELLED`; mọi `TripSeat` → `AVAILABLE`
   (BR-26, BR-27).
5. Commit và hiển thị kết quả.

**Luồng ngoại lệ**

- `E1` — **Booking không ở trạng thái `CONFIRMED`.** Từ chối, giải thích.
- `E2` — **Chuyến đã khởi hành.** Từ chối (BR-25).
- `E3` — **Lỗi ghi database.** Rollback, giữ nguyên booking.

**Quy tắc liên quan:** BR-25, BR-26, BR-27, BR-30, BR-32 · **Dữ liệu:** ghi
`Booking`, `TripSeat`

---

### UC-25 Xem danh sách tài khoản

| Mục | Nội dung |
| --- | --- |
| **Actor** | ADMIN |
| **Mô tả** | Xem danh sách tài khoản trong hệ thống, chỉ đọc |
| **Tiền điều kiện** | Đã đăng nhập `ADMIN` |
| **Kích hoạt** | Vào `/admin/users` |
| **Hậu điều kiện** | Không thay đổi dữ liệu |

**Luồng chính**

1. Hệ thống kiểm tra vai trò `ADMIN` (BR-30).
2. Hệ thống hiển thị bảng: họ tên, email, vai trò, ngày tạo.

**Luồng ngoại lệ**

- `E1` — **Người dùng không phải ADMIN.** Trả 403.

**Quy tắc liên quan:** BR-30, BR-35 · **Dữ liệu:** đọc `User`

**Chỉ xem.** Không có thêm, sửa, xóa tài khoản. Tài khoản `ADMIN` tạo bằng seed
data, không đăng ký qua giao diện (BR-35).

**Bảo mật:** trang này **không được** hiển thị mật khẩu, kể cả bản đã hash.

---
# Phần V — Thiết kế giao diện

## 22. Hệ thống giao diện

Nguồn: `docs/decisions/0003-he-thong-giao-dien.md`.

### 22.1 Phong cách

Modern Travel: nền sáng, card đơn giản, typography rõ, một màu primary dễ nhận
diện, whitespace hợp lý.

| Nhóm trang | Ưu tiên |
| --- | --- |
| Landing | Branding và form tìm chuyến |
| Booking | Chức năng và trạng thái rõ ràng |
| Admin | Bảng, form và CRUD mạch lạc |

Trạng thái chỗ phải nhìn là hiểu ngay, không cần đọc chú thích.

### 22.2 Design token

Khai trong `:root` của `app/src/main/resources/static/css/app.css`. Trang **không
được hardcode mã màu**, luôn đọc biến.

| Token | Giá trị | Dùng cho |
| --- | --- | --- |
| `--traingo-primary` | `#0f6cbd` | Nút chính, link, điểm nhấn |
| `--traingo-primary-dark` | `#0b558f` | Trạng thái hover và active |
| `--traingo-primary-soft` | `#eaf3fb` | Nền nhạt, badge số bước |
| `--traingo-ink` | `#16232e` | Chữ chính |
| `--traingo-muted` | `#5b6b7a` | Chữ phụ |
| `--traingo-surface` | `#ffffff` | Nền card |
| `--traingo-surface-alt` | `#f5f8fb` | Nền section xen kẽ, footer |
| `--traingo-border` | `#dde5ec` | Viền |
| `--traingo-radius` | `0.75rem` | Bo góc card |
| `--traingo-shadow` | hai lớp | Đổ bóng card |

Các token này được gán ngược vào biến của Bootstrap (`--bs-primary`,
`--bs-link-color`, `--bs-body-color`), nên `btn-primary` và link tự động theo màu
dự án mà không phải override từng chỗ.

### 22.3 Quy tắc chữ tiếng Việt

**`line-height` của heading tối thiểu 1.25.** Hiện dùng 1.28.

Bootstrap đặt `$headings-line-height: 1.2`. Tiếng Việt xếp chồng dấu thanh lên
trên dấu nguyên âm — `Ế`, `Ộ`, `ữ` — và ở tỉ lệ 1.2 những dấu đó bị cắt ngọn hoặc
chạm dòng phía trên. Tiếng Anh không bao giờ lộ lỗi này nên upstream không có lý
do sửa.

**Cách kiểm tra:** đo `getComputedStyle(el).lineHeight / fontSize` cho mọi
heading; tỉ lệ phải từ 1.15 trở lên.

Tên ga tiếng Việt dài hơn tiếng Anh đáng kể. Mọi thành phần hiển thị tên ga phải
cho chữ xuống dòng (`overflow-wrap: anywhere`) thay vì kéo giãn khung. Trường hợp
thử: `Ga Sài Gòn — Bến xe Miền Đông mới`.

### 22.4 Quy ước đặt tên

| Loại | Quy ước | Ví dụ |
| --- | --- | --- |
| CSS custom property | `--traingo-` | `--traingo-primary` |
| Class riêng của dự án | `tg-` | `tg-hero`, `tg-route-card` |
| Id dùng cho JavaScript | camelCase, tiền tố `tg` | `tgSearchForm`, `tgSwap` |

### 22.5 Layout dùng chung

`templates/layout/base.html` giữ `<head>`, navbar, footer và thẻ script. Mỗi
trang chỉ viết `<title>` và `<main>` của mình:

```html
<html th:replace="~{layout/base :: page(~{::title}, ~{::main})}">
<head><title>Tên trang — TrainGo</title></head>
<body>
<main>
	<!-- nội dung trang -->
</main>
</body>
</html>
```

`~{::title}` nghĩa là "lấy thẻ `title` của chính trang này". Đây là fragment có
tham số của Thymeleaf 3, không cần cài `thymeleaf-layout-dialect`.

Lỗi cú pháp ở đây **chỉ lộ lúc render**, không lộ lúc biên dịch — nên mỗi trang
mới cần ít nhất một `@WebMvcTest` render thật.

### 22.6 Bootstrap phục vụ từ ứng dụng

Hai file trong `static/vendor/bootstrap/`, không dùng CDN:

| File | Kích thước |
| --- | --- |
| `bootstrap.min.css` | 232.803 bytes |
| `bootstrap.bundle.min.js` | 80.721 bytes (bản `bundle` đã gồm Popper) |

**Bẫy quan trọng:** `SecurityConfig` kết thúc bằng `anyRequest().authenticated()`.
Nếu quên `permitAll()` cho `/vendor/**`, Spring Security trả trang login thay cho
file CSS, trình duyệt lặng lẽ bỏ qua, và **mọi trang hiện ra không có style —
log không báo gì cả**. Đã khóa bằng test
`bootstrapStylesheet_isReachable_withoutLogin`.

### 22.7 Component Bootstrap được dùng

| Component | Dùng ở đâu |
| --- | --- |
| `navbar`, `collapse` | Header mọi trang |
| `card` | Chuyến, booking, vé, thống kê |
| `form-control`, `form-select`, `form-label` | Mọi form |
| `btn`, `btn-outline-*` | Mọi nút |
| `table`, `table-hover`, `table-responsive` | Trang admin |
| `badge` | Trạng thái booking, thanh toán, chuyến |
| `alert` | Thông báo lỗi và thành công |
| `modal` | Xác nhận hủy booking, xác nhận xóa |
| `pagination` | Danh sách admin |
| `progress` | Đếm ngược giữ chỗ |
| `nav-tabs`, `nav-pills` | Nhóm booking, chọn toa |
| `offcanvas` | Sidebar admin trên mobile |

### 22.8 Bảng màu trạng thái

Dùng thống nhất trên toàn hệ thống:

| Trạng thái | Màu | Class |
| --- | --- | --- |
| Chỗ `AVAILABLE` | trắng, viền xám | `tg-seat--available` |
| Chỗ `SELECTED` (chỉ ở giao diện) | primary đậm | `tg-seat--selected` |
| Chỗ `HELD` | vàng | `tg-seat--held` |
| Chỗ `BOOKED` | xám đặc | `tg-seat--booked` |
| Booking `PENDING` | vàng | `bg-warning-subtle` |
| Booking `CONFIRMED` | xanh lá | `bg-success-subtle` |
| Booking `CANCELLED` | đỏ nhạt | `bg-danger-subtle` |
| Thanh toán `UNPAID` | xám | `bg-secondary-subtle` |
| Thanh toán `PAID` | xanh lá | `bg-success-subtle` |

Chỗ đã đặt **không được chỉ khác nhau bằng màu** — phải có thêm dấu hiệu hình
dạng hoặc ký hiệu, để người mù màu vẫn phân biệt được.

### 22.9 Responsive

Ba breakpoint phải kiểm tra: **375px** (mobile), **768px** (tablet), **1280px**
(desktop).

Sơ đồ ghế là màn hình khó nhất trên mobile: cho cuộn ngang **trong khung riêng**,
tuyệt đối không để cả trang cuộn ngang.

---

## 23. Đặc tả màn hình

22 màn hình. Mỗi màn hình gồm: đường dẫn, use case liên quan, wireframe,
component, các trạng thái phải xử lý, và điểm cần chú ý ở mobile.

**Trạng thái hiện tại:** chỉ `SC-01` đã được xây và kiểm chứng. 21 màn hình còn
lại là thiết kế dự kiến, chưa có code.

### 23.1 Nhóm Public

---

#### SC-01 Landing Page

`/` · UC-04 · **✅ Đã xây và kiểm chứng**

**Mục đích:** vừa giới thiệu TrainGo vừa là điểm bắt đầu của luồng đặt vé.

```
┌──────────────────────────────────────────────────────────┐
│ [TG] TrainGo    Trang chủ  Cách đặt vé  Chuyến của tôi   │
│                              [Đăng nhập] [Đăng ký]       │
├──────────────────────────────────────────────────────────┤
│                                                          │
│            Khám phá Việt Nam bằng tàu                    │
│      Tìm chuyến, chọn chỗ trên sơ đồ toa, thanh toán     │
│           và nhận vé điện tử — trong vài phút.           │
│                                                          │
│   ┌────────────────────────────────────────────────┐     │
│   │ Ga đi      ⇄   Ga đến    Ngày đi   Khách       │     │
│   │ [Sài Gòn ▾]    [Nha Trang▾] [24/08] [2]  [Tìm] │     │
│   └────────────────────────────────────────────────┘     │
├──────────────────────────────────────────────────────────┤
│  Tuyến phổ biến                                          │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐             │
│  │ SG     │ │ SG     │ │ HN     │ │ HN     │             │
│  │  ↓     │ │  ↓     │ │  ↓     │ │  ↓     │             │
│  │ Nha Tr │ │ Đà Nẵng│ │ Đà Nẵng│ │ Lào Cai│             │
│  └────────┘ └────────┘ └────────┘ └────────┘             │
├──────────────────────────────────────────────────────────┤
│  Đặt vé thế nào                                          │
│  (1) Tìm chuyến  (2) Chọn chỗ  (3) Nhập hành khách       │
│  (4) Thanh toán  (5) Nhận vé                             │
├──────────────────────────────────────────────────────────┤
│  Footer: TrainGo · Đặt vé · Tài khoản                    │
└──────────────────────────────────────────────────────────┘
```

**Component:** `navbar` + `collapse`, form với `form-select` và `form-control`,
card bo góc đổ bóng, grid 4 cột cho tuyến phổ biến, grid 5 cột cho các bước.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Ga đi trùng ga đến | Lỗi đỏ dưới form, không submit |
| Ngày ở quá khứ | Thuộc tính `min` chặn, JavaScript kiểm tra lại khi submit |
| Chưa đăng nhập | Navbar hiện Đăng nhập / Đăng ký |
| Đã đăng nhập | Navbar hiện Chuyến của tôi / Hồ sơ / Đăng xuất |

**Mobile:** form xếp dọc, nút đổi chiều thành nút ngang full width, tuyến phổ
biến xếp một cột.

**Đã biết:** form submit sang `/trips` trả 404 cho tới khi Giai đoạn 3 xong. Đây
là hành vi có chủ đích, **không phải lỗi**.

---

#### SC-02 Đăng nhập

`/login` · UC-02

**Mục đích:** xác thực bằng email và mật khẩu, session-based.

```
┌──────────────────────────────────────────┐
│ [TG] TrainGo                             │
├──────────────────────────────────────────┤
│        ┌──────────────────────┐          │
│        │  Đăng nhập           │          │
│        │  Email               │          │
│        │  [________________]  │          │
│        │  Mật khẩu            │          │
│        │  [________________]  │          │
│        │  [   Đăng nhập    ]  │          │
│        │  Chưa có tài khoản?  │          │
│        │  Đăng ký             │          │
│        └──────────────────────┘          │
└──────────────────────────────────────────┘
```

**Component:** card căn giữa `col-md-5`, `form-control`, `alert-danger` cho lỗi.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Sai email hoặc mật khẩu | Một thông báo chung, không nói cái nào sai |
| Vừa đăng ký xong | `alert-success` "Đăng ký thành công, mời đăng nhập" |
| Bị đá về từ trang cần quyền | Thông báo cần đăng nhập, quay lại đúng trang đó sau khi vào |

**Mobile:** card chiếm gần trọn chiều ngang, giảm padding.

**Không có** "Quên mật khẩu" — ngoài phạm vi.

---

#### SC-03 Đăng ký

`/register` · UC-01

**Mục đích:** tạo tài khoản `CUSTOMER`.

```
┌──────────────────────────────────────────┐
│        ┌──────────────────────┐          │
│        │  Đăng ký             │          │
│        │  Họ và tên           │          │
│        │  [________________]  │          │
│        │  Email               │          │
│        │  [________________]  │          │
│        │  Mật khẩu            │          │
│        │  [________________]  │          │
│        │  Nhập lại mật khẩu   │          │
│        │  [________________]  │          │
│        │  [    Đăng ký     ]  │          │
│        │  Đã có tài khoản?    │          │
│        └──────────────────────┘          │
└──────────────────────────────────────────┘
```

**Component:** như SC-02, thêm `invalid-feedback` dưới từng ô.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Email đã tồn tại | Lỗi ngay dưới ô email, giữ lại các giá trị khác |
| Email sai định dạng | `@Email` bắt ở backend |
| Mật khẩu quá ngắn | `@Size` bắt, nói rõ số ký tự tối thiểu |
| Hai mật khẩu không khớp | Lỗi dưới ô nhập lại |

**Mobile:** một cột, nút full width.

**Không có** tùy chọn vai trò trên form (BR-29).

---

### 23.2 Nhóm Booking

---

#### SC-04 Danh sách chuyến

`/trips` · UC-05

**Mục đích:** hiển thị các chuyến phù hợp với điều kiện tìm kiếm.

```
┌───────────────────────────────────────────────────────────┐
│ Sài Gòn → Nha Trang · 24/08/2026 · 2 khách   [Đổi tìm kiếm]│
├───────────────────────────────────────────────────────────┤
│ ● Trip ─ ○ Seat ─ ○ Passenger ─ ○ Review ─ ○ Pay ─ ○ Ticket│
├───────────────────────────────────────────────────────────┤
│ ┌───────────────────────────────────────────────────────┐ │
│ │ SE2                                                   │ │
│ │ 06:00 ──────── 7 giờ 30 phút ──────── 13:30           │ │
│ │ Ga Sài Gòn                          Ga Nha Trang      │ │
│ │ Còn 24 chỗ                Giá từ  350.000đ [Chọn]     │ │
│ └───────────────────────────────────────────────────────┘ │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ SE4  ...                                              │ │
│ └───────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
```

**Thông tin mỗi chuyến:** mã tàu, ga đi, ga đến, giờ khởi hành, giờ đến, thời
gian di chuyển, giá thấp nhất, số chỗ còn lại.

**Component:** `card` cho mỗi chuyến, stepper tự viết bằng flex, `badge` cho số
chỗ còn lại.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Không có chuyến nào | Khối rỗng "Không tìm thấy chuyến phù hợp" + gợi ý đổi ngày |
| Chuyến hết chỗ | Vẫn hiện, nút vô hiệu, badge "Hết chỗ" |
| Chuyến `CANCELLED` | Không hiện trong danh sách |
| Tham số tìm kiếm sai | Quay về landing kèm thông báo |

**Mobile:** card xếp dọc, dòng giờ đi–giờ đến thành hai dòng, giá và nút xuống
dòng riêng.

---

#### SC-05 Chi tiết chuyến

`/trips/:id` · UC-06

**Mục đích:** xem thông tin đầy đủ một chuyến trước khi chọn chỗ.

```
┌──────────────────────────────────────────────────────┐
│ ← Quay lại danh sách                                 │
│ SE2 · Ga Sài Gòn → Ga Nha Trang                      │
│ 24/08/2026 · 06:00 → 13:30 · 7 giờ 30 phút           │
├──────────────────────────────────────────────────────┤
│ Các toa                                              │
│ ┌───────────────┐ ┌────────────────┐                 │
│ │ Toa 01 · Ghế  │ │ Toa 02 · Giường│                 │
│ │ Còn 12/16     │ │ Còn 8/16       │                 │
│ │ 350.000đ      │ │ 550.000đ       │                 │
│ └───────────────┘ └────────────────┘                 │
│              [       Chọn chỗ       ]                │
└──────────────────────────────────────────────────────┘
```

**Component:** `card`, `badge` loại toa, link quay lại.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Chuyến không tồn tại | 404 tử tế, có nút về trang chủ |
| Chuyến `CANCELLED` | `alert-danger`, ẩn nút chọn chỗ |
| Chuyến đã khởi hành | `alert-warning`, ẩn nút chọn chỗ |
| Toa hết chỗ | Card mờ, không click được |
| Tàu chưa có toa | Thông báo chuyến chưa mở bán |

**Mobile:** card toa xếp một cột.

---

#### SC-06 Chọn toa và chỗ

`/booking/:tripId/seats` · UC-07, UC-08 · **Màn hình khó nhất**

```
┌────────────────────────────────────────────────────────┐
│ ● Trip ─ ● Seat ─ ○ Passenger ─ ○ Review ─ ○ Pay        │
├────────────────────────────────────────────────────────┤
│ [Toa 01 Ghế] [Toa 02 Giường] [Toa 03 Ghế]              │
├─────────────────────────────────┬──────────────────────┤
│  Sơ đồ toa 01                   │  Đã chọn             │
│                                 │                      │
│   ┌──┐┌──┐    ┌──┐┌──┐          │  A03  350.000đ  [x]  │
│   │A1││A2│    │A3││A4│          │  A04  350.000đ  [x]  │
│   └──┘└──┘    └──┘└──┘          │                      │
│   ┌──┐┌──┐    ┌──┐┌──┐          │  Tổng   700.000đ     │
│   │A5││A6│    │A7││A8│          │                      │
│   └──┘└──┘    └──┘└──┘          │  Giữ chỗ còn 04:37   │
│                                 │  ▓▓▓▓▓▓▓░░░          │
│   □ Trống  ■ Đang chọn          │                      │
│   ▨ Đang giữ  ▩ Đã đặt          │  [   Tiếp tục    ]   │
└─────────────────────────────────┴──────────────────────┘
```

Toa giường vẽ theo cabin, mỗi cabin 4 giường:

```
   Cabin 1        Cabin 2
  ┌────┬────┐   ┌────┬────┐
  │ B1 │ B2 │   │ B5 │ B6 │
  ├────┼────┤   ├────┼────┤
  │ B3 │ B4 │   │ B7 │ B8 │
  └────┴────┘   └────┴────┘
```

**Component:** `nav-pills` chọn toa, sơ đồ vẽ bằng CSS grid, `progress` cho đếm
ngược, panel bên phải `position: sticky`.

**Kỹ thuật:** JavaScript thuần trong `static/js/` gọi `@RestController` dưới
`/api/...` trả JSON trạng thái `TripSeat`.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Chỗ vừa bị người khác giữ | Cập nhật sơ đồ theo trạng thái thật, `alert` giải thích |
| Hết 5 phút giữ chỗ | Modal "Hết thời gian giữ chỗ", trả chỗ, quay về chọn lại |
| Chọn quá số hành khách | Chặn click, giải thích đã đủ chỗ |
| Chọn ít hơn số hành khách | Nút Tiếp tục vô hiệu, nói rõ còn thiếu mấy chỗ |
| Chuyến bị hủy khi đang chọn | Đá về SC-05 kèm thông báo |

**Mobile:** panel "Đã chọn" chuyển xuống dưới, dính đáy màn hình. Sơ đồ cuộn
ngang **trong khung riêng** — cả trang không được cuộn ngang.

---

#### SC-07 Nhập thông tin hành khách

`/booking/passengers` · UC-09

```
┌────────────────────────────────────────────────────┐
│ ● Trip ─ ● Seat ─ ● Passenger ─ ○ Review ─ ○ Pay    │
│                              Giữ chỗ còn 03:12     │
├────────────────────────────────────────────────────┤
│ Hành khách 1 — Chỗ A03                             │
│ Họ và tên      [________________________]          │
│ Ngày sinh      [__/__/____]                        │
│ CCCD/Passport  [________________________]          │
├────────────────────────────────────────────────────┤
│ Hành khách 2 — Chỗ A04                             │
│ Họ và tên      [________________________]          │
│ Ngày sinh      [__/__/____]                        │
│ CCCD/Passport  [________________________]          │
├────────────────────────────────────────────────────┤
│              [← Quay lại]  [Tiếp tục →]            │
└────────────────────────────────────────────────────┘
```

**Component:** một `card` cho mỗi hành khách, `form-control`,
`invalid-feedback`, đồng hồ đếm ngược ở đầu trang.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Thiếu trường bắt buộc | Lỗi dưới đúng ô đó, **không mất** dữ liệu đã nhập |
| Hết giờ giữ chỗ khi đang nhập | Cảnh báo, giữ lại dữ liệu đã nhập, đưa về SC-06 |
| Số form khác số chỗ | Không xảy ra từ giao diện, nhưng backend vẫn kiểm tra |

**Mobile:** mỗi card một cột, các ô full width.

---

#### SC-08 Xem lại booking

`/booking/review` · UC-10

```
┌────────────────────────────────────────────────────┐
│ ● Trip ─ ● Seat ─ ● Passenger ─ ● Review ─ ○ Pay    │
├────────────────────────────────────────────────────┤
│ Chuyến                                             │
│ SE2 · Ga Sài Gòn → Ga Nha Trang                    │
│ 24/08/2026 · 06:00 → 13:30                         │
├────────────────────────────────────────────────────┤
│ Hành khách                                         │
│ ┌────────────────────────────────────────────────┐ │
│ │ Nguyễn Văn A · 01/01/1998 · Toa 01 · A03       │ │
│ │                                     350.000đ   │ │
│ ├────────────────────────────────────────────────┤ │
│ │ Trần Thị B  · 05/09/2000 · Toa 01 · A04        │ │
│ │                                     350.000đ   │ │
│ └────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────┤
│ Tổng cộng                            700.000đ      │
│              [← Quay lại]  [Thanh toán →]          │
└────────────────────────────────────────────────────┘
```

**Component:** `card`, `list-group` cho hành khách, dòng tổng in đậm cỡ lớn.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Hết giờ giữ chỗ | Chặn nút thanh toán, đưa về SC-06 |
| Chỗ bị mất | `alert-danger` nêu rõ chỗ nào, quay về SC-06 |
| Giá đổi so với lúc chọn | Không xảy ra: giá đã đóng băng (BR-19) |

**Mobile:** bảng hành khách thành card xếp dọc, không dùng bảng ngang.

---

#### SC-09 Thanh toán

`/booking/payment` · UC-11

```
┌────────────────────────────────────────────────────┐
│ ● Trip ─ ● Seat ─ ● Passenger ─ ● Review ─ ● Pay    │
├────────────────────────────────────────────────────┤
│        ┌──────────────────────────────┐            │
│        │  Thanh toán                  │            │
│        │  Mã booking  BK202608001     │            │
│        │  Số vé       2               │            │
│        │  Tổng tiền   700.000 VND     │            │
│        │                              │            │
│        │  ⓘ Đây là thanh toán mô      │            │
│        │    phỏng, không có giao      │            │
│        │    dịch thật.                │            │
│        │                              │            │
│        │  [    Xác nhận thanh toán  ] │            │
│        │  [         Hủy bỏ          ] │            │
│        └──────────────────────────────┘            │
└────────────────────────────────────────────────────┘
```

**Component:** `card` căn giữa, `alert-info` cho ghi chú mô phỏng, nút chính cỡ
lớn.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Đang xử lý | Nút `disabled` + spinner, chặn bấm hai lần |
| Chỗ vừa bị mất | Rollback, `alert-danger`, quay về SC-06 |
| Hết giờ giữ chỗ | Chặn ngay, không tạo booking |
| Thành công | Chuyển sang SC-10 |

**Mobile:** card gần full width.

---

#### SC-10 Đặt vé thành công

`/booking/success` · UC-11

```
┌────────────────────────────────────────────────────┐
│                      ✓                             │
│              Đặt vé thành công                     │
│                                                    │
│           Mã booking:  BK202608001                 │
│           2 vé · Ga Sài Gòn → Ga Nha Trang         │
│           24/08/2026 · 06:00                       │
│                                                    │
│        [  Xem vé điện tử  ]  [ Chuyến của tôi ]    │
└────────────────────────────────────────────────────┘
```

**Component:** ký hiệu lớn, `card` căn giữa, hai nút.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Tải lại trang | Vẫn hiện đúng booking, **không** tạo booking mới |
| Vào bằng URL trực tiếp | Nếu không có booking trong session thì đá về SC-11 |

**Mobile:** hai nút xếp dọc, full width.

---

### 23.3 Nhóm Customer

---

#### SC-11 Chuyến của tôi

`/my-trips` · UC-13

```
┌────────────────────────────────────────────────────┐
│ Chuyến của tôi                                     │
│ [Sắp đi] [Đã hoàn thành] [Đã hủy]                  │
├────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐ │
│ │ BK202608001              [CONFIRMED] [PAID]    │ │
│ │ Ga Sài Gòn → Ga Nha Trang                      │ │
│ │ 24/08/2026 · 06:00 · SE2 · Toa 01 · A03, A04   │ │
│ │            [Xem chi tiết] [Xem vé] [Hủy]       │ │
│ └────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────┐ │
│ │ BK202607015              [CANCELLED]           │ │
│ └────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────┘
```

**Component:** `nav-tabs` cho ba nhóm, `card` cho mỗi booking, `badge` trạng
thái, `modal` xác nhận hủy.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Chưa có booking nào | Khối rỗng + nút "Tìm chuyến ngay" |
| Nhóm rỗng | Thông báo riêng cho từng tab |
| Booking không hủy được | Nút Hủy ẩn hoặc vô hiệu, có giải thích |

**Mobile:** tab cuộn ngang, card một cột, nút xếp dọc.

---

#### SC-12 Chi tiết booking

`/my-trips/:bookingId` · UC-14, UC-15

```
┌────────────────────────────────────────────────────┐
│ ← Chuyến của tôi                                   │
│ Booking BK202608001      [CONFIRMED] [PAID]        │
├────────────────────────────────────────────────────┤
│ Chuyến                                             │
│ SE2 · Ga Sài Gòn → Ga Nha Trang                    │
│ 24/08/2026 · 06:00 → 13:30                         │
├────────────────────────────────────────────────────┤
│ Hành khách và vé                                   │
│ Nguyễn Văn A · Toa 01 · A03 · 350.000đ  [Xem vé]   │
│ Trần Thị B  · Toa 01 · A04 · 350.000đ  [Xem vé]    │
├────────────────────────────────────────────────────┤
│ Tổng tiền            700.000đ                      │
│ Đặt lúc              23/08/2026 14:22              │
│                                   [Hủy booking]    │
└────────────────────────────────────────────────────┘
```

**Component:** `card`, `list-group`, `badge`, `modal` xác nhận hủy.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Booking của người khác | **403, không phải 404**, và không hiện nội dung |
| Booking không tồn tại | 404 tử tế |
| Đã hủy | Badge `CANCELLED`, ẩn nút hủy |
| Chuyến đã khởi hành | Ẩn nút hủy |

**Mobile:** danh sách hành khách thành card xếp dọc.

**Bảo mật:** đây là chỗ hay quên nhất — phải kiểm tra `booking.customerId` khớp
người đang đăng nhập (BR-31). Bắt buộc có test.

---

#### SC-13 Vé điện tử

`/tickets/:ticketId` · UC-12

```
┌──────────────────────────────────────┐
│  TrainGo · Vé điện tử                │
│  ──────────────────────────────────  │
│  Mã vé      TK202608001-1            │
│  Booking    BK202608001              │
│                                      │
│  Hành khách Nguyễn Văn A             │
│                                      │
│  Ga Sài Gòn  →  Ga Nha Trang         │
│  24/08/2026 · 06:00 → 13:30          │
│                                      │
│  Tàu SE2 · Toa 01 · Ghế A03          │
│  Giá 350.000đ                        │
│  ──────────────────────────────────  │
│  [QR code — optional]                │
└──────────────────────────────────────┘
```

**Component:** card dạng vé, viền đứt phân tách phần cuống, mã vé cỡ lớn dễ đọc.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Vé của người khác | 403, không hiện nội dung |
| Booking đã hủy | Đóng dấu "ĐÃ HỦY" chéo qua vé, vẫn cho xem |
| In ra giấy | Print stylesheet: ẩn navbar, footer, nút |

**Mobile:** đây là trang hay được mở trên điện thoại nhất — chữ phải đủ lớn,
không cần phóng to.

---

### 23.4 Nhóm Admin

Mọi trang admin dùng chung một layout: sidebar bên trái, nội dung bên phải.

```
┌──────────┬─────────────────────────────────────────┐
│ TrainGo  │  Tiêu đề trang           [+ Thêm mới]   │
│ Admin    │  ─────────────────────────────────────  │
│          │                                         │
│ Dashboard│  (bảng hoặc form)                       │
│ Ga       │                                         │
│ Tuyến    │                                         │
│ Tàu      │                                         │
│ Chuyến   │                                         │
│ Booking  │                                         │
│ Tài khoản│                                         │
└──────────┴─────────────────────────────────────────┘
```

Trên mobile sidebar thu thành `offcanvas` mở bằng nút hamburger.

---

#### SC-14 Dashboard

`/admin` · UC-16

```
┌─────────────────────────────────────────────────┐
│ Dashboard                                       │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │ Booking │ │ Chuyến  │ │ Người   │ │ Doanh   │ │
│ │         │ │         │ │ dùng    │ │ thu     │ │
│ │   128   │ │   42    │ │   87    │ │ 44,8tr  │ │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘ │
│                                                 │
│ Booking gần đây                                 │
│ (bảng 5 dòng mới nhất)                          │
└─────────────────────────────────────────────────┘
```

**Component:** 4 `card` thống kê, `table` nhỏ bên dưới.

**Trạng thái:** dữ liệu rỗng thì hiện `0`, không hiện trang trắng.

**Mobile:** thẻ số xếp 2 cột.

---

#### SC-15 Quản lý ga

`/admin/stations` · UC-17

```
┌───────────────────────────────────────────────────┐
│ Quản lý ga                          [+ Thêm ga]   │
│ ┌───────────────────────────────────────────────┐ │
│ │ Code │ Tên ga      │ Thành phố  │ Thao tác    │ │
│ ├──────┼─────────────┼────────────┼─────────────┤ │
│ │ SGN  │ Ga Sài Gòn  │ Hồ Chí Minh│ [Sửa][Xóa]  │ │
│ │ NTR  │ Ga Nha Trang│ Khánh Hòa  │ [Sửa][Xóa]  │ │
│ └───────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

**Component:** `table table-hover`, `modal` cho form thêm và sửa, `modal` xác
nhận xóa, `pagination`.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Mã ga trùng | Lỗi dưới ô mã, không đóng modal |
| Tên để trống | Lỗi dưới ô tên |
| Ga đang được tuyến dùng | Chặn xóa, chỉ ra nơi đang dùng |
| Chưa có ga nào | Khối rỗng + nút thêm |

**Mobile:** bảng cuộn ngang trong `table-responsive`.

---

#### SC-16 Quản lý tuyến

`/admin/routes` · UC-18

```
┌───────────────────────────────────────────────────┐
│ Quản lý tuyến                    [+ Thêm tuyến]   │
│ ┌───────────────────────────────────────────────┐ │
│ │ Ga đi      │ Ga đến      │ Km  │ Thao tác     │ │
│ ├────────────┼─────────────┼─────┼──────────────┤ │
│ │ Ga Sài Gòn │ Ga Nha Trang│ 411 │ [Sửa][Xóa]   │ │
│ └───────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

**Component:** như SC-15, form dùng `form-select` chọn hai ga.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Ga đi trùng ga đến | Chặn ngay, hiện lỗi |
| Chưa có ga nào | Chặn tạo tuyến, chỉ sang trang quản lý ga |
| Tuyến đang có chuyến | Chặn xóa, nói rõ số chuyến |

**Mobile:** bảng cuộn ngang.

---

#### SC-17 Quản lý tàu

`/admin/trains` · UC-19

```
┌───────────────────────────────────────────────────┐
│ Quản lý tàu                        [+ Thêm tàu]   │
│ ┌───────────────────────────────────────────────┐ │
│ │ Mã tàu │ Tên      │ Số toa │ Thao tác         │ │
│ ├────────┼──────────┼────────┼──────────────────┤ │
│ │ SE2    │ Tàu SE2  │ 8      │ [Chi tiết][Sửa]  │ │
│ └───────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

**Component:** `table`, `modal` cho form.

**Trạng thái:** mã tàu trùng thì báo lỗi; tàu đang có chuyến thì chặn xóa.

**Mobile:** bảng cuộn ngang.

---

#### SC-18 Chi tiết tàu và cấu hình toa

`/admin/trains/:id` · UC-20

**Không có route `/admin/coaches` riêng.**

```
┌────────────────────────────────────────────────────┐
│ ← Danh sách tàu                                    │
│ Tàu SE2                             [+ Thêm toa]   │
├────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐ │
│ │ Toa 01 · SEAT · 16 chỗ · +0đ                   │ │
│ │ A01 A02 A03 A04 A05 A06 A07 A08                │ │
│ │ A09 A10 A11 A12 A13 A14 A15 A16    [Sửa][Xóa]  │ │
│ ├────────────────────────────────────────────────┤ │
│ │ Toa 02 · SLEEPER · 16 chỗ · +200.000đ          │ │
│ │ Cabin 1: B01 B02 B03 B04                       │ │
│ │ Cabin 2: B05 B06 B07 B08           [Sửa][Xóa]  │ │
│ └────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────┘
```

Form thêm toa chỉ có ba trường: loại toa, `capacity`, `priceModifier`. **Hệ thống
tự sinh ghế** (BR-04).

**Component:** `card` cho mỗi toa, `badge` loại toa, danh sách mã ghế dạng chip.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Sau khi thêm toa | Hiện ngay danh sách ghế vừa sinh, để Admin thấy kết quả |
| Toa đã có chỗ được đặt | Chặn xóa và chặn sửa `capacity` |
| `SLEEPER` có `capacity` không chia hết cho 4 | Cảnh báo cabin cuối không đủ 4 giường |

**Mobile:** chip mã ghế xuống dòng tự nhiên.

---

#### SC-19 Quản lý chuyến

`/admin/trips` · UC-21

```
┌─────────────────────────────────────────────────────┐
│ Quản lý chuyến                     [+ Thêm chuyến]  │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Tàu │ Tuyến      │ Ngày   │ Giờ   │ Giá  │ TT   │ │
│ ├─────┼────────────┼────────┼───────┼──────┼──────┤ │
│ │ SE2 │ SGN → NTR  │ 24/08  │ 06:00 │ 350k │ [SCH]│ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

**Component:** `table`, `badge` trạng thái, form trong `modal` hoặc trang riêng.

**Trạng thái**

| Trạng thái | Xử lý |
| --- | --- |
| Sau khi tạo chuyến | Thông báo đã sinh bao nhiêu chỗ, để Admin biết đã chạy đúng |
| Chuyến đã có booking | Chặn xóa; chỉ cho hủy chuyến |
| Ngày ở quá khứ | Cảnh báo, vì chuyến quá khứ không đặt được |
| Chưa có tàu hoặc tuyến | Chặn tạo, chỉ sang trang tương ứng |
| Tàu chưa có toa | Cảnh báo chuyến sẽ có 0 chỗ |

**Mobile:** bảng cuộn ngang.

---

#### SC-20 Quản lý booking

`/admin/bookings` · UC-22

```
┌─────────────────────────────────────────────────────┐
│ Quản lý booking      [Tìm theo mã booking      ][🔍]│
│ ┌─────────────────────────────────────────────────┐ │
│ │ Mã          │ Khách    │ Chuyến │ Tiền │ TT     │ │
│ ├─────────────┼──────────┼────────┼──────┼────────┤ │
│ │ BK202608001 │ Văn A    │ SE2    │ 700k │ [CONF] │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

**Component:** ô tìm kiếm, `table`, `badge` kép cho trạng thái booking và thanh
toán, `pagination`.

**Trạng thái:** không tìm thấy mã thì hiện khối rỗng nói rõ đã tìm mã nào.

**Mobile:** bảng cuộn ngang.

---

#### SC-21 Chi tiết booking (admin)

`/admin/bookings/:id` · UC-23, UC-24

```
┌────────────────────────────────────────────────────┐
│ ← Danh sách booking                                │
│ BK202608001               [CONFIRMED] [PAID]       │
├────────────────────────────────────────────────────┤
│ Khách hàng   Nguyễn Văn A · a@example.com          │
│ Chuyến       SE2 · SGN → NTR · 24/08 06:00         │
├────────────────────────────────────────────────────┤
│ Hành khách                                         │
│ Nguyễn Văn A · CCCD 0790… · Toa 01 · A03 · 350k    │
│ Trần Thị B  · CCCD 0791… · Toa 01 · A04 · 350k     │
├────────────────────────────────────────────────────┤
│ Tổng 700.000đ · Đặt lúc 23/08 14:22                │
│                                  [Hủy booking]     │
└────────────────────────────────────────────────────┘
```

**Component:** `card`, `list-group`, `modal` xác nhận hủy.

**Admin KHÔNG được:** đổi chỗ sau khi đã đặt, sửa trạng thái thanh toán thủ công,
hoàn tiền, đổi hành khách (BR-32). Chỉ được hủy.

**Mobile:** danh sách hành khách thành card.

---

#### SC-22 Danh sách tài khoản

`/admin/users` · UC-25

```
┌─────────────────────────────────────────────────────┐
│ Tài khoản                                           │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Tên        │ Email          │ Role     │ Ngày   │ │
│ ├────────────┼────────────────┼──────────┼────────┤ │
│ │ Nguyễn A   │ a@example.com  │ CUSTOMER │ 20/08  │ │
│ │ Admin      │ admin@traingo  │ ADMIN    │ 01/08  │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

**Component:** `table`, `badge` cho vai trò, `pagination`.

**Trạng thái:** không có nút sửa hay xóa.

**Mobile:** bảng cuộn ngang.

**Bảo mật:** trang này **không được** hiển thị mật khẩu, kể cả bản hash.

---
# Phần VI — Xây dựng và kiểm chứng

## 24. Kiến trúc kỹ thuật

### 24.1 Ba tầng

| Tầng | Trách nhiệm | Không được làm gì |
| --- | --- | --- |
| Controller | Nhận request, validate cơ bản, gọi service, trả view hoặc JSON | Không đặt nghiệp vụ phức tạp |
| Service | Nghiệp vụ và transaction | Không truy cập database trực tiếp |
| Repository | Truy cập dữ liệu | Không chứa nghiệp vụ |

Dùng constructor injection, **không** dùng `@Autowired` trên field.

### 24.2 Chia package theo feature

```
com.voyagego.traingo
├── auth/       đăng ký, đăng nhập, User
├── station/
├── route/
├── train/      Train, Coach, Seat
├── trip/       Trip, TripSeat
├── booking/    Booking, BookingPassenger
├── ticket/
├── admin/      các màn hình quản trị
├── home/       landing page
└── common/     config, exception, tiện ích dùng chung
```

Mỗi package chứa đủ controller, service, repository và entity của nghiệp vụ đó.

Lý do chọn cách này thay vì chia theo tầng: ba người làm song song sẽ không cùng
sửa một thư mục `service/` trong mọi task, nên ít xung đột Git hơn hẳn.

### 24.3 Tech stack

| Nhóm | Công nghệ |
| --- | --- |
| Frontend | HTML5, CSS3, JavaScript thuần, Thymeleaf, Bootstrap 5.3.3 |
| Backend | Java 17, Spring Boot 4.1.1, Spring MVC |
| Database | MySQL 8.4 (dev chạy bằng Docker Compose) |
| ORM | Spring Data JPA, Hibernate |
| Auth | Spring Security, session-based |
| Validation | Bean Validation (`@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`) |
| Test | JUnit 5, Mockito, H2 in-memory |
| Optional | ZXing (QR), Leaflet + OpenStreetMap (map) |

### 24.4 Mức tách client

Thymeleaf render server-side cho phần lớn màn hình. Riêng phần cần tương tác — sơ
đồ ghế, giữ chỗ, đếm ngược 5 phút — dùng JavaScript thuần trong
`src/main/resources/static/js/` gọi các endpoint `@RestController` dưới `/api/...`
trả JSON.

**Không dựng dự án frontend riêng.** React sẽ khiến phải xử lý CORS và làm
session-based authentication rắc rối hơn nhiều, với khối lượng công việc gần gấp
đôi.

### 24.5 Bẫy của Spring Boot 4 so với tài liệu 3.x

Tài liệu và ví dụ trên mạng phần lớn viết cho Spring Boot 3.x. Khi gặp lỗi không
tìm thấy class hoặc annotation, **kiểm tra jar thật trong `~/.m2/repository` trước
khi tin bài viết trên mạng**. Đã gặp:

| Thứ | Ở Boot 3.x | Ở Boot 4.1.1 |
| --- | --- | --- |
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure`, cần `spring-boot-starter-webmvc-test` |
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure`, cần `spring-boot-starter-data-jpa-test` |
| Mock bean | `@MockBean` | `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) |

`spring-boot-starter-test` một mình **không đủ** để dùng `@WebMvcTest` hay
`@DataJpaTest` nữa.

### 24.6 Chạy ở máy local

```powershell
cd app
docker compose up -d          # MySQL cho môi trường dev
.\mvnw.cmd spring-boot:run    # ứng dụng ở http://localhost:8081
.\mvnw.cmd test               # không cần Docker, chạy trên H2
```

Không có `mvn` trên PATH — luôn dùng Maven Wrapper `.\mvnw.cmd`.

Ứng dụng nghe ở **8081** chứ không phải 8080, vì Apache của XAMPP đang giữ 8080
trên máy dev.

Chi tiết và cách xử lý sự cố: `docs/RUNBOOK.md`.

---

## 25. Kế hoạch xây dựng

Xây theo thứ tự phụ thuộc dữ liệu. Mỗi giai đoạn kết thúc bằng một thứ chạy được
và một bộ test tương ứng.

| Giai đoạn | Nội dung | Use case | Trạng thái |
| --- | --- | --- | --- |
| 0 | Cấu trúc thư mục, build, bộ test, landing tạm | — | ✅ xong |
| 0b | MySQL bằng Docker, ứng dụng kết nối được database | — | ✅ xong 24/08/2026 |
| 0c | Nền tảng giao diện, Bootstrap local, layout, landing thật | UC-04 (một phần) | ✅ xong 24/08/2026 |
| 1 | Auth | UC-01, UC-02, UC-03 | ⬜ |
| 2 | Station → Route → Train + Coach + Seat → Trip + TripSeat | UC-17..UC-21 | ⬜ |
| 3 | Tìm chuyến và danh sách chuyến | UC-04, UC-05, UC-06 | ⬜ |
| 4 | Chọn chỗ và giữ chỗ | UC-07, UC-08 | ⬜ |
| 5 | Hành khách và review | UC-09, UC-10 | ⬜ |
| 6 | Thanh toán mô phỏng, transaction, vé điện tử | UC-11, UC-12 | ⬜ |
| 7 | My Trips và hủy booking | UC-13, UC-14, UC-15 | ⬜ |
| 8 | Admin dashboard, quản lý booking, danh sách người dùng | UC-16, UC-22..UC-25 | ⬜ |
| 9 | Hoàn thiện, responsive, seed data | — | ⬜ |

### 25.1 Chi tiết từng giai đoạn

**Giai đoạn 1 — Auth.** `User` entity, hash mật khẩu, đăng ký, đăng nhập, đăng
xuất, phân quyền `CUSTOMER` / `ADMIN`, seed tài khoản admin. Mọi việc sau đều cần
biết ai đang đăng nhập.

**Giai đoạn 2 — Dữ liệu vận hành.** Làm đúng thứ tự Station → Route → Train với
Coach và sinh Seat tự động → Trip kèm sinh TripSeat, vì mỗi bước phụ thuộc bước
trước.

**Giai đoạn 3 — Tìm chuyến.** Nối form ở landing vào backend, validate điều kiện
tìm kiếm, danh sách chuyến với số chỗ còn lại và giá thấp nhất, trang chi tiết
chuyến.

**Giai đoạn 4 — Chọn chỗ và giữ chỗ.** Trang chọn toa, sơ đồ ghế và sơ đồ giường
vẽ bằng JavaScript, endpoint JSON trả trạng thái TripSeat, endpoint giữ chỗ, đếm
ngược 5 phút, xử lý hết hạn. **Phải chốt cách thu hồi chỗ hết hạn trước khi bắt
đầu.**

**Giai đoạn 5 — Hành khách và review.** Form nhập hành khách khớp số chỗ, gán mỗi
hành khách một chỗ, màn hình review với giá từng vé và tổng tiền.

**Giai đoạn 6 — Thanh toán và vé.** Xác nhận booking trong một transaction, sinh
Ticket, trang booking thành công, trang vé điện tử.

**Giai đoạn 7 — My Trips và hủy booking.** Danh sách theo ba nhóm, chi tiết
booking, hủy booking trả chỗ về `AVAILABLE`, kiểm tra quyền sở hữu.

**Giai đoạn 8 — Admin còn lại.** Dashboard, quản lý booking, danh sách người
dùng.

**Giai đoạn 9 — Hoàn thiện.** Responsive, thông báo lỗi tử tế, seed data demo, rà
lại toàn bộ luồng.

### 25.2 Gợi ý chia việc cho ba người

Sau Giai đoạn 1:

| Người | Nhận | Use case |
| --- | --- | --- |
| A | Giai đoạn 2 và 8 — dữ liệu vận hành và admin | UC-16..UC-25 |
| B | Giai đoạn 3 và 4 — tìm chuyến và chọn chỗ | UC-04..UC-08 |
| C | Giai đoạn 5, 6 và 7 — hành khách, thanh toán, My Trips | UC-09..UC-15 |

Package chia theo feature nên ba nhánh ít đụng nhau. File dễ xung đột nhất là
`SecurityConfig.java` và `app.css` — thống nhất ai sửa thì báo trước.

---

## 26. Chiến lược kiểm thử

Nguồn: `docs/decisions/0002-chien-luoc-test.md`.

### 26.1 Quy tắc số một

**Không tuyên bố hoàn thành khi chưa chạy test.** Trước khi báo xong một phần
việc, phải chạy `.\mvnw.cmd test` và dẫn kết quả thật.

Phân biệt rõ bốn việc khác nhau: đã sửa code, đã biên dịch, đã chạy test, test đã
xanh.

### 26.2 Bốn tầng test

Chọn tầng thấp nhất chứng minh được hành vi.

| Loại | Annotation | Dùng cho | Tốc độ |
| --- | --- | --- | --- |
| Unit | JUnit + Mockito | Quy tắc nghiệp vụ trong service | mili giây |
| Repository | `@DataJpaTest` | Query tự viết, ràng buộc unique, quan hệ entity | vài trăm ms |
| Controller | `@WebMvcTest` | Routing, validation, phân quyền, status code | vài trăm ms |
| Toàn hệ thống | `@SpringBootTest` | Luồng MVP xuyên suốt | vài giây |

`@SpringBootTest` chỉ dùng cho một đến hai test luồng chính. Viết mọi thứ bằng
`@SpringBootTest` sẽ làm bộ test chậm tới mức không ai muốn chạy.

### 26.3 Nghiệp vụ bắt buộc phải có test

| Nghiệp vụ | Quy tắc | Tầng test |
| --- | --- | --- |
| Giữ chỗ: `AVAILABLE → HELD` | BR-08, BR-13 | Unit + Repository |
| Chặn người thứ hai giữ cùng một chỗ | BR-06, BR-08 | Repository |
| Xử lý hết hạn giữ chỗ | BR-14 | Unit |
| Bốn thay đổi cùng thành công hoặc cùng thất bại | BR-20 | Unit + Repository |
| Không tồn tại booking `CONFIRMED` mà chỗ vẫn `AVAILABLE` | BR-20 | Repository |
| Hủy booking trả chỗ về `AVAILABLE` | BR-26 | Unit |
| Số hành khách bằng số chỗ | BR-16 | Unit |
| Công thức giá và việc giá được đóng băng | BR-18, BR-19 | Unit |
| Customer bị chặn khi gõ tay `/admin/**` | BR-30 | Controller |
| Customer bị chặn khi mở booking của người khác | BR-31 | Controller |

CRUD admin đơn giản không bắt buộc có test riêng.

### 26.4 Quy ước

- **Class test phải kết thúc bằng `Test`** — nếu không, Surefire bỏ qua mà không
  báo lỗi. Đây là cái bẫy khiến người ta tưởng test đã chạy.
- Tên method mô tả hành vi mong đợi, ví dụ
  `confirmBooking_shouldFail_whenSeatAlreadyBooked`.
- File test đặt trong `src/test/java` với đúng cây package của class được test.

### 26.5 Bộ test không phụ thuộc Docker

Test chạy trên H2 in-memory khai trong
`app/src/test/resources/application.properties`. Không cần Docker, không cần
MySQL.

Đánh đổi: H2 ở chế độ MySQL không phản ánh hết hành vi MySQL thật, đặc biệt về
kiểu dữ liệu và khóa. Lỗi loại đó chỉ lộ ra khi chạy ứng dụng thật.

### 26.6 Kiểm tra giao diện

Ngoài test tự động, mỗi màn hình mới phải được xem ở **375px, 768px, 1280px** và
kiểm tra:

- Dấu tiếng Việt trên heading không bị cắt: tỉ lệ `line-height / font-size` từ
  1.15 trở lên.
- Không có thanh cuộn ngang ở cấp trang.
- Nội dung dài thật — tên ga dài, tên hành khách dài — không làm vỡ layout.
- Trang vẫn nguyên giao diện khi ngắt mạng (NFR-06).

### 26.7 Trạng thái hiện tại

`.\mvnw.cmd test` ngày 24/08/2026:

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Sáu test gồm `contextLoads`, và năm test của `HomeControllerTest` phủ view name,
model attribute, render bốn khối landing, link Bootstrap nội bộ, và `/vendor/**`
truy cập được khi chưa đăng nhập.

---

## 27. Ma trận truy vết

Ma trận nối yêu cầu với use case, quy tắc, dữ liệu và màn hình. Dùng để trả lời
hai câu hỏi: *"Yêu cầu này được hiện thực ở đâu?"* và *"Nếu sửa bảng này thì ảnh
hưởng tới đâu?"*

### 27.1 Yêu cầu chức năng → Use case → Màn hình

| FR | Use case | Màn hình | Quy tắc | Thực thể |
| --- | --- | --- | --- | --- |
| FR-01 | UC-01 | SC-03 | BR-28, BR-29 | EN-01 |
| FR-02 | UC-02 | SC-02 | BR-28 | EN-01 |
| FR-03 | UC-03 | mọi trang | BR-14 | — |
| FR-04 | mọi UC | mọi trang | BR-30, BR-31 | EN-01 |
| FR-05 | UC-04 | SC-01 | BR-10, BR-11, BR-12 | EN-02 |
| FR-06 | UC-05 | SC-04 | BR-23, BR-24 | EN-07, EN-08 |
| FR-07 | UC-06 | SC-05 | BR-18, BR-23 | EN-07, EN-05 |
| FR-08 | UC-04 | SC-01 | — | — |
| FR-09 | UC-07 | SC-06 | — | EN-05, EN-08 |
| FR-10 | UC-08 | SC-06 | — | EN-06, EN-08 |
| FR-11 | UC-08 | SC-06 | — | EN-06, EN-08 |
| FR-12 | UC-08 | SC-06 | BR-06, BR-08, BR-09, BR-13 | EN-08 |
| FR-13 | UC-08 | SC-06, SC-07, SC-08 | BR-13 | EN-08 |
| FR-14 | UC-08 | SC-06 | BR-14 | EN-08 |
| FR-15 | UC-09 | SC-07 | BR-16, BR-17 | EN-10 |
| FR-16 | UC-10 | SC-08 | BR-18, BR-19 | EN-09, EN-10 |
| FR-17 | UC-11 | SC-09, SC-10 | BR-20, BR-21, BR-22 | EN-08, EN-09, EN-10 |
| FR-18 | UC-11, UC-12 | SC-13 | BR-20 | EN-11 |
| FR-19 | UC-13 | SC-11 | BR-31 | EN-09 |
| FR-20 | UC-14 | SC-12 | BR-25, BR-31 | EN-09, EN-10 |
| FR-21 | UC-12 | SC-13 | BR-31 | EN-11 |
| FR-22 | UC-15 | SC-12 | BR-25, BR-26, BR-27 | EN-08, EN-09 |
| FR-23 | UC-16 | SC-14 | BR-30 | EN-09, EN-07, EN-01 |
| FR-24 | UC-17 | SC-15 | BR-01, BR-33 | EN-02 |
| FR-25 | UC-18 | SC-16 | BR-02 | EN-03 |
| FR-26 | UC-19 | SC-17 | BR-03 | EN-04 |
| FR-27 | UC-20 | SC-18 | BR-04 | EN-05, EN-06 |
| FR-28 | UC-21 | SC-19 | BR-05, BR-23, BR-34 | EN-07, EN-08 |
| FR-29 | UC-22, UC-23 | SC-20, SC-21 | BR-30, BR-32 | EN-09 |
| FR-30 | UC-24 | SC-21 | BR-25, BR-26, BR-27 | EN-08, EN-09 |
| FR-31 | UC-25 | SC-22 | BR-30, BR-35 | EN-01 |

### 27.2 Thực thể → Use case nào động vào

Đọc theo chiều ngược: sửa một bảng thì phải kiểm tra lại những use case này.

| Thực thể | Use case đọc | Use case ghi |
| --- | --- | --- |
| EN-01 `User` | UC-02, UC-25 | UC-01 |
| EN-02 `Station` | UC-04, UC-05, UC-06, UC-18 | UC-17 |
| EN-03 `Route` | UC-05, UC-06, UC-21 | UC-18 |
| EN-04 `Train` | UC-05, UC-06, UC-21 | UC-19 |
| EN-05 `Coach` | UC-06, UC-07, UC-10, UC-21 | UC-20 |
| EN-06 `Seat` | UC-08, UC-21 | UC-20 (hệ thống sinh) |
| EN-07 `Trip` | UC-05, UC-06, UC-08, UC-11, UC-13, UC-16 | UC-21 |
| EN-08 `TripSeat` | UC-05, UC-06, UC-07, UC-08, UC-10 | UC-08, UC-11, UC-15, UC-21, UC-24 |
| EN-09 `Booking` | UC-13, UC-14, UC-16, UC-22, UC-23 | UC-11, UC-15, UC-24 |
| EN-10 `BookingPassenger` | UC-10, UC-14, UC-23 | UC-09, UC-11 |
| EN-11 `Ticket` | UC-12, UC-14 | UC-11 (hệ thống sinh) |

`EN-08 TripSeat` bị ghi bởi **năm** use case — nhiều nhất hệ thống. Đây là lý do
mọi thay đổi liên quan tới bảng này đều phải chạy lại toàn bộ test nghiệp vụ.

### 27.3 Quy tắc nghiệp vụ → Nơi kiểm chứng

| Quy tắc | Use case | Test bắt buộc |
| --- | --- | --- |
| BR-06 unique `(tripId, seatId)` | UC-08, UC-11 | `@DataJpaTest` |
| BR-08 không giữ chỗ đã `HELD`/`BOOKED` | UC-08 | Unit + Repository |
| BR-13, BR-14 giữ chỗ 5 phút và hết hạn | UC-08 | Unit |
| BR-16 số hành khách bằng số chỗ | UC-09 | Unit |
| BR-18, BR-19 công thức giá và đóng băng giá | UC-10, UC-11 | Unit |
| BR-20, BR-21, BR-22 transaction xác nhận | UC-11 | Unit + Repository |
| BR-25, BR-26, BR-27 hủy booking | UC-15, UC-24 | Unit |
| BR-30 phân quyền theo vai trò | mọi UC admin | `@WebMvcTest` |
| BR-31 quyền sở hữu | UC-12, UC-14, UC-15 | `@WebMvcTest` |

---

## 28. Rủi ro và điểm chưa chốt

### 28.1 Rủi ro

| Rủi ro | Mức | Phòng bằng cách |
| --- | --- | --- |
| Giữ chỗ sai dẫn tới hai người cùng một ghế | **Cao** | Ràng buộc unique `(tripId, seatId)` ở database, kiểm tra lại ngay trước khi commit, test cho từng nhánh thất bại |
| Transaction không bao trọn bốn thay đổi trạng thái | **Cao** | `@Transactional` ở tầng service, test khẳng định rollback |
| Quên kiểm tra quyền sở hữu khi truy cập theo id | **Cao** | `@WebMvcTest` cho UC-12, UC-14, UC-15 |
| Chưa chốt cách thu hồi chỗ hết hạn | Trung bình | Phải quyết trước Giai đoạn 4, ghi vào plan |
| Lệch phiên bản Spring Boot 4 so với tài liệu 3.x | Trung bình | Kiểm tra jar thật trong `~/.m2/repository` trước khi tin bài viết trên mạng |
| `ddl-auto=update` làm hỏng schema khi entity đổi nhiều | Trung bình | `docker compose down -v` rồi chạy lại, chấp nhận mất dữ liệu dev |
| Tính năng optional lấn tiến độ | Trung bình | Không bắt đầu QR, bản đồ hay PDF trước khi Giai đoạn 7 xong |
| Tài liệu này lệch khỏi `docs/product/` | Thấp | Đóng dấu ngày, ghi rõ bản gốc, sinh lại HTML từ Markdown |

### 28.2 Điểm chưa chốt

Bốn việc phải quyết trước khi chạm vào phần liên quan:

| # | Điểm chưa chốt | Phải quyết trước | Ảnh hưởng |
| --- | --- | --- | --- |
| 1 | Cách thu hồi `TripSeat` hết hạn giữ: kiểm tra lười khi đọc sơ đồ, hay job định kỳ | Giai đoạn 4 | UC-08, BR-14 |
| 2 | Nội dung seed data: bao nhiêu ga, tuyến, tàu, chuyến mẫu, tài khoản admin nào | Giai đoạn 9 | Toàn bộ demo |
| 3 | Quy tắc xóa khi dữ liệu đã bị tham chiếu: chặn xóa hay xóa mềm | Giai đoạn 2 | UC-17..UC-21, BR-33 |
| 4 | Khi Admin hủy chuyến đã có booking `CONFIRMED`: tự hủy hàng loạt hay chặn hủy chuyến | Giai đoạn 8 | UC-21 |

Điểm 1 và 4 là quyết định nghiệp vụ, không phải kỹ thuật — người quyết phải là
người sở hữu sản phẩm, không phải người viết code. Đừng chọn ngầm trong code rồi
đi tiếp.

### 28.3 Giới hạn đã biết của phiên bản hiện tại

| Giới hạn | Sẽ hết khi |
| --- | --- |
| Form tìm chuyến submit sang `/trips` trả 404 | Giai đoạn 3 |
| Navbar luôn hiện Đăng nhập / Đăng ký vì chưa có auth | Giai đoạn 1 |
| Danh sách ga và tuyến phổ biến là dữ liệu tĩnh trong `HomeController` | Giai đoạn 2 |
| Ô `input type="date"` hiển thị theo locale trình duyệt, máy tiếng Anh ra `mm/dd/yyyy` | Chỉ hết nếu tự viết date picker — chưa đáng làm |
| H2 không phản ánh hết hành vi MySQL | Cân nhắc Testcontainers nếu gặp lỗi chỉ có trên MySQL |

---

*Tài liệu phân tích nghiệp vụ TrainGo · Phiên bản 1.0 · 24/08/2026*
*Bản gốc khi có mâu thuẫn: `SPEC.md` và `docs/product/`*

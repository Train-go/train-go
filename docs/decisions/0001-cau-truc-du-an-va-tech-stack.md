# 0001 Cấu trúc dự án và tech stack

Date: 2026-08-23

## Status

Accepted

## Context

Repository ban đầu chỉ có `SPEC.md` và một skeleton Spring Boot sinh từ Spring
Initializr, bị tạo trùng hai lần (`demo/` và `demo/demo/` giống hệt nhau), mang
tên mặc định `com.example.demo`. Nhóm ba người mới làm quen Java, Spring Boot,
Thymeleaf và JPA nên cần một cấu trúc rõ ràng, dễ chia việc, ít xung đột Git.

`SPEC.md` mục 43 và 44 đã chốt kiến trúc web application với Spring MVC,
Thymeleaf, Spring Data JPA và MySQL. Phần còn bỏ ngỏ là cách đặt thư mục, cách
chia package, mức độ tách client, và cách chạy MySQL ở máy dev.

## Decision

**Vị trí code.** Toàn bộ ứng dụng nằm trong `app/`. Gốc repository dành cho tài
liệu và bộ harness. Thư mục trùng lặp `demo/demo/` đã bị xóa.

**Định danh Maven.** `groupId` là `com.voyagego`, `artifactId` là `traingo`,
package gốc là `com.voyagego.traingo`, class khởi động là `TrainGoApplication`.

**Chia package theo feature.** Mỗi nghiệp vụ một package chứa đủ controller,
service, repository và entity của nó:

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

Bên trong mỗi feature package ở trên (trừ `common`), khi feature đó bắt đầu có
code thì chia tiếp thành các subpackage `controller/`, `model/`,
`repository/`, `service/`. Ví dụ đầy đủ đầu tiên là `station/`:

```
station/
├── controller/StationAdminController.java
├── model/Station.java, StationForm.java
├── repository/StationRepository.java
└── service/StationService.java, các exception của nghiệp vụ ga
```

`model/` chứa entity, enum và form object của feature. Exception nghiệp vụ nằm
cạnh service ném ra nó.

Đây **không phải** việc tái áp dụng phương án "chia theo tầng" đã bị bác bỏ ở
mục Alternatives: phạm vi mỗi subpackage vẫn nằm trong một feature duy nhất
(`station/controller/` khác thư mục với `booking/controller/`), nên hai người
làm hai feature khác nhau vẫn không đụng file của nhau. Không bắt buộc tạo sẵn
thư mục rỗng cho feature chưa có code; áp dụng dần khi feature đó bắt đầu
triển khai.

**Màn hình admin của một feature nằm trong package của feature đó**, không nằm
trong `admin/`. Ví dụ `/admin/stations` là
`station/controller/StationAdminController`, gọi thẳng `StationService`. Package
`admin/` chỉ giữ dashboard, `AdminShellModelAdvice` (dữ liệu header dùng chung)
và những màn hình còn dùng dữ liệu giả. Màn hình nào nối database thật thì
chuyển ra package của feature, cùng lúc xóa phần dữ liệu giả tương ứng trong
`AdminService` và `admin/model/`. Nhờ vậy hai người làm hai màn hình admin khác
nhau không cùng sửa một controller. Mốc quyết định: 01/10/2026.

**Mức tách client.** Thymeleaf render server-side cho phần lớn màn hình. Riêng
phần cần tương tác — sơ đồ ghế, giữ chỗ, đếm ngược 5 phút — dùng JavaScript
thuần trong `src/main/resources/static/js/` gọi các endpoint `@RestController`
dưới `/api/...` trả JSON. Không dựng dự án frontend riêng.

**Spring Boot 4.1.1**, Java 17 là target biên dịch.

**MySQL 8.4 chạy bằng Docker Compose** ở môi trường dev, khai báo trong
`app/compose.yaml`. Bộ test dùng H2 in-memory nên chạy được khi không có Docker.

## Alternatives Considered

1. **Chia package theo tầng** (`controller/`, `service/`, `repository/`,
   `entity/`). Giống đa số tutorial nên dễ đối chiếu khi học, nhưng cả ba thành
   viên sẽ cùng sửa `service/` và `controller/` trong mọi task, gây xung đột Git
   liên tục.
2. **Tách hẳn frontend React + REST API.** Cả nhóm quen React hơn Thymeleaf,
   nhưng lệch khỏi `SPEC.md`, phải xử lý CORS, và session-based authentication mà
   spec yêu cầu trở nên rắc rối hơn nhiều. Khối lượng công việc gần gấp đôi.
3. **Hạ Spring Boot về 3.5.x.** Nhiều tài liệu và ví dụ hơn hẳn, phù hợp người
   mới. Nhóm chọn giữ 4.1.1 để dùng bản mới nhất, chấp nhận chi phí tra cứu.
4. **Cài MySQL trực tiếp lên Windows.** Quen thuộc hơn, nhưng khó reset và khó
   chia sẻ cấu hình giống nhau cho cả ba máy.

## Consequences

Positive:

- Mỗi thành viên nhận một nhóm feature và làm việc gần như độc lập.
- Đọc một nghiệp vụ chỉ cần mở một package, không nhảy qua bốn thư mục.
- Bộ test không phụ thuộc Docker nên chạy được mọi lúc.
- Database dev giống hệt nhau trên cả ba máy, reset bằng một lệnh.

Tradeoffs:

- Spring Boot 4 đã tách các test slice thành từng module riêng. Chỉ có
  `spring-boot-starter-test` là **không** đủ để dùng `@WebMvcTest` hay
  `@DataJpaTest`; phải khai thêm `spring-boot-starter-webmvc-test` và
  `spring-boot-starter-data-jpa-test`. Package cũng đổi:
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` thay cho
  `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`.
- Tutorial 3.x sẽ sai ở một số chỗ. Đã gặp: `@MockBean` được thay bằng
  `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- Package theo feature khiến các ví dụ trên mạng trông khác cấu trúc của dự án;
  cần tự ánh xạ khi tham khảo.
- Chọn ghế phải viết JavaScript thuần, không có React để dựa vào.

## Follow-Up

- ~~Chốt tên hiển thị sản phẩm.~~ Đã chốt là **TrainGo** ngày 24/08/2026 —
  `docs/decisions/0003-he-thong-giao-dien.md`.
- ~~Quyết định cách quản lý schema về lâu dài: giữ `ddl-auto=update` hay chuyển
  sang Flyway trước khi có seed data thật.~~ Đã chốt Flyway ngày 01/10/2026,
  xem `docs/decisions/0005-quan-ly-schema-bang-flyway.md`.
- Cài JDK độc lập và trỏ `JAVA_HOME` vào đó thay vì JBR của Android Studio.

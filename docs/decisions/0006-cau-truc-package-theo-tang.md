# 0006 Cấu trúc package theo tầng

Date: 2026-10-11

## Status

Accepted. Thay thế mục "Chia package theo feature" của
`docs/decisions/0001-cau-truc-du-an-va-tech-stack.md`; các phần khác của 0001
(vị trí code, định danh Maven, mức tách client, tech stack) vẫn có hiệu lực.

## Context

ADR 0001 chia package theo feature (`station/`, `booking/`, `trip/`...), mỗi
feature lại có `controller/ model/ repository/ service/`. Sau khi có đủ 11
entity và màn hình ga đầu tiên, cách này cho ra nhiều package nhỏ, mỗi package
chỉ giữ một hai file; người mới làm quen Spring Boot phải mở nhiều thư mục để
đi hết một luồng, và tên package không trùng với cấu trúc quen thuộc trong
tài liệu, tutorial mà nhóm đối chiếu.

Nhóm quyết định đổi sang chia theo tầng. Alternatives #1 của ADR 0001 từng bác
bỏ phương án này vì lo xung đột Git; quyết định hiện tại chấp nhận rủi ro đó
(xem Consequences).

## Decision

Mọi code Java nằm trong năm tầng dưới package gốc `com.voyagego.traingo`. Mỗi
nghiệp vụ chỉ có **một file trong mỗi tầng** nó cần, không còn package riêng
cho từng nghiệp vụ:

```
com.voyagego.traingo
├── TrainGoApplication
├── common/config/        SecurityConfig, cấu hình dùng chung
├── common/exception/     exception nghiệp vụ (DuplicateStationCodeException...)
├── controller/           @Controller, @RestController, @ControllerAdvice
├── entity/               entity JPA và enum của entity (BookingStatus...)
├── model/
│   ├── dto/              dữ liệu trung gian giữa các tầng, không gắn với request hay response nào
│   ├── request/          object nhận từ form hoặc JSON, hậu tố Request
│   └── response/         object trả cho view hoặc JSON
├── repository/           Spring Data JPA
└── service/              nghiệp vụ và transaction
```

Quy ước:

- Tên file chứa tên nghiệp vụ: `StationAdminController`, `StationService`,
  `StationRepository`, `StationRequest`. Hai nghiệp vụ không bao giờ chung một
  file.
- `model/request` thay cho hậu tố `Form` cũ: `StationForm` thành
  `StationRequest`. Tên model attribute trong controller vẫn là `stationForm`
  nên template không đổi.
- Class giả của admin trùng tên entity được đổi thành `Admin*` trong
  `model/response` (`AdminBooking`, `AdminRoute`, `AdminTrain`, `AdminTrip`,
  `AdminUser`) và biến mất cùng `AdminService` khi màn hình tương ứng nối
  database.
- Enum đi cùng entity dùng nó, trong `entity/`.
- Exception nghiệp vụ nằm ở `common/exception/`, không còn cạnh service ném ra
  nó.
- `model/dto/` chưa có class nào; tạo khi có DTO đầu tiên (Git không lưu thư mục
  rỗng).
- Việc tách `admin/` và `home/` thành hai khu vực giao diện (ADR 0004) là chuyện
  của template, CSS, JS và URL, không liên quan package Java.

Hai việc không đổi so với ADR 0001: controller mỏng, service giữ transaction,
constructor injection; và màn hình admin của một nghiệp vụ vẫn có controller
riêng (`StationAdminController`) thay vì dồn vào `AdminController`.

## Alternatives Considered

1. **Giữ chia theo feature (ADR 0001).** Ít đụng file nhất khi nhiều người làm
   song song. Bị bác vì nhiều package gần như rỗng và khác cấu trúc tutorial.
2. **Chia theo tầng nhưng giữ `model/` phẳng.** Ít thư mục hơn, nhưng không
   phân biệt được form nhập, JSON trả ra và dữ liệu nội bộ.
3. **Đặt exception cạnh service** như ADR 0001. Gọn khi chỉ có một feature;
   với package service dùng chung thì lẫn exception với service, nên tách.

## Consequences

Positive:

- Muốn xem mọi controller, mọi entity hay mọi repository chỉ cần mở một thư
  mục; khớp cấu trúc tutorial Spring Boot.
- Không còn package chỉ chứa một hai file.
- Đổi từ `Form` sang `Request` cho thấy rõ file nào nhận dữ liệu từ ngoài.

Tradeoffs:

- Cả ba người cùng thêm file vào `controller/`, `service/`, `repository/`,
  `entity/`. Xung đột Git vẫn ít vì mỗi nghiệp vụ là một file riêng, nhưng khi
  hai người cùng sửa một entity dùng chung thì phải bàn trước.
- Không còn ranh giới package giữa các nghiệp vụ, nên cần kỷ luật: service của
  nghiệp vụ này gọi service/repository của nghiệp vụ khác phải có lý do rõ.
- Plan `docs/plans/active/` và các branch cũ (`feature/dashboard*`,
  `feat/database-and-station-admin`) còn đường dẫn kiểu cũ; merge cần đổi
  đường dẫn tay.

## Follow-Up

- Khi có DTO đầu tiên, tạo `model/dto/` và dùng nó cho dữ liệu trung gian (ví
  dụ `BookingDraft` của T09).
- Cân nhắc kiểm tra cấu trúc bằng ArchUnit nếu nhóm thấy tầng bị vi phạm (ví
  dụ controller gọi thẳng repository).

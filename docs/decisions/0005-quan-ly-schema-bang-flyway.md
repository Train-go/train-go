# 0005 Quản lý schema bằng Flyway

Date: 2026-10-01

## Status

Accepted

## Context

Tới hết Giai đoạn 0, ứng dụng chưa có entity nào. `application.properties` để
`spring.jpa.hibernate.ddl-auto=update`, nghĩa là Hibernate tự sửa bảng theo
entity mỗi lần khởi động. ADR 0001 ghi việc chọn giữa cách này và Flyway là
việc phải chốt "trước khi có seed data thật".

Ngày 01/10/2026 nhóm cần dựng toàn bộ database cùng relation và seed data trong
MySQL Docker, để các thành viên làm controller và giao diện trên dữ liệu thật.
Phi đã chọn:

- Flyway thay vì `ddl-auto=update` hoặc script init của Docker;
- tạo đủ 11 bảng của `docs/product/domain-model.md` ngay đợt này;
- seed bộ demo đầy đủ, admin là `admin@traingo.vn`.

## Decision

**Flyway là nơi duy nhất thay đổi schema.** Migration nằm trong
`app/src/main/resources/db/migration/`, chạy tự động khi ứng dụng khởi động:

| File | Nội dung |
| --- | --- |
| `V1__create_schema.sql` | 11 bảng, khóa chính, khóa ngoại, unique, `CHECK` |
| `V2__seed_demo_data.sql` | Dữ liệu demo |

**Hibernate chỉ kiểm tra, không sửa:** `spring.jpa.hibernate.ddl-auto=validate`.
Entity lệch schema thì ứng dụng dừng ngay lúc khởi động, không đợi tới khi một
trang nào đó lỗi.

**Migration đã chạy thì không sửa nữa.** Flyway lưu checksum từng file trong
bảng `flyway_schema_history` và từ chối khởi động khi checksum đổi. Muốn đổi
schema thì thêm file mới `V3__...`, `V4__...`.

**Quy ước schema:**

- Tên bảng số nhiều, cột `snake_case`. Entity viết `camelCase`, Spring Boot tự
  đổi tên (`coachNumber` → `coach_number`).
- Khóa ngoại tên `<bảng>_id`, ràng buộc có tên rõ: `pk_`, `fk_`, `uk_`, `ck_`,
  `idx_`. Lỗi từ database vì thế chỉ thẳng vào luật bị vi phạm, ví dụ
  `Check constraint 'ck_bookings_confirmed_is_paid' is violated`.
- Tiền là `BIGINT` theo đơn vị đồng, Java dùng `long`. VND không có phần lẻ,
  nên không cần `BigDecimal` và không bao giờ dùng `double`.
- Enum lưu `VARCHAR` kèm `CHECK` liệt kê giá trị hợp lệ. Entity phải khai cả
  `@Enumerated(EnumType.STRING)` lẫn `@JdbcTypeCode(SqlTypes.VARCHAR)`: thiếu
  dòng sau, Hibernate 7 đòi cột `ENUM` của MySQL và `validate` từ chối bảng.
- Khóa ngoại không có `ON DELETE`. Hàng đang được tham chiếu không xóa được;
  service phải kiểm tra trước và báo lý do bằng lời, như `StationService.delete`.
- Chỉ dùng cú pháp mà cả MySQL 8 lẫn H2 chế độ MySQL đều chấp nhận, để test sau
  này chạy được migration trên H2 (ADR 0002).

**Relation trong entity là `@ManyToOne(fetch = FetchType.LAZY)` một chiều.**
Không khai collection `@OneToMany`. Muốn lấy danh sách con thì query qua
repository, ví dụ toa của một tàu. Vì `spring.jpa.open-in-view=false`, mọi dữ
liệu view cần phải được nạp xong bên trong service có `@Transactional`. Đọc một
relation `LAZY` chưa nạp trong template sẽ ném `LazyInitializationException`.

**Seed dùng ngày tương đối.** `V2` tính ngày từ `CURRENT_DATE` lúc migration
chạy: chuyến chạy trong 14 ngày sau đó, hai chuyến đã chạy, một chuyến bị hủy.
Hết 14 ngày thì reset database để có ngày mới (`docs/RUNBOOK.md`).

**MySQL chạy giờ Việt Nam.** `compose.yaml` khai
`--default-time-zone=+07:00`, khớp `serverTimezone=Asia/Ho_Chi_Minh` trong JDBC
URL. Nếu để UTC, `CURRENT_DATE` của seed sẽ lùi một ngày trong khoảng 0-7 giờ
sáng, và thời điểm do seed ghi lệch 7 giờ so với thời điểm do ứng dụng ghi.

## Alternatives Considered

1. **Giữ `ddl-auto=update`, seed bằng code Java.** Không thêm dependency nào.
   Bị loại vì schema không nằm trong file nào để đọc hay review, Hibernate
   không bao giờ xóa cột hay đổi kiểu, và ba máy dễ có ba schema khác nhau mà
   không ai biết.
2. **Script SQL mount vào `/docker-entrypoint-initdb.d`.** Database có dữ liệu
   ngay sau `docker compose up`, chưa cần chạy ứng dụng. Bị loại vì script chỉ
   chạy khi volume trống: mỗi lần đổi schema, cả nhóm phải
   `docker compose down -v` và mất dữ liệu đang có.
3. **Liquibase.** Mạnh hơn Flyway, nhưng changelog XML/YAML là thêm một ngôn ngữ
   nữa cho nhóm đang học. Flyway chỉ cần SQL thuần.
4. **Lưu tiền bằng `BigDecimal`.** Là cách chuẩn cho tiền tệ có phần lẻ. VND
   không có phần lẻ nên `long` vừa chính xác vừa đơn giản hơn.

## Consequences

Positive:

- Schema đọc được trong một file, review được như code.
- Máy nào kéo code mới về, chạy ứng dụng là có migration mới, không mất dữ liệu.
- Entity lệch schema lộ ra ngay lúc khởi động.
- Luật dữ liệu quan trọng được chặn ở database, kể cả khi code service quên.

Tradeoffs:

- Đổi schema phải viết SQL bằng tay, không còn "sửa entity là xong".
- Migration sai đã chạy trên máy ai thì máy đó phải reset database.
- Mỗi trường mới phải sửa hai nơi: migration và entity. `validate` bắt được lỗi
  thiếu cột hay sai kiểu, nhưng không bắt được lệch độ dài cột hay ràng buộc.
- Seed tính theo ngày chạy, nên ngày của các máy khác nhau.

## Follow-Up

- Khi viết `@DataJpaTest` đầu tiên, kiểm tra migration có chạy được trên H2 hay
  không. Đến ngày 01/10/2026, migration mới chỉ được chạy trên MySQL 8.4.
- Auth phải dùng `BCryptPasswordEncoder`: hash trong seed là BCrypt thuần
  (`$2a$10$...`), không có tiền tố `{bcrypt}` mà `DelegatingPasswordEncoder` cần.

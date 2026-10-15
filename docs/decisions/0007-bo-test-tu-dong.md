# 0007 Bỏ test tự động

Date: 2026-10-12

## Status

Accepted. Thay cho `docs/decisions/0002-chien-luoc-test.md`.

## Context

ADR 0002 bắt buộc mọi task phải chạy `.\mvnw.cmd test` và viết test cho các nghiệp
vụ rủi ro: giữ chỗ, xác nhận booking, hủy booking, công thức giá, phân quyền.

Ngày 11/10/2026, commit `d6033aa` xóa toàn bộ test theo yêu cầu của nhóm. Hai test
cũ trong `AdminControllerTest` đã fail sẵn sau đợt đổi cấu trúc package. Từ đó repo
không còn test nào, trong khi `AGENTS.md` và 14 hướng dẫn task vẫn yêu cầu viết
test. Quy định và thực tế lệch nhau, người làm task không biết nên theo bên nào.

Ngày 12/10/2026, Phi chốt bỏ test tự động hẳn, thay vì dựng lại hạ tầng test.

## Decision

- Dự án không có test tự động. Không có thư mục `app/src/test`, không có task dựng
  hạ tầng test (task T01 cũ bị bỏ).
- Một task được coi là xong khi:
  - `.\mvnw.cmd compile` chạy không lỗi;
  - ứng dụng khởi động được bằng `.\mvnw.cmd spring-boot:run` (Flyway và
    `ddl-auto=validate` không báo lỗi);
  - người làm đã đi hết mục "Kịch bản thử bằng tay" trong file hướng dẫn của task,
    trên trình duyệt với MySQL và dữ liệu mẫu;
  - pull request được một người khác đọc qua.
- Những gì trước đây định viết thành test được chuyển thành kịch bản thử bằng tay.
  Với các tình huống khó tạo bằng tay, kịch bản hướng dẫn dùng SQL. Ví dụ: lùi
  `held_until` về quá khứ để thử hết hạn giữ chỗ mà không phải đợi 5 phút, hoặc dùng
  hai trình duyệt để thử hai người giữ cùng một ghế.
- Bỏ quy ước dùng bean `java.time.Clock`. Quy ước đó chỉ để test giả được thời gian;
  không còn test thì code dùng thẳng `LocalDateTime.now()`.
- Bỏ các dependency chỉ dùng cho test khỏi `app/pom.xml`.

## Alternatives Considered

1. **Giữ test như ADR 0002.** Bắt được lỗi khó thấy bằng mắt, nhất là hai người đặt
   trùng ghế và thanh toán dở dang. Không chọn vì nhóm muốn tập trung làm tính năng.
2. **Chỉ giữ test cho vài nghiệp vụ rủi ro nhất** (giữ chỗ, thanh toán, hủy booking).
   Bảo vệ đúng chỗ dễ sai mà tốn ít công. Không chọn; nhóm thống nhất bỏ hẳn cho đơn
   giản.

## Consequences

Positive:

- Mỗi task ít việc hơn; người mới không phải học thêm cách viết test Spring.
- Quy định trong repo khớp với thực tế.

Tradeoffs:

- Lỗi chỉ xảy ra khi hai người thao tác cùng lúc, hoặc đúng lúc hết hạn giữ chỗ, chỉ
  bắt được nếu người làm chịu khó chạy đủ kịch bản thử tay. Không có gì tự báo khi
  một thay đổi sau này làm hỏng luật cũ.
- Mỗi lần sửa phần giữ chỗ, đặt vé hay thanh toán, phải thử tay lại cả luồng.
- Migration Flyway chỉ được kiểm tra trên MySQL lúc ứng dụng khởi động.

## Follow-Up

- Trước buổi demo (T14), cả nhóm chạy lại kịch bản thử tay của toàn luồng đặt vé.
- Nếu về sau gặp lỗi lặp lại ở giữ chỗ hoặc thanh toán, cân nhắc thêm lại test cho
  riêng phần đó bằng một ADR mới.

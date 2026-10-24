---
phase: 1
title: Controller and security integration
status: completed
effort: 2–3h
priority: P1
dependencies: []
---

# Phase 1: Controller and security integration

## Overview

Nối MVC vào cơ chế auth hiện có, giữ hợp đồng phân quyền và không lặp nghiệp vụ service.
Trạng thái: completed (24/10/2026).

## Context Links

- [Tổng quan](./plan.md), [auth](../../../product/auth.md), [ADR 0013](../../../decisions/0013-quyen-truy-cap-mac-dinh-mo.md).
- Mã Java dưới `app/src/main/java/com/voyagego/traingo/`.

## Key Insights

- `SecurityConfig` đã dùng email, GET request cache ngoài API, success handler theo role và POST logout.
- `UserService.register` là transaction riêng, validate + normalize email + BCrypt + unique collision đã có.
- `DuplicateEmailException` đã kế thừa `BusinessRuleException`; tài liệu checkpoint nói RuntimeException đã cũ.
- `CurrentUserModelAdvice` cung cấp principal `currentUser`; không thêm cơ chế nhận diện người dùng thứ hai.

## Requirements

- GET login/register chuyển người đã đăng nhập theo role: ADMIN `/admin/trains`, CUSTOMER `/`.
- POST login do filter Spring Security xử lý. POST register do controller validate và gọi service.
- Lỗi đăng nhập không phân biệt email không tồn tại/mật khẩu sai. Giữ email ngắn hạn qua server,
  không URL, không giữ password trong session/log/flash attribute hoặc HTML trả về.
- Đăng ký thất bại giữ fullName/email/phone; mật khẩu trống; lỗi field tiếng Việt đúng vị trí.
- Giữ `hasRole` với authority `ROLE_*`, default mở, API JSON và CSRF hiện tại.

## Architecture

GET form → `AuthController` → template. POST login → security filter → failure/success handler.
POST register → Bean Validation → `UserService.register` → transaction commit → `request.login`
→ redirect `/`. Tự đăng nhập lỗi sau commit → flash thông báo an toàn → redirect `/login`.
Không annotate transaction ở controller; không rollback tài khoản đã tạo chỉ vì auto-login lỗi.

## Related Code Files

- Tạo: `controller/AuthController.java`; helper failure handler trong `common/config/` nếu cần tách trách nhiệm.
- Sửa: `common/config/SecurityConfig.java`, `common/config/RoleBasedLoginSuccessHandler.java`.
- Đọc/tái dùng: `model/request/RegisterRequest.java`, `service/UserService.java`,
  `service/TrainGoUserDetails.java`, `controller/CurrentUserModelAdvice.java`,
  `common/exception/DuplicateEmailException.java`.
- Không sửa schema, service nghiệp vụ hoặc request constraints nếu không có lỗi được chứng minh.

## Implementation Steps

1. Xác minh baseline và jar resolved nếu API Spring Security 7 còn chưa rõ; không suy từ ví dụ Boot 3.
2. Thêm GET `/login`, GET/POST `/register`, model nhất quán; dùng principal thật để redirect người đã login.
   POST `/register` cũng kiểm tra principal trước khi gọi service, tránh tạo thêm tài khoản từ phiên đã đăng nhập.
3. POST register nhận `@Valid` + `BindingResult`; giữ lỗi sẵn có và chuyển hai lỗi thuộc tính tổng hợp
   về field tương ứng. Bắt riêng `DuplicateEmailException`, không nuốt mọi lỗi service thành lỗi email.
4. Trả lại form lỗi với non-password fields. Không để `BindingResult.rejectedValue` phục hồi mật khẩu;
   password input không có binding/value render lại. Không log request DTO/password.
5. Sau service trả về và commit xong, gọi `request.login` bằng email đã normalize và mật khẩu vừa nhận.
   Dùng persistence/security context của Spring Security; kiểm chứng request kế tiếp nhận principal.
   Xóa request cache cũ để đăng ký thành công luôn về `/`, không lưu đích trước đăng ký cho lần login sau.
6. Nếu `request.login` ném lỗi, giữ tài khoản đã tạo, dùng thông báo chung “Tài khoản đã được tạo.
   Vui lòng đăng nhập.” và redirect login; không đưa exception nội bộ/credential lên giao diện.
7. Đổi formLogin sang `loginPage("/login")`, login processing POST `/login`, username `email`;
   giữ success handler theo role. Failure handler lưu duy nhất email tối đa 255 ký tự vào session key `AUTH_LOGIN_EMAIL`,
   redirect `/login?error`; không đưa email vào query. Login GET lấy rồi xóa;
   success handler xóa dữ liệu lỗi còn lại ở cả hai nhánh role. Không thêm TTL service,
   không đổi thời hạn session đăng nhập; không lưu toàn request hay password.
8. Giữ request cache chỉ GET ngoài API, query nguyên vẹn; không sửa luật URL/401/403/logout/CSRF.
9. Chạy `app/mvnw.cmd compile`; phase 3 xác minh form/filter/session thật sau khi có template.

## Todo List

- [x] Controller + binding lỗi + redirect người đã login.
- [x] Auto-login sau commit và fallback an toàn.
- [x] Custom login + email lỗi một lần và clear thành công.
- [x] Compile, kiểm tra diff không đổi hợp đồng security.

## Success Criteria

- [x] Controller chỉ điều phối, service giữ nghiệp vụ/transaction.
- [x] Lỗi synthetic map đúng field; password không xuất hiện trong response/session riêng/log.
- [x] Login success handler giữ hai hành vi role và xóa trạng thái lỗi.
- [x] Compile đạt; không dependency/migration/test mới.

## Risk Assessment

- Clear DTO không đủ nếu BindingResult còn rejectedValue: template dùng input password không value/binding phục hồi.
- Auto-login sau commit có thể lỗi riêng: fallback báo tài khoản đã tạo, không yêu cầu đăng ký lại.
- Session context có thể không được lưu nếu tự tạo Authentication: dùng `request.login`, chứng minh bằng request kế tiếp.
- Failure metadata bị đọc lại: consume once + clear success, chỉ giữ một email có giới hạn.

## Security Considerations

Không tự set role từ form, không lưu password plaintext hoặc redirect theo URL tùy ý do người dùng cung cấp.
Không tắt CSRF để làm form chạy. Request cache hiện tại tiếp tục quyết định đích login CUSTOMER.

## Next Steps

Phase 2 dùng hợp đồng model đã chốt; phase 3 chứng minh transaction, session và redirect bằng MySQL/trình duyệt.

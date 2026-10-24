---
phase: 3
title: Manual verification and documentation
status: in-progress
effort: 2h
priority: P1
dependencies:
  - 2
---

# Phase 3: Manual verification and documentation

## Overview

Kiểm chứng compile, startup, MySQL và luồng trình duyệt thật; cập nhật tài liệu theo kết quả quan sát.
Trạng thái: in-progress. Không tạo test suite/dependency theo ADR 0007.

## Context Links

- [Tổng quan](./plan.md), [ADR 0007](../../../decisions/0007-bo-test-tu-dong.md), [runbook](../../../RUNBOOK.md).
- [Auth](../../../product/auth.md), [hồ sơ tiến độ](../../../plans/active/tien-do-ticket-ca-nhan.md),
  [hướng dẫn T02](../../../plans/active/huong-dan-task/T02-dang-ky-dang-nhap-phan-quyen.md).

## Key Insights

Compile không chứng minh template/session đúng. Trang protected chưa triển khai có thể trả 404 sau redirect;
đó chỉ chứng minh URL/guard, không chứng minh đã có My Trips. Bằng chứng server cũ không thay thế thử UI mới.

## Requirements

- MySQL thật, seed hiện có, account thử riêng; không reset database, sửa migration hoặc đổi credential chung.
- Ghi actual result/status/URL và ảnh cần thiết; không commit credential, cookie/token hay log chứa password.
- Không coi checklist dự kiến là kết quả; đánh dấu rõ case chưa thử/blocker.
- Review cuối sau sửa lỗi; một dòng WORKLOG cùng commit task.

## Architecture

Maven compile → instance ứng dụng do phiên sở hữu → HTTP/browser với MySQL → kiểm persisted state
→ đối chiếu/review → docs. Tài khoản thử xác định bằng email/id riêng để cleanup chính xác.

## Related Code Files

- Kiểm chứng: toàn bộ file đổi trong phase 1/2, không tạo `app/src/test` hoặc sửa `pom.xml`.
- Cập nhật: `docs/product/auth.md`, mục thử tay của T02,
  `docs/plans/active/tien-do-ticket-ca-nhan.md`, `docs/WORKLOG.md`.
- Người điều phối sở hữu hồ sơ tiến độ/WORKLOG; không ghi chồng khi làm song song.

## Implementation Steps

1. Đọc runbook, xác minh MySQL và cổng; chạy `cd app; .\mvnw.cmd compile`, rồi
   `.\mvnw.cmd spring-boot:run`. Ghi BUILD SUCCESS/startup/Flyway/Hibernate và PID thuộc phiên.
   Không dừng app của người khác; dùng instance được xác định hoặc cổng riêng theo runbook.
2. Chạy ma trận bên dưới qua trình duyệt/HTTP thật; dữ liệu sai được gửi tới server kể cả khi native validation chặn.
3. Đối chiếu MySQL tài khoản mới: email lowercase, CUSTOMER, phone đúng, hash BCrypt khác plaintext;
   không đưa hash/password/token vào báo cáo. Auto-login phải còn hiệu lực qua reload/request sau redirect.
4. Với hai đăng ký cùng email đồng thời, chứng minh chỉ một dòng tồn tại, một thành công và một lỗi email;
   không dùng mock repository/hash. Bằng chứng server cũ là tham khảo, lần này kiểm controller/render lỗi.
5. Thử fallback auto-login lỗi bằng cách tái hiện có kiểm soát sau commit trên dữ liệu thử, không thêm endpoint
   hay nhánh production chỉ phục vụ kiểm chứng. Ghi cách gây lỗi và kết quả; nếu chưa tái hiện được thì nêu rõ
   khoảng trống, không đánh dấu case pass chỉ từ đọc code.
6. Kiểm tra ảnh 1440/390/320px và keyboard thật, console/network; sửa lỗi rồi kiểm lại case liên quan.
7. Review code sau compile/browser đạt: security policy không đổi, password không render/log/session riêng,
   không CSS/dependency/migration/test mới. Chạy `git diff --check`.
8. Cập nhật auth/T02: custom page, lỗi, auto-login/fallback, shell identity; bỏ mô tả stale generated login,
   thiếu shell và RuntimeException. Giữ checkpoint lịch sử, thêm trạng thái mới có ngày/bằng chứng.
9. Cập nhật trạng thái plan bằng CLI khi thật sự đạt, read-back toàn bộ bốn file và đối chiếu contract/links;
   parent cập nhật hồ sơ tiến độ + WORKLOG. Dọn đúng tài khoản thử, dừng đúng process phiên tạo, giữ MySQL.

## Kịch bản thử bằng tay

| Nhóm | Thao tác | Kết quả cần quan sát |
| --- | --- | --- |
| Trang public | Guest GET `/login`, `/register`, đường dẫn lạ | Hai form 200; đường dẫn lạ 404 tiếng Việt, không redirect login |
| Login lỗi | Email lạ, sai password; email có ký tự HTML | Cùng thông báo chung; email escaped giữ một lần; password trống; URL không email |
| Metadata một lần | Reload sau consume; login thành công rồi quay lại | Không khôi phục email lỗi cũ; không tồn tại trạng thái lỗi sau success |
| Login đúng | Seed CUSTOMER/ADMIN với email khác hoa/thường | CUSTOMER `/`, ADMIN `/admin/trains`; tên đúng; mật khẩu seed hợp lệ |
| Người đã login | Mở lại login/register và gửi POST register hợp lệ có CSRF ở cả hai role | Chuyển đích theo role; POST không tạo account mới từ session đang login |
| Saved request | Guest `/my-trips?status=upcoming`, gọi API guest rồi login CUSTOMER | Giữ nguyên path/query; API không ghi đè; 404 tại đích chưa có chỉ là proof redirect |
| Cache/ADMIN | Saved GET rồi login ADMIN; thử POST protected | ADMIN luôn `/admin/trains`; POST không thành saved request |
| Đăng ký đúng | Tạo account riêng với email có khoảng trắng/hoa thường | Lưu normalized email, CUSTOMER/phone/hash; auto-login về `/`, tên đúng sau reload |
| Form lỗi | Trống, tên >100, email sai/>255, phone sai/bỏ số 0 đầu/non-ASCII | Lỗi inline đúng; fullName/email/phone giữ; password/confirmPassword rỗng |
| Biên password | <8 ký tự; lệch confirm; 72/73 ASCII và 24/25 ký tự € | Giới hạn 72 byte đúng; lỗi vào password hoặc confirmPassword, không synthetic field |
| Trùng email | Khác hoa/thường + hai request đăng ký đồng thời | Lỗi email tiếng Việt; giữ form an toàn; chỉ một account |
| Auto-login lỗi | Gây lỗi có kiểm soát sau tài khoản commit | Account tồn tại; về login với account-created notice; login sau đó được |
| HTML roles | Guest admin; CUSTOMER admin; ADMIN `/my-trips` | Guest login redirect; role sai 403, trang lỗi vẫn render |
| API roles | GET `/api/admin/...` ở guest/CUSTOMER/ADMIN | 401 JSON/403 JSON/qua guard; endpoint chưa có có thể 404; không HTML redirect |
| CSRF | POST login/register/logout thiếu hoặc sai token | 403; không tạo account/đăng nhập/đăng xuất; POST API có thể 403 trước auth |
| Logout UI | Nút navbar CUSTOMER, navbar ADMIN và sidebar ADMIN | POST có token về `/`; guest nav; mở protected lại yêu cầu login |
| Layout | Cả hai form và nav guest/CUSTOMER/ADMIN ở 1440/390/320px | Không tràn ngang/cắt form; footer hợp lý; tên dài không phá navbar |
| Keyboard | Tab/Shift+Tab/Enter/Space, password toggle, menu mobile, lỗi | Focus thấy được; đúng thứ tự; toggle không submit; label/lỗi liên kết; không trap |
| Không JS | Login/register với JS tắt | Submit và lỗi server hoạt động; password vẫn masked |

## Todo List

- [x] Compile + startup thật, MySQL/Flyway/Hibernate qua.
- [x] Ma trận auth/register/role/API/CSRF/saved request/auto-login/logout đạt, trừ case auto-login lỗi sau commit (chưa tái hiện được: DB user không có quyền tạo trigger tạm).
- [ ] Responsive + keyboard + no-JS + DOM không chứa password đã nhập. Đã xong: DOM không chứa password, 320px/390px không tràn ngang. Còn lại: bàn phím, tắt JS (FE sẽ chỉnh giao diện).
- [x] Review, diff check, docs và WORKLOG có evidence; cleanup đúng tài nguyên.

## Success Criteria

- [x] Bằng chứng có kết quả thật cho từng case, hoặc gap công khai chưa hoàn thành.
- [x] Không regression role/404/API/CSRF, không thông tin nhạy cảm trong artifact Git.
- [x] Tài liệu mô tả đúng source hiện tại; không còn blocker shell giả hoặc exception cũ.
- [x] Plan thống nhất cả bốn file, links hợp lệ sau lifecycle move; không hai bản plan.

## Risk Assessment

- Dữ liệu test làm bẩn DB: ghi id/email riêng, xóa đúng account sau kiểm tra quan hệ; không xóa hàng seed.
- Session cũ che lỗi auto-login: tách browser context, xác minh principal ở request mới, thử logout rồi login.
- Fallback khó tái hiện: công khai cách kiểm chứng hoặc gap, không thêm cấu hình backdoor hay tuyên bố pass giả.
- Tool/browser bị chặn: ghi blocker và bằng chứng phần đã làm, không thay browser proof bằng compile.

## Security Considerations

Không chụp/log password/cookie/token; không commit file tạm riêng tư. Thử quyền qua URL trực tiếp,
không chỉ nhìn menu. Giữ credential và cài đặt hệ thống của người dùng.

## Next Steps

Chỉ đánh dấu hoàn thành sau bằng chứng và review. Push/PR/merge thực hiện khi nằm trong quyền đã được giao;
không suy quyền xuất bản từ việc hoàn tất plan hay triển khai local.

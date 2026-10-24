---
title: Hoàn thiện UI đăng nhập và đăng ký
description: Nối server auth hiện có với form Bootstrap đơn giản và danh tính trên shell.
status: in-progress
priority: P1
branch: feat/auth-ui
tags:
  - auth
  - thymeleaf
  - bootstrap
effort: 5–7h
blockedBy: []
blocks: []
created: '2026-10-24T13:16:08.911Z'
createdBy: 'ck:plan'
source: skill
---

# Hoàn thiện UI đăng nhập và đăng ký

## Overview

Hoàn thiện `/login`, `/register`, danh tính và đăng xuất trên navbar khách/sidebar admin.
Form gọn, căn giữa theo chiều ngang, dùng component/class Bootstrap trong shell client hiện có.
Đã triển khai trên `feat/auth-ui`; ba phase chạy tuần tự, tổng ước lượng 5–7 giờ. Phase 3 còn khoảng trống kiểm chứng, xem [hồ sơ tiến độ](../tien-do-ticket-ca-nhan.md).

Nguồn thẩm quyền: [auth](../../../product/auth.md), [ADR 0013](../../../decisions/0013-quyen-truy-cap-mac-dinh-mo.md),
[ADR 0007](../../../decisions/0007-bo-test-tu-dong.md), [AGENTS](../../../../AGENTS.md),
[workflow](../../../WORKFLOW.md). Yêu cầu phiên này: chỉ Bootstrap, không thêm CSS/override/include mới.

## Phạm vi và quyết định đã chốt

- Dùng `layout/base`, `compactFooter=true`, font/màu sẵn có; không thiết kế lại shell.
- Login dùng Spring Security, `email`/`password`, lỗi chung tiếng Việt, giữ email một lần;
  CUSTOMER quay về GET đã lưu nguyên query, ADMIN về `/admin/trains`.
- Đăng ký gọi `UserService.register`, sau khi commit mới `request.login`, thành công về `/`;
  nếu tự đăng nhập thất bại, về login với thông báo tài khoản đã tạo.
- Giữ dữ liệu form ngoài hai mật khẩu; map `passwordConfirmed` → `confirmPassword`,
  `passwordWithinLimit` → `password`, email trùng → `email`.
- Giữ default `permitAll`, năm tiền tố bảo vệ, JSON API 401/403 và CSRF.
- Không migration, dependency, test suite, OAuth, quên mật khẩu, API auth mới hay session tự quản.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Controller and security integration](./phase-01-controller-and-security-integration.md) | Completed |
| 2 | [Bootstrap forms and account navigation](./phase-02-bootstrap-forms-and-account-navigation.md) | Completed |
| 3 | [Manual verification and documentation](./phase-03-manual-verification-and-documentation.md) | In Progress |

## Dependencies

Server #22/#23 và shell #20/#21 đã có, không còn blocker shell. Source hiện tại đã có
`DuplicateEmailException extends BusinessRuleException`; không làm lại server đã kiểm chứng.
[Hồ sơ tiến độ](../../../plans/active/tien-do-ticket-ca-nhan.md) có checkpoint lịch sử cũ;
người điều phối cập nhật liên kết và trạng thái hiện tại, không dùng trạng thái issue để suy ra UI tồn tại.

## Bằng chứng hoàn thành

- Compile thành công; Maven khởi động trên MySQL thật, Flyway/Hibernate validate qua.
- Đi hết ma trận trình duyệt ở phase 3, gồm 1440/390/320px, bàn phím, lỗi và role/CSRF.
- Review diff, cập nhật tài liệu auth/hướng dẫn thử tay/hồ sơ tiến độ và một dòng WORKLOG.
- Tách rõ kết quả đã kiểm chứng, phần chưa thử và lỗi còn lại; chưa có bằng chứng thì chưa đánh dấu hoàn tất.

## Vòng đời và khôi phục

Sau khi được chấp thuận, di chuyển **toàn bộ thư mục** sang `docs/plans/active/auth-ui/`,
chỉnh liên kết tương đối và liên kết từ hồ sơ; không giữ hai bản. Hoàn tất kiểm chứng mới chuyển
sang `docs/plans/completed/auth-ui/`. Cập nhật trạng thái phase bằng CLI `ck plan check`.
Nếu cần hoàn tác triển khai, revert commit của task; không reset DB/migration hoặc sửa công việc khác.
Chỉ dọn tài khoản thử và process do phiên kiểm chứng tạo, sau khi xác định chính xác quyền sở hữu.

## Câu hỏi còn mở

Không còn lựa chọn sản phẩm cần hỏi. Cơ chế persistence của `request.login` và API Spring Security 7
được xác minh bằng jar resolved tại máy và runtime trong phase 1/3 nếu có điểm chưa rõ.

package com.voyagego.traingo.model.request;

import java.util.Objects;
import java.nio.charset.StandardCharsets;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
	@NotBlank(message = "Vui lòng nhập họ tên.")
	@Size(max = 100, message = "Họ tên không quá 100 ký tự.")
	private String fullName;

	@NotBlank(message = "Vui lòng nhập email.")
	@Email(message = "Email không đúng định dạng.")
	@Size(max = 255, message = "Email không quá 255 ký tự.")
	private String email;

	@NotBlank(message = "Vui lòng nhập số điện thoại.")
	@Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.")
	private String phoneNumber;

	@NotBlank(message = "Vui lòng nhập mật khẩu.")
	@Size(min = 8, message = "Mật khẩu có ít nhất 8 ký tự.")
	private String password;

	@NotBlank(message = "Vui lòng nhập lại mật khẩu.")
	private String confirmPassword;

	// The page controller maps passwordConfirmed violations to confirmPassword.
	@AssertTrue(message = "Hai mật khẩu không giống nhau.")
	public boolean isPasswordConfirmed() {
		return Objects.equals(password, confirmPassword);
	}

	// BCrypt rejects input above 72 UTF-8 bytes instead of truncating it.
	@AssertTrue(message = "Mật khẩu không quá 72 byte UTF-8; ký tự có dấu có thể chiếm nhiều byte.")
	public boolean isPasswordWithinLimit() {
		return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
	}

	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email == null ? null : email.strip(); }
	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public String getConfirmPassword() { return confirmPassword; }
	public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}

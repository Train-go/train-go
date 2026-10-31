package com.voyagego.traingo.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The fields a signed-in user may change on /profile; same rules as RegisterRequest. */
public class ProfileRequest {
	@NotBlank(message = "Vui lòng nhập họ tên.")
	@Size(max = 100, message = "Họ tên không quá 100 ký tự.")
	private String fullName;

	@NotBlank(message = "Vui lòng nhập số điện thoại.")
	@Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.")
	private String phoneNumber;

	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName == null ? null : fullName.strip(); }
	public String getPhoneNumber() { return phoneNumber; }
	public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber == null ? null : phoneNumber.strip(); }
}

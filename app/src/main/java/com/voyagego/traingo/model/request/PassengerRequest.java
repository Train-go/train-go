package com.voyagego.traingo.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One passenger line of the passenger page. Every ticket carries a name and a
 * 12-digit CCCD (D10); the account stores no CCCD, so even a ticket in the
 * account holder's own name needs one.
 */
public class PassengerRequest {

	@NotBlank(message = "Nhập họ tên người đi.")
	@Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
	private String fullName;

	@Pattern(regexp = "\\d{12}", message = "Số CCCD gồm đúng 12 chữ số.")
	private String identityNumber;

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName == null ? null : fullName.trim();
	}

	public String getIdentityNumber() {
		return identityNumber;
	}

	public void setIdentityNumber(String identityNumber) {
		this.identityNumber = identityNumber == null ? null : identityNumber.trim();
	}

}

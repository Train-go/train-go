package com.voyagego.traingo.model.request;

import com.voyagego.traingo.entity.Train;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the add and edit sheets of /admin/trains send as JSON. Kept apart from
 * the entity so a request can only ever set the code and the name.
 *
 * The code may arrive in lower case and with spaces around it ("se4 "):
 * the pattern allows both, and TrainService stores it as Train.normalizeCode
 * returns it.
 */
public class TrainRequest {

	@NotBlank(message = "Vui lòng nhập mã tàu.")
	@Pattern(regexp = "\\s*[A-Za-z]{1,4}\\d{1,4}\\s*", message = "Mã tàu gồm 1-4 chữ cái rồi 1-4 chữ số, ví dụ SE4.")
	private String code;

	@NotBlank(message = "Vui lòng nhập tên tàu.")
	@Size(max = Train.NAME_MAX_LENGTH, message = "Tên tàu tối đa {max} ký tự.")
	private String name;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

package com.voyagego.traingo.station.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the add and edit forms post. Kept apart from the entity so a request
 * can only ever set these four fields, never the id.
 *
 * The controller trims every field and turns blank ones into null before
 * validation, so the rules below see "SGN" rather than "  SGN ".
 */
public class StationForm {

	@NotBlank(message = "Vui lòng nhập mã ga.")
	@Size(max = Station.CODE_MAX_LENGTH, message = "Mã ga tối đa {max} ký tự.")
	@Pattern(regexp = "[A-Za-z0-9]+", message = "Mã ga chỉ gồm chữ cái không dấu và chữ số.")
	private String code;

	@NotBlank(message = "Vui lòng nhập tên ga.")
	@Size(max = Station.NAME_MAX_LENGTH, message = "Tên ga tối đa {max} ký tự.")
	private String name;

	@Size(max = Station.CITY_MAX_LENGTH, message = "Tỉnh / thành phố tối đa {max} ký tự.")
	private String city;

	@Size(max = Station.ADDRESS_MAX_LENGTH, message = "Địa chỉ tối đa {max} ký tự.")
	private String address;

	public static StationForm from(Station station) {
		StationForm form = new StationForm();
		form.code = station.getCode();
		form.name = station.getName();
		form.city = station.getCity();
		form.address = station.getAddress();
		return form;
	}

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

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

}

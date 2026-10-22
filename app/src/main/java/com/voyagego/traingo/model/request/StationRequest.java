package com.voyagego.traingo.model.request;

import com.voyagego.traingo.entity.Station;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the add and edit forms post. Kept apart from the entity so a request
 * can only ever set these five fields, never the id.
 *
 * The controller trims every field and turns blank ones into null before
 * validation, so the rules below see "SGN" rather than "  SGN ".
 */
public class StationRequest {

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

	// Text that is not a whole number never reaches these rules: binding fails
	// first and messages.properties words that error (typeMismatch.kmMark).
	@NotNull(message = "Vui lòng nhập lý trình.")
	@Min(value = 0, message = "Lý trình không được nhỏ hơn 0.")
	@Max(value = KM_MARK_MAX, message = "Lý trình tối đa {value} km.")
	private Integer kmMark;

	/** Well past the 1726 km of the whole line, so only a typo goes over it. */
	public static final int KM_MARK_MAX = 5000;

	public static StationRequest from(Station station) {
		StationRequest form = new StationRequest();
		form.code = station.getCode();
		form.name = station.getName();
		form.city = station.getCity();
		form.address = station.getAddress();
		form.kmMark = station.getKmMark();
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

	public Integer getKmMark() {
		return kmMark;
	}

	public void setKmMark(Integer kmMark) {
		this.kmMark = kmMark;
	}

}

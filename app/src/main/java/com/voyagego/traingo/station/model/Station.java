package com.voyagego.traingo.station.model;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A railway station (docs/product/domain-model.md). Hibernate creates the
 * "stations" table from this class on startup (ddl-auto=update), so there is
 * no SQL script to keep in step with it.
 */
@Entity
@Table(name = "stations", uniqueConstraints = @UniqueConstraint(name = "uk_stations_code", columnNames = "code"))
public class Station {

	// Column sizes, shared with StationForm so validation and schema agree.
	public static final int CODE_MAX_LENGTH = 10;
	public static final int NAME_MAX_LENGTH = 100;
	public static final int CITY_MAX_LENGTH = 100;
	public static final int ADDRESS_MAX_LENGTH = 255;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = CODE_MAX_LENGTH)
	private String code;

	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Column(length = CITY_MAX_LENGTH)
	private String city;

	@Column(length = ADDRESS_MAX_LENGTH)
	private String address;

	/** Required by JPA. */
	protected Station() {
	}

	public Station(String code, String name, String city, String address) {
		update(code, name, city, address);
	}

	public void update(String code, String name, String city, String address) {
		this.code = normalizeCode(code);
		this.name = name;
		this.city = city;
		this.address = address;
	}

	/**
	 * Codes are stored upper case, so "sgn" and "SGN" can never both exist and
	 * the uniqueness check does not depend on the database collation.
	 */
	public static String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getCity() {
		return city;
	}

	public String getAddress() {
		return address;
	}

}

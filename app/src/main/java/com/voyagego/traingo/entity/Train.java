package com.voyagego.traingo.entity;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A train. Every train has the same fixed coach layout, which is not stored
 * here: it is copied onto TripSeat when a trip is created. A Train runs many
 * Trips, so booking state never lives here but on TripSeat.
 *
 * A train is soft deleted: the row stays so its trips and tickets keep it. Only
 * trains that are not deleted need distinct codes, so a deleted train's code
 * can be reused. V8 enforces that with a unique key on a generated column
 * (active_code), which this class does not map.
 */
@Entity
@Table(name = "trains")
public class Train {

	// Column sizes, shared with TrainRequest so validation and schema agree.
	public static final int CODE_MAX_LENGTH = 10;
	public static final int NAME_MAX_LENGTH = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** For example SE1 or SNT2. */
	@Column(nullable = false, length = CODE_MAX_LENGTH)
	private String code;

	/** For example "Thống Nhất". */
	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	/** Hidden from the trains page; restored with {@link #restore()}. */
	@Column(name = "is_delete", nullable = false)
	private boolean deleted;

	/** Required by JPA. */
	protected Train() {
	}

	public Train(String code, String name) {
		update(code, name);
	}

	public void update(String code, String name) {
		this.code = normalizeCode(code);
		this.name = name.trim();
	}

	/**
	 * Codes are stored upper case, so "se4" and "SE4" can never both exist and
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

	public boolean isDeleted() {
		return deleted;
	}

	public void delete() {
		this.deleted = true;
	}

	public void restore() {
		this.deleted = false;
	}

}

package com.voyagego.traingo.train.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The physical make-up of a train: its coaches and their seats. A Train runs
 * many Trips, so booking state never lives here but on TripSeat.
 */
@Entity
@Table(name = "trains", uniqueConstraints = @UniqueConstraint(name = "uk_trains_code", columnNames = "code"))
public class Train {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** For example SE1 or SNT2. */
	@Column(nullable = false, length = 10)
	private String code;

	/** For example "Thống Nhất". */
	@Column(nullable = false, length = 100)
	private String name;

	/** Required by JPA. */
	protected Train() {
	}

	public Train(String code, String name) {
		this.code = code;
		this.name = name;
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

}

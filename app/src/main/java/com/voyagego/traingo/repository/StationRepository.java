package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Station;

public interface StationRepository extends JpaRepository<Station, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Station s where s.id = :id")
	Optional<Station> findByIdForUpdate(Long id);

	/** North to south along the line. */
	List<Station> findAllByOrderByKmMarkAsc();

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

	boolean existsByKmMark(int kmMark);

	boolean existsByKmMarkAndIdNot(int kmMark, Long id);

}

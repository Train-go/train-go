package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Station;

public interface StationRepository extends JpaRepository<Station, Long> {

	List<Station> findAllByOrderByNameAsc();

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);

}

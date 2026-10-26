package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.voyagego.traingo.entity.Train;

/**
 * "DeletedFalse" methods see only trains that are not soft deleted, the ones
 * the admin works with. Trips and bookings still reach a deleted train through
 * their own relation.
 */
public interface TrainRepository extends JpaRepository<Train, Long> {

	List<Train> findAllByDeletedFalseOrderByCodeAsc();

	List<Train> findAllByDeletedTrueOrderByCodeAsc();

	Optional<Train> findByIdAndDeletedFalse(Long id);

	Optional<Train> findByIdAndDeletedTrue(Long id);

	boolean existsByCodeAndDeletedFalse(String code);

	boolean existsByCodeAndDeletedFalseAndIdNot(String code, Long id);

}

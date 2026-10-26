package com.voyagego.traingo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.Train;

import jakarta.persistence.LockModeType;

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

	/**
	 * A train that is not deleted, locked for writing. Adding trips takes this
	 * lock first, so two admins cannot both chain a trip after the same last
	 * trip of one train.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Train t where t.id = :id and t.deleted = false")
	Optional<Train> findByIdForUpdate(Long id);

}

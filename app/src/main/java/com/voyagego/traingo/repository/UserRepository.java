package com.voyagego.traingo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.voyagego.traingo.entity.User;
import com.voyagego.traingo.entity.UserRole;
import com.voyagego.traingo.model.dto.DayCount;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	/** Newest account first, for the admin's user list. */
	List<User> findByRoleOrderByCreatedAtDesc(UserRole role);

	long countByRole(UserRole role);

	/**
	 * Customers registered per day in [from, to). Native SQL for the same reason
	 * as the other per-day queries: JPQL cannot group by the date of a timestamp.
	 */
	@Query(value = """
			select date(u.created_at) as day, count(*) as total
			from users u
			where u.role = 'CUSTOMER' and u.created_at >= :from and u.created_at < :to
			group by date(u.created_at)
			order by day""", nativeQuery = true)
	List<DayCount> findSignupsByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	/** Users of one role registered before {@code time}, the starting point of a running total. */
	long countByRoleAndCreatedAtBefore(UserRole role, LocalDateTime time);

}

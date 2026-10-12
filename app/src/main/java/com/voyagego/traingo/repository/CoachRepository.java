package com.voyagego.traingo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.voyagego.traingo.entity.Coach;
import com.voyagego.traingo.model.dto.TrainCapacity;

public interface CoachRepository extends JpaRepository<Coach, Long> {

	/** Coach and seat totals of every train that has at least one coach. */
	@Query("""
			select c.train.id as trainId, count(c) as coachCount, sum(c.capacity) as seatCount
			from Coach c
			group by c.train.id
			""")
	List<TrainCapacity> summarizeByTrain();

}

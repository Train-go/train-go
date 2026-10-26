package com.voyagego.traingo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.DuplicateTrainCodeException;
import com.voyagego.traingo.common.exception.TrainCodeInUseException;
import com.voyagego.traingo.common.exception.TrainCodeLockedException;
import com.voyagego.traingo.common.exception.TrainHasUnfinishedTripsException;
import com.voyagego.traingo.common.exception.TrainNotFoundException;
import com.voyagego.traingo.entity.Train;
import com.voyagego.traingo.model.request.TrainRequest;
import com.voyagego.traingo.repository.TrainRepository;
import com.voyagego.traingo.repository.TripRepository;

/**
 * Train rules from docs/product/admin.md that need the database: a code is
 * used by one train in use only, it no longer changes once the train has
 * trips, and a train is soft deleted only when none of its trips is still
 * upcoming or running. The shape of each field is checked earlier, on
 * TrainRequest.
 *
 * Every method except {@link #findDeleted()} and {@link #restore(long)} works
 * on trains that are not deleted; a deleted one answers TrainNotFoundException.
 */
@Service
public class TrainService {

	private final TrainRepository trainRepository;
	private final TripRepository tripRepository;

	public TrainService(TrainRepository trainRepository, TripRepository tripRepository) {
		this.trainRepository = trainRepository;
		this.tripRepository = tripRepository;
	}

	/** Trains in use, sorted by code: SE1, SE2, SE3. */
	@Transactional(readOnly = true)
	public List<Train> findAll() {
		return trainRepository.findAllByDeletedFalseOrderByCodeAsc();
	}

	@Transactional(readOnly = true)
	public List<Train> findDeleted() {
		return trainRepository.findAllByDeletedTrueOrderByCodeAsc();
	}

	@Transactional(readOnly = true)
	public Train findById(long id) {
		return trainRepository.findByIdAndDeletedFalse(id).orElseThrow(TrainNotFoundException::new);
	}

	/** Any trip, cancelled ones included: the code is locked from the first one. */
	@Transactional(readOnly = true)
	public boolean hasTrips(long id) {
		return tripRepository.existsByTrainId(id);
	}

	/** Trips that block {@link #delete}: not cancelled and not yet arrived. */
	@Transactional(readOnly = true)
	public long countUnfinishedTrips(long id, LocalDateTime now) {
		return tripRepository.countUnfinishedByTrain(id, now);
	}

	/**
	 * Two requests can pass the existence check at the same moment. The unique
	 * key on active_code catches the second one, and its
	 * DataIntegrityViolationException leaves this method uncaught so the
	 * transaction rolls back cleanly.
	 */
	@Transactional
	public Train create(TrainRequest form) {
		String code = Train.normalizeCode(form.getCode());
		if (trainRepository.existsByCodeAndDeletedFalse(code)) {
			throw new DuplicateTrainCodeException();
		}
		return trainRepository.save(new Train(code, form.getName()));
	}

	@Transactional
	public Train update(long id, TrainRequest form) {
		Train train = findById(id);
		String code = Train.normalizeCode(form.getCode());
		boolean codeChanged = !code.equals(train.getCode());
		if (codeChanged && tripRepository.existsByTrainId(id)) {
			throw new TrainCodeLockedException();
		}
		if (codeChanged && trainRepository.existsByCodeAndDeletedFalseAndIdNot(code, id)) {
			throw new DuplicateTrainCodeException();
		}
		train.update(code, form.getName());
		// Write now instead of at commit, so a code taken by a concurrent
		// request fails here with the same exception create() gives.
		trainRepository.flush();
		return train;
	}

	/**
	 * Soft delete: the train leaves the trains page, its trips and tickets stay
	 * as they are. Only when every trip has arrived or been cancelled, so no
	 * customer is left holding a ticket for a train that has disappeared.
	 */
	@Transactional
	public Train delete(long id, LocalDateTime now) {
		Train train = findById(id);
		long unfinished = tripRepository.countUnfinishedByTrain(id, now);
		if (unfinished > 0) {
			throw new TrainHasUnfinishedTripsException(unfinished);
		}
		train.delete();
		return train;
	}

	/**
	 * Brings a deleted train back, unless a train in use has taken its code
	 * meanwhile. The unique key on active_code catches a concurrent request
	 * that takes the code between the check and the flush.
	 */
	@Transactional
	public Train restore(long id) {
		Train train = trainRepository.findByIdAndDeletedTrue(id).orElseThrow(TrainNotFoundException::new);
		if (trainRepository.existsByCodeAndDeletedFalse(train.getCode())) {
			throw new TrainCodeInUseException(train.getCode());
		}
		train.restore();
		trainRepository.flush();
		return train;
	}

}

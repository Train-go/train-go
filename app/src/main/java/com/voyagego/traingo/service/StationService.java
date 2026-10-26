package com.voyagego.traingo.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.voyagego.traingo.common.exception.DuplicateStationCodeException;
import com.voyagego.traingo.common.exception.DuplicateStationKmMarkException;
import com.voyagego.traingo.common.exception.NoFreeKmException;
import com.voyagego.traingo.common.exception.StationCodeLockedException;
import com.voyagego.traingo.common.exception.StationInUseException;
import com.voyagego.traingo.common.exception.StationNotFoundException;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.request.StationRequest;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;

/**
 * Station rules from docs/product/admin.md that need the database: a code and
 * a km mark are each used by one station only, and a station in use cannot be
 * deleted. The shape of each field is checked earlier, on StationRequest.
 */
@Service
public class StationService {

	private final StationRepository stationRepository;
	private final RouteRepository routeRepository;

	public StationService(StationRepository stationRepository, RouteRepository routeRepository) {
		this.stationRepository = stationRepository;
		this.routeRepository = routeRepository;
	}

	/** North to south along the line. */
	@Transactional(readOnly = true)
	public List<Station> findAll() {
		return stationRepository.findAllByOrderByKmMarkAsc();
	}

	@Transactional(readOnly = true)
	public Station findById(long id) {
		return stationRepository.findById(id).orElseThrow(StationNotFoundException::new);
	}

	/**
	 * Two requests can pass the existence checks at the same moment. The unique
	 * keys on the code and the km mark catch the second one, and its
	 * DataIntegrityViolationException leaves this method uncaught so the
	 * transaction rolls back cleanly.
	 */
	@Transactional
	public Station create(StationRequest form) {
		String code = Station.normalizeCode(form.getCode());
		int kmMark = form.getKmMark();
		if (stationRepository.existsByCode(code)) {
			throw new DuplicateStationCodeException();
		}
		stationRepository.findByKmMark(kmMark).ifPresent(owner -> {
			throw new DuplicateStationKmMarkException(kmMark, owner);
		});
		return stationRepository.save(new Station(code, form.getName(), form.getCity(), form.getAddress(), kmMark));
	}

	@Transactional
	public Station update(long id, StationRequest form) {
		// Route creation locks the same station, so it cannot slip between the
		// code check and the update.
		Station station = lockStation(id);
		String code = Station.normalizeCode(form.getCode());
		int kmMark = form.getKmMark();
		if (!Station.normalizeCode(station.getCode()).equals(code)
				&& routeRepository.existsByOriginStationIdOrDestinationStationId(id, id)) {
			throw new StationCodeLockedException();
		}
		if (stationRepository.existsByCodeAndIdNot(code, id)) {
			throw new DuplicateStationCodeException();
		}
		stationRepository.findByKmMarkAndIdNot(kmMark, id).ifPresent(owner -> {
			throw new DuplicateStationKmMarkException(kmMark, owner);
		});
		station.update(code, form.getName(), form.getCity(), form.getAddress(), kmMark);
		// Write now instead of at commit, so a code or km mark taken by a
		// concurrent request fails here with the same DataIntegrityViolationException
		// create() gives.
		stationRepository.flush();
		return station;
	}

	/**
	 * docs/product/admin.md only allows deleting a station nothing uses. Only
	 * routes point at stations; trips reach them through a route.
	 *
	 * The check runs before the delete on purpose. Letting the foreign key fail
	 * and catching DataIntegrityViolationException inside this transaction would
	 * leave it marked rollback-only, and the commit would then throw
	 * UnexpectedRollbackException instead of a message the admin can read.
	 */
	@Transactional
	public Station delete(long id) {
		// Holding the station lock also blocks new routes until deletion commits.
		Station station = lockStation(id);
		long count = countRoutes(id);
		if (count > 0) {
			throw new StationInUseException(count);
		}
		stationRepository.delete(station);
		return station;
	}

	@Transactional(readOnly = true)
	public long countRoutes(long id) {
		return routeRepository.countByOriginStationIdOrDestinationStationId(id, id);
	}

	@Transactional(readOnly = true)
	public int freeKmBetween(int lo, int hi) {
		if (lo >= hi) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		Set<Integer> occupied = new HashSet<>(stationRepository.findKmMarksBetween(lo, hi));
		long middle = ((long) lo + hi) / 2;
		for (long offset = 0; middle - offset > lo || middle + offset < hi; offset++) {
			long lower = middle - offset;
			if (lower > lo && lower < hi && !occupied.contains((int) lower)) {
				return (int) lower;
			}
			long upper = middle + offset;
			if (offset > 0 && upper > lo && upper < hi && !occupied.contains((int) upper)) {
				return (int) upper;
			}
		}
		throw new NoFreeKmException();
	}

	private Station lockStation(long id) {
		return stationRepository.findByIdForUpdate(id).orElseThrow(StationNotFoundException::new);
	}

}

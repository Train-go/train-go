package com.voyagego.traingo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.voyagego.traingo.common.exception.DuplicateStationCodeException;
import com.voyagego.traingo.common.exception.StationInUseException;
import com.voyagego.traingo.common.exception.StationNotFoundException;
import com.voyagego.traingo.entity.Station;
import com.voyagego.traingo.model.request.StationRequest;
import com.voyagego.traingo.repository.RouteRepository;
import com.voyagego.traingo.repository.StationRepository;

/**
 * Station rules from docs/product/admin.md that need the database: a code is
 * used by one station only, and a station in use cannot be deleted. The shape
 * of each field is checked earlier, on StationRequest.
 */
@Service
public class StationService {

	private final StationRepository stationRepository;
	private final RouteRepository routeRepository;

	public StationService(StationRepository stationRepository, RouteRepository routeRepository) {
		this.stationRepository = stationRepository;
		this.routeRepository = routeRepository;
	}

	@Transactional(readOnly = true)
	public List<Station> findAll() {
		return stationRepository.findAllByOrderByNameAsc();
	}

	@Transactional(readOnly = true)
	public Station findById(long id) {
		return stationRepository.findById(id).orElseThrow(() -> new StationNotFoundException(id));
	}

	/**
	 * Two requests can pass the existence check at the same moment. The unique
	 * key on the code catches the second one, and its
	 * DataIntegrityViolationException leaves this method uncaught so the
	 * transaction rolls back cleanly.
	 */
	@Transactional
	public Station create(StationRequest form) {
		String code = Station.normalizeCode(form.getCode());
		if (stationRepository.existsByCode(code)) {
			throw new DuplicateStationCodeException(code);
		}
		return stationRepository.save(new Station(code, form.getName(), form.getCity(), form.getAddress()));
	}

	@Transactional
	public Station update(long id, StationRequest form) {
		Station station = findById(id);
		String code = Station.normalizeCode(form.getCode());
		if (stationRepository.existsByCodeAndIdNot(code, id)) {
			throw new DuplicateStationCodeException(code);
		}
		station.update(code, form.getName(), form.getCity(), form.getAddress());
		// Write now instead of at commit, so a code taken by a concurrent request
		// fails here with the same DataIntegrityViolationException create() gives.
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
		Station station = findById(id);
		if (routeRepository.existsByOriginStationIdOrDestinationStationId(id, id)) {
			throw new StationInUseException(station);
		}
		stationRepository.delete(station);
		return station;
	}

}

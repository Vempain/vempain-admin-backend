package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.rest.UnitAPI;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// TODO Check that the user has permission to access this API

@Slf4j
@RequiredArgsConstructor
@RestController
public class UnitController implements UnitAPI {
	private final UnitService unitService;

	@Override
	public ResponseEntity<List<UnitResponse>> getUnits() {
		Iterable<Unit> units = unitService.findAll();

		ArrayList<UnitResponse> responses = new ArrayList<>();

		for (Unit unit : units) {
			responses.add(unit.getUnitResponse());
		}
		return ResponseEntity.ok(responses);
	}

	@Override
	public ResponseEntity<PagedResponse<UnitResponse>> getPagedUnits(PagedRequest request) {
		var responses = new ArrayList<UnitResponse>();
		unitService.findAll()
		           .forEach(unit -> responses.add(unit.getUnitResponse()));
		var query = request.getSearch();
		if (query != null && !query.isBlank()) {
			var normalized = Boolean.TRUE.equals(request.getCaseSensitive()) ? query : query.toLowerCase(Locale.ROOT);
			responses.removeIf(unit -> {
				var name = Boolean.TRUE.equals(request.getCaseSensitive()) ? unit.getName() : unit.getName()
				                                                                                  .toLowerCase(Locale.ROOT);
				return !name.contains(normalized);
			});
		}
		responses.sort(java.util.Comparator.comparing(UnitResponse::getName, String.CASE_INSENSITIVE_ORDER));
		if ("id".equals(request.getSortBy())) {
			responses.sort(java.util.Comparator.comparing(UnitResponse::getId));
		}
		if (request.getDirection() == org.springframework.data.domain.Sort.Direction.DESC) {
			java.util.Collections.reverse(responses);
		}
		int pages = (int) Math.ceil((double) responses.size() / request.getSize());
		int from = Math.min(request.getPage() * request.getSize(), responses.size());
		int to = Math.min(from + request.getSize(), responses.size());
		return ResponseEntity.ok(PagedResponse.of(responses.subList(from, to), request.getPage(), request.getSize(), responses.size(), pages,
		                                          request.getPage() == 0, pages == 0 || request.getPage() + 1 >= pages));
	}

	@Override
	public ResponseEntity<UnitResponse> findById(Long unitId) {
		if (unitId == null || unitId < 0) {
			log.error("Invalid unit ID: {}", unitId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed parameter");
		}

		UnitResponse unitResponse;

		try {
			unitResponse = unitService.findById(unitId);
		} catch (VempainEntityNotFoundException e) {
			log.error("Could not find any unit by id {}", unitId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No unit was found with given ID");
		}

		return ResponseEntity.ok(unitResponse);
	}

	@Override
	public ResponseEntity<UnitResponse> addUnit(UnitRequest unitRequest) {
		var newUnitResponse = unitService.createUnit(unitRequest);
		return ResponseEntity.ok(newUnitResponse);
	}

	@Override
	public ResponseEntity<UnitResponse> updateUser(Long unitId, UnitRequest unitRequest) {
		var updatedUnitResponse = unitService.updateUnit(unitId, unitRequest);
		return ResponseEntity.ok(updatedUnitResponse);
	}

	@ExceptionHandler(RuntimeException.class)
	public final ResponseEntity<Exception> handleRuntimeExceptions(RuntimeException e) {
		return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}

package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.request.UnitRequest;
import fi.poltsi.vempain.admin.api.response.UnitResponse;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.rest.UnitAPI;
import fi.poltsi.vempain.admin.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

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

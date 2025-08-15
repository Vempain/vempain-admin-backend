package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.FormRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.exception.EntityAlreadyExistsException;
import fi.poltsi.vempain.admin.exception.InvalidRequestException;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.rest.FormAPI;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FormController implements FormAPI {
	private final FormService   formService;
	private final DeleteService deleteService;

	@Override
	public ResponseEntity<List<FormResponse>> getForms(QueryDetailEnum requestForm) {
		try {
			return ResponseEntity.ok(formService.findAllAsResponsesForUser(requestForm));
		} catch (VempainComponentException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch any forms for user");
		}
	}

	@Override
	public ResponseEntity<List<FormResponse>> getFormsByComponentId(long formId) {
		var formResponses = formService.findFormsByComponentId(formId);
		return ResponseEntity.ok(formResponses);
	}

	@Override
	public ResponseEntity<List<FormResponse>> getFormsByLayoutId(long layoutId) {
		var formResponses = formService.findFormsByLayoutId(layoutId);
		return ResponseEntity.ok(formResponses);
	}

	@Override
	public ResponseEntity<FormResponse> getFormById(Long formId) {

		try {
			var formResponse = formService.getFormResponseById(formId);
			return ResponseEntity.ok(formResponse);
		} catch (VempainEntityNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to fetch form for user");
		} catch (VempainComponentException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch components for form");
		}
	}

	@Override
	public ResponseEntity<FormResponse> addForm(FormRequest formRequest) {
		try {
			return ResponseEntity.ok(formService.saveRequest(formRequest));
		} catch (EntityAlreadyExistsException | InvalidRequestException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed request");
		} catch (ProcessingFailedException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
		}
	}

	@Override
	public ResponseEntity<FormResponse> updateForm(FormRequest formRequest) {
		log.debug("updateForm: {}", formRequest);
		try {
			var newFormResponse = formService.updateForm(formRequest);
			return ResponseEntity.ok(newFormResponse);
		} catch (VempainEntityNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to fetch form for user");
		} catch (EntityAlreadyExistsException | InvalidRequestException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed request");
		} catch (ProcessingFailedException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
		}
	}

	@Override
	public ResponseEntity<DeleteResponse> deleteFormById(long formId) {
		if (formId < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, formId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		try {
			deleteService.deleteFormById(formId);
			return ResponseEntity.ok(DeleteResponse.builder()
												   .count(1)
												   .id(formId)
												   .name("Form")
												   .timestamp(Instant.now())
												   .httpStatus(HttpStatus.OK)
												   .build());

		} catch (VempainEntityNotFoundException e) {
			log.error("Failed to delete a form:\n{}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}
}

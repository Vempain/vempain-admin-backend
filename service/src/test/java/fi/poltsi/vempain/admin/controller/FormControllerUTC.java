package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.FormRequest;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.exception.EntityAlreadyExistsException;
import fi.poltsi.vempain.admin.exception.InvalidRequestException;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.tools.MockServiceTools;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormControllerUTC {
	static final long count = 10L;

	@Mock
	private FormService   formService;
	@Mock
	private DeleteService deleteService;

	@InjectMocks
	private FormController formController;

	@Test
	void getForms() throws VempainComponentException {
		MockServiceTools.formServiceFindAllAsResponsesForUserOk(formService, count);

		ResponseEntity<List<FormResponse>> formResponse = formController.getForms(QueryDetailEnum.FULL);
		assertNotNull(formResponse);
		List<FormResponse> formResponses = formResponse.getBody();
		assertNotNull(formResponses);
		assertEquals(count, formResponses.size());
	}

	@Test
	void getFormsFail() throws VempainComponentException {
		when(formService.findAllAsResponsesForUser(any(QueryDetailEnum.class))).thenThrow(new VempainComponentException("Failed to find component ID: 1"));

		try {
			formController.getForms(QueryDetailEnum.FULL);
			fail("We should have gotten an exception");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
			assertEquals("500 INTERNAL_SERVER_ERROR \"Failed to fetch any forms for user\"", e.getMessage());
		}
	}

	@Test
	void addForm() throws EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {
		MockServiceTools.formServiceAddFormOk(formService);
		FormRequest formRequest = FormRequest.builder()
											 .build();
		ResponseEntity<FormResponse> formResponseResponseEntity = formController.addForm(formRequest);
		assertNotNull(formResponseResponseEntity);
		FormResponse formResponse = formResponseResponseEntity.getBody();
		assertNotNull(formResponse);
	}

	@Test
	void addFormFailsWithEntityAlreadyExistsException() throws EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {
		when(formService.saveRequest(any())).thenThrow(new EntityAlreadyExistsException());
		FormRequest formRequest = FormRequest.builder()
											 .build();

		try {
			formController.addForm(formRequest);
			fail("Creating a new form should have failed with EntityAlreadyExistsException");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"Malformed request\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	@Test
	void addFormFailsWithProcessingFailedException() throws EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {
		when(formService.saveRequest(any())).thenThrow(new ProcessingFailedException("Some exception"));
		FormRequest formRequest = FormRequest.builder()
											 .build();

		try {
			formController.addForm(formRequest);
			fail("Creating a new form should have failed with ProcessingFailedException");
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
			assertEquals("500 INTERNAL_SERVER_ERROR \"Internal server error\"", e.getMessage());
		}
	}

	// ---- paged listing, lookups, update and delete ----

	private static FormResponse form(long id) {
		return TestUTCTools.generateForm(id, id)
						   .getFormResponse();
	}

	@Test
	void getPagedFormsDelegatesAndReportsAComponentFailure() throws Exception {
		var request = new PagedRequest();
		var page = PagedResponse.of(List.of(form(1L)), 0, 10, 1, 1, true, true);
		when(formService.findPagedAsResponsesForUser(request)).thenReturn(page);
		assertEquals(page, formController.getPagedForms(request)
										 .getBody());

		when(formService.findPagedAsResponsesForUser(request)).thenThrow(new VempainComponentException("gone"));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, assertThrows(ResponseStatusException.class, () -> formController.getPagedForms(request)).getStatusCode());
	}

	@Test
	void formsAreListedByComponentAndByLayout() {
		when(formService.findFormsByComponentId(5L)).thenReturn(List.of(form(1L)));
		when(formService.findFormsByLayoutId(7L)).thenReturn(List.of(form(2L), form(3L)));

		assertEquals(1, formController.getFormsByComponentId(5L)
									  .getBody()
									  .size());
		assertEquals(2, formController.getFormsByLayoutId(7L)
									  .getBody()
									  .size());
	}

	@Test
	void getFormByIdAnswersTheFormOrTranslatesTheFailure() throws Exception {
		when(formService.findByIdForUser(1L)).thenReturn(form(1L));
		assertEquals(1L, formController.getFormById(1L)
									   .getBody()
									   .getId());

		when(formService.findByIdForUser(2L)).thenThrow(new VempainEntityNotFoundException("missing", "form"));
		assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> formController.getFormById(2L)).getStatusCode());

		when(formService.findByIdForUser(3L)).thenThrow(new VempainComponentException("gone"));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, assertThrows(ResponseStatusException.class, () -> formController.getFormById(3L)).getStatusCode());
	}

	@Test
	void addFormRejectsAnInvalidRequest() throws Exception {
		when(formService.saveRequest(any())).thenThrow(new InvalidRequestException("no components"));

		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> formController.addForm(FormRequest.builder()
																																 .build())).getStatusCode());
	}

	@Test
	void updateFormAnswersTheFormOrTranslatesTheFailure() throws Exception {
		var request = FormRequest.builder()
								 .id(1L)
								 .name("Renamed")
								 .build();
		when(formService.updateForm(request)).thenReturn(form(1L));
		assertEquals(1L, formController.updateForm(request)
									   .getBody()
									   .getId());

		doThrow(new VempainEntityNotFoundException("missing", "form")).when(formService)
																	  .updateForm(request);
		assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> formController.updateForm(request)).getStatusCode());

		doThrow(new EntityAlreadyExistsException("taken", "form")).when(formService)
																  .updateForm(request);
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> formController.updateForm(request)).getStatusCode());

		doThrow(new InvalidRequestException("no components")).when(formService)
															 .updateForm(request);
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> formController.updateForm(request)).getStatusCode());

		doThrow(new ProcessingFailedException("db")).when(formService)
													.updateForm(request);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, assertThrows(ResponseStatusException.class, () -> formController.updateForm(request)).getStatusCode());
	}

	@Test
	void deleteFormByIdValidatesTheIdDeletesTheFormAndReportsAMissingOne() throws Exception {
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> formController.deleteFormById(0L)).getStatusCode());

		var deleted = formController.deleteFormById(1L);
		assertEquals(HttpStatus.OK, deleted.getStatusCode());
		assertEquals(1L, deleted.getBody()
								.getId());
		verify(deleteService).deleteFormById(1L);

		doThrow(new VempainEntityNotFoundException("missing", "form")).when(deleteService)
																	  .deleteFormById(2L);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, assertThrows(ResponseStatusException.class, () -> formController.deleteFormById(2L)).getStatusCode());
	}
}

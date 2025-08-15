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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormControllerUTC {
    static final long count = 10L;

    @Mock
    private FormService    formService;
	@Mock
	private DeleteService  deleteService;

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
        FormRequest formRequest = FormRequest.builder().build();
        ResponseEntity<FormResponse> formResponseResponseEntity = formController.addForm(formRequest);
        assertNotNull(formResponseResponseEntity);
        FormResponse formResponse = formResponseResponseEntity.getBody();
        assertNotNull(formResponse);
    }

    @Test
    void addFormFailsWithEntityAlreadyExistsException() throws EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {
        when(formService.saveRequest(any())).thenThrow(new EntityAlreadyExistsException());
        FormRequest formRequest = FormRequest.builder().build();

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
        FormRequest formRequest = FormRequest.builder().build();

        try {
            formController.addForm(formRequest);
            fail("Creating a new form should have failed with ProcessingFailedException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertEquals("500 INTERNAL_SERVER_ERROR \"Internal server error\"", e.getMessage());
        }
    }
}

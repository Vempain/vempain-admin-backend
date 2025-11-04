package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.UnitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitControllerUTC {
    @Mock
	UnitService unitService;

	@InjectMocks
    private UnitController unitController;

    @Test
    void getUnitsOk() {
        when(unitService.findAll()).thenReturn(TestUTCTools.generateUnitList(5L));

        try {
            ResponseEntity<List<UnitResponse>> responseEntity = unitController.getUnits();
            assertNotNull(responseEntity);
            List<UnitResponse> unitResponses = responseEntity.getBody();
            assertNotNull(unitResponses);
            assertEquals(5L, unitResponses.size());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }

    @Test
    void getUnitsNoneFoundOk() {
        when(unitService.findAll()).thenReturn(new ArrayList<>());

        try {
            ResponseEntity<List<UnitResponse>> responseEntity = unitController.getUnits();
            assertNotNull(responseEntity);
            List<UnitResponse> unitResponses = responseEntity.getBody();
            assertNotNull(unitResponses);
            assertEquals(0, unitResponses.size());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }

    @Test
    void findByIdOk() throws VempainEntityNotFoundException {
		var unit = TestUTCTools.generateUnit(1L);
		var unitResponse = unit.getUnitResponse();
        when(unitService.findById(1L)).thenReturn(unitResponse);

        try {
            ResponseEntity<UnitResponse> responseEntity = unitController.findById(1L);
            assertNotNull(responseEntity);
            UnitResponse unitResponseFromBody = responseEntity.getBody();
            assertNotNull(unitResponseFromBody);
            assertEquals(1L, unitResponseFromBody.getId());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }

    @Test
    void findByIdNegativeIdFail() {
        findByIdFail(-1L);
    }

    @Test
    void findByIdNullIdFail() {
        findByIdFail(null);
    }

    private void findByIdFail(Long unitId) {
        try {
            unitController.findById(unitId);
            fail("Should have received a ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals("400 BAD_REQUEST \"Malformed parameter\"", e.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void findByIdNoneFoundFail() throws VempainEntityNotFoundException {
        when(unitService.findById(1L)).thenThrow(new VempainEntityNotFoundException("Test message", "Unit"));

        try {
            unitController.findById(1L);
            fail("Should have received a ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals("404 NOT_FOUND \"No unit was found with given ID\"", e.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void handleRuntimeExceptionsOk() {
        try {
            ResponseEntity<Exception> responseEntity = unitController.handleRuntimeExceptions(new NullPointerException("Test exception"));
            assertNotNull(responseEntity);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
            Exception e = responseEntity.getBody();
            assertNotNull(e);
            assertEquals("Test exception", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }
}

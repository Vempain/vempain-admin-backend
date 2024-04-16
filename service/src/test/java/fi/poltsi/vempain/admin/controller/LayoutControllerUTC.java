package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
class LayoutControllerUTC {
	@Mock
	private LayoutService layoutService;
	@Mock
	private DeleteService deleteService;

	private LayoutController layoutController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		layoutController = new LayoutController(layoutService, deleteService);
	}

	@Test
	void getLayoutsOk() {
		List<LayoutResponse> responses = TestUTCTools.generateLayoutResponseList(5L);
		when(layoutService.findAllByUser()).thenReturn(responses);

		try {
			ResponseEntity<List<LayoutResponse>> responseEntity = layoutController.getLayouts();
			assertNotNull(responseEntity);
			List<LayoutResponse> layoutResponses = responseEntity.getBody();
			assertNotNull(layoutResponses);
			assertEquals(5L, layoutResponses.size());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void getLayoutsEmptyListOk() {
		when(layoutService.findAllByUser()).thenReturn(new ArrayList<>());

		try {
			ResponseEntity<List<LayoutResponse>> responseEntity = layoutController.getLayouts();
			assertNotNull(responseEntity);
			List<LayoutResponse> layoutResponses = responseEntity.getBody();
			assertNotNull(layoutResponses);
			assertEquals(0, layoutResponses.size());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void getLayoutByIdOk() throws VempainEntityNotFoundException {
		Layout layout = TestUTCTools.generateLayout(1L);
		when(layoutService.findLayoutResponseByIdByUser(1L)).thenReturn(layout.getLayoutResponse());

		try {
			ResponseEntity<LayoutResponse> responseEntity = layoutController.getLayoutById(1L);
			assertNotNull(responseEntity);
			LayoutResponse layoutResponse = responseEntity.getBody();
			assertNotNull(layoutResponse);
			assertEquals(1L, layoutResponse.getId());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void getLayoutByIdInvalidIdFail() {
		Long[] invalidIds = {null, -1L};

		for (Long invalidId : invalidIds) {
			try {
				layoutController.getLayoutById(invalidId);
				fail("Should not have found a layout with negative ID");
			} catch (ResponseStatusException e) {
				assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
				assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
			} catch (Exception e) {
				fail("Should not have received any other exceptions: " + e);
			}
		}
	}

	@Test
	void getLayoutByIdLayoutExceptionFail() throws VempainEntityNotFoundException {
		doThrow(new VempainEntityNotFoundException("Test exception", "layout")).when(layoutService).findLayoutResponseByIdByUser(1L);

		try {
			layoutController.getLayoutById(1L);
			fail("Should not have found a layout when throwing an exception");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions: " + e);
		}
	}

	@Test
	void getLayoutByNameOk() throws VempainLayoutException {
		Layout layout = TestUTCTools.generateLayout(1L);
		layout.setLayoutName("test");
		when(layoutService.findLayoutResponseByLayoutNameByUser("test")).thenReturn(layout.getLayoutResponse());

		try {
			ResponseEntity<LayoutResponse> responseEntity = layoutController.getLayoutByName("test");
			assertNotNull(responseEntity);
			LayoutResponse layoutResponse = responseEntity.getBody();
			assertNotNull(layoutResponse);
			assertEquals(1L, layoutResponse.getId());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void getLayoutByNameNullNameFail() {
		String[] errorNames = {null, "", " ", "\t", " \n"};

		for (String errorName : errorNames) {
			try {
				layoutController.getLayoutByName(errorName);
				fail("Should not have found a layout with null name");
			} catch (ResponseStatusException e) {
				assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_OBJECT_NAME_IN_REQUEST + "\"", e.getMessage());
				assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
			} catch (Exception e) {
				fail("Should not have received any other exceptions: " + e);
			}
		}
	}

	@Test
	void getLayoutByNameLayoutExceptionFail() throws VempainLayoutException {
		doThrow(new VempainLayoutException(VempainMessages.NO_LAYOUT_FOUND_BY_ID)).when(layoutService).findLayoutResponseByLayoutNameByUser("test");

		try {
			layoutController.getLayoutByName("test");
			fail("Should not have found a layout when throwing an exception");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions: " + e);
		}
	}

	@Test
	void saveLayoutOk() {
		Layout        layout        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);
		when(layoutService.saveLayoutRequestByUser(layoutRequest)).thenReturn(layout);
		when(layoutService.createLayoutResponse(layout)).thenReturn(layout.getLayoutResponse());

		try {
			ResponseEntity<LayoutResponse> responseEntity = layoutController.saveLayout(layoutRequest);
			assertNotNull(responseEntity);
			LayoutResponse layoutResponse = responseEntity.getBody();
			assertNotNull(layoutResponse);
			assertEquals(layout.getId(), layoutResponse.getId());
			assertEquals(layout.getLayoutName(), layoutResponse.getLayoutName());
			assertEquals(layout.getStructure(), layoutResponse.getStructure());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void saveLayoutValidationFail() {
		List<LayoutRequest> requests = new ArrayList<>();

		requests.add(null);

		String[] emptyNames = {null, "", " ", "\t", " \n"};

		for (String emptyName : emptyNames) {
			Layout        layout        = TestUTCTools.generateLayout(1L);
			LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);
			layoutRequest.setLayoutName(emptyName);
			requests.add(layoutRequest);
		}

		Layout        layout1        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest1 = TestUTCTools.generateLayoutRequest(layout1);
		layoutRequest1.setAcls(null);
		requests.add(layoutRequest1);

		Layout        layout2        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest2 = TestUTCTools.generateLayoutRequest(layout2);
		layoutRequest2.setAcls(new ArrayList<>());
		requests.add(layoutRequest2);

		for (LayoutRequest layoutRequest : requests) {
			try {
				layoutController.saveLayout(layoutRequest);
				fail("Should not have been able to complete saving a layout with invalid request");
			} catch (ResponseStatusException e) {
				assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_OBJECT_IN_REQUEST + "\"", e.getMessage());
				assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
			} catch (Exception e) {
				fail("Should not have received any other exceptions: " + e);
			}
		}
	}

	@Test
	void updateLayoutOk() {
		Layout        layout        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);
		when(layoutService.updateByUser(layoutRequest)).thenReturn(layout);
		when(layoutService.createLayoutResponse(layout)).thenReturn(layout.getLayoutResponse());

		try {
			ResponseEntity<LayoutResponse> responseEntity = layoutController.updateLayout(layoutRequest);
			assertNotNull(responseEntity);
			LayoutResponse layoutResponse = responseEntity.getBody();
			assertNotNull(layoutResponse);
			assertEquals(layout.getId(), layoutResponse.getId());
			assertEquals(layout.getLayoutName(), layoutResponse.getLayoutName());
			assertEquals(layout.getStructure(), layoutResponse.getStructure());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void updateLayoutInvalidIdFail() {
		Layout        layout        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);
		layoutRequest.setId(-1);

		try {
			layoutController.updateLayout(layoutRequest);
			fail("Should not have found a layout with invalid ID");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions: " + e);
		}
	}

	@Test
	void updateLayoutNullRequestFail() {
		try {
			layoutController.updateLayout(null);
			fail("Should not have been able to update layout with null object");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions: " + e);
		}
	}

	@Test
	void updateLayoutValidationFail() {
		List<LayoutRequest> requests = new ArrayList<>();

		String[] emptyNames = {null, "", " ", "\t", " \n"};

		for (String emptyName : emptyNames) {
			Layout        layout        = TestUTCTools.generateLayout(1L);
			LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);
			layoutRequest.setLayoutName(emptyName);
			requests.add(layoutRequest);
		}

		Layout        layout1        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest1 = TestUTCTools.generateLayoutRequest(layout1);
		layoutRequest1.setAcls(null);
		requests.add(layoutRequest1);

		Layout        layout2        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest2 = TestUTCTools.generateLayoutRequest(layout2);
		layoutRequest2.setAcls(new ArrayList<>());
		requests.add(layoutRequest2);

		for (LayoutRequest layoutRequest : requests) {
			try {
				layoutController.updateLayout(layoutRequest);
				fail("Should not have been able to update layout with malformed request object");
			} catch (ResponseStatusException e) {
				assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_OBJECT_IN_REQUEST + "\"", e.getMessage());
				assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
			} catch (Exception e) {
				fail("Should not have received any other exceptions: " + e);
			}
		}
	}

	@Test
	void removeLayoutByIdOk() {
		Layout        layout        = TestUTCTools.generateLayout(1L);
		LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);
		doNothing().when(layoutService).deleteByUser(layoutRequest.getId());

		try {
			ResponseEntity<DeleteResponse> responseEntity = layoutController.removeLayoutById(layoutRequest.getId());
			assertNotNull(responseEntity);
			DeleteResponse deleteResponse = responseEntity.getBody();
			assertNotNull(deleteResponse);
			assertEquals(1L, deleteResponse.getCount());
			assertEquals(1L, deleteResponse.getId());
			assertEquals("layout", deleteResponse.getName());
		} catch (Exception e) {
			fail("Should not have received any exceptions: " + e);
		}
	}

	@Test
	void deleteLayoutInvalidIdFail() {
		Long[] invalidIds = {null, -1L};

		for (Long invalidId : invalidIds) {
			try {
				layoutController.removeLayoutById(invalidId);
				fail("Should not have found a layout with invalid ID");
			} catch (ResponseStatusException e) {
				assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
				assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
			} catch (Exception e) {
				fail("Should not have received any other exceptions: " + e);
			}
		}
	}

	@Test
	void deleteLayoutNoLayoutFail() throws VempainEntityNotFoundException {
		ArrayList<Exception> exceptions = new ArrayList<>();
		exceptions.add(new VempainEntityNotFoundException());

		for (Exception e : exceptions) {
			doThrow(e).when(deleteService).deleteLayoutById(1L);

			try {
				layoutController.removeLayoutById(1L);
				fail("Should not succeed to delete a non-existing layout");
			} catch (ResponseStatusException ex) {
				assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", ex.getMessage());
				assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
			} catch (Exception ex) {
				fail("Should not have received any other exceptions: " + ex);
			}
		}
	}
}

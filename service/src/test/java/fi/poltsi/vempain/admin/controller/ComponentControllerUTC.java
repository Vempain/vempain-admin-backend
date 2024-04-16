package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class ComponentControllerUTC {
	final String[] invalidFields = {null, "", " ", "\t", " \t"};

	@Mock
	private ComponentService componentService;
	@Mock
	private AclService       aclService;
	@Mock
	private DeleteService	deleteService;

	private ComponentController componentController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		componentController = new ComponentController(componentService, aclService, deleteService);
	}

	@Test
	void getComponentsOk() {
		List<Component> components = TestUTCTools.generateComponentList(10L);
		when(componentService.findAllByUser()).thenReturn(components);

		try {
			ResponseEntity<List<ComponentResponse>> response = componentController.getComponents();
			assertNotNull(response);
			List<ComponentResponse> componentResponses = response.getBody();
			assertNotNull(componentResponses);
			assertEquals(10L, componentResponses.size());
		} catch (Exception e) {
			fail("Should not have received any exceptions in a successful call to findAll");
		}
	}

	@Test
	void getComponentsEmptyListFail() {
		when(componentService.findAllByUser()).thenReturn(new ArrayList<>());

		try {
			ResponseEntity<List<ComponentResponse>> response = componentController.getComponents();
			assertNotNull(response);
			List<ComponentResponse> componentResponses = response.getBody();
			assertNotNull(componentResponses);
			assertEquals(0L, componentResponses.size());
		} catch (Exception e) {
			fail("Should not have received any exceptions in a successful call to findAll even with nothing found");
		}
	}

	@Test
	void getComponentByIdOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		when(componentService.findByIdByUser(1L)).thenReturn(component);

		try {
			ResponseEntity<ComponentResponse> response = componentController.getComponentById(1L);
			assertNotNull(response);
			ComponentResponse componentResponse = response.getBody();
			assertNotNull(componentResponse);
		} catch (Exception e) {
			fail("Should not have received any exceptions in a successful call to getComponentById even with nothing found");
		}
	}

	@Test
	void getComponentByIdNullIdFail() {
		getComponentFail(null);
	}

	@Test
	void getComponentByIdNegativeIdFail() {
		getComponentFail(-1L);
	}

	private void getComponentFail(Long componentId) {
		try {
			componentController.getComponentById(componentId);
			fail("Invalid component ID should have caused an exception");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("No other exception should have been thrown: " + e);
		}
	}

	@Test
	void addComponentOk() throws VempainComponentException {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(new VempainComponentException("Failed to find component by name")).when(componentService).findByName(componentRequest.getCompName());
		when(componentService.saveFromRequest(componentRequest)).thenReturn(component);
		List<Acl> acls = TestUTCTools.generateAclList(1L, 1L);
		when(aclService.findAclByAclId(component.getAclId())).thenReturn(acls);
		List<AclResponse> aclResponses = TestUTCTools.generateAclResponses(1L, 2L);
		when(aclService.getAclResponses(component.getAclId())).thenReturn(aclResponses);

		try {
			ResponseEntity<ComponentResponse> response = componentController.addComponent(componentRequest);
			assertNotNull(response);
			ComponentResponse componentResponse = response.getBody();
			assertNotNull(componentResponse);
			List<AclResponse> responseAcls = componentResponse.getAcls();
			assertNotNull(responseAcls);
			assertFalse(responseAcls.isEmpty());
		} catch (Exception e) {
			fail("Should not have received any exceptions in a successful call to addComponent: " + e);
		}
	}

	@Test
	void addComponentMalformedRequestNullFail() {
		malformedComponentRequest(null);
	}

	@Test
	void addComponentMalformedRequestNameFail() {
		for (String invalidName : invalidFields) {
			ComponentRequest componentRequest = TestUTCTools.generateComponentRequest(1L, 1L);
			componentRequest.setCompName(invalidName);
			malformedComponentRequest(componentRequest);
		}
	}

	@Test
	void addComponentMalformedRequestDataFail() {
		for (String invalidData : invalidFields) {
			ComponentRequest componentRequest = TestUTCTools.generateComponentRequest(1L, 1L);
			componentRequest.setCompData(invalidData);
			malformedComponentRequest(componentRequest);
		}
	}

	@Test
	void addComponentMalformedRequestAclsNullFail() {
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequest(1L, 1L);
		componentRequest.setAcls(null);
		malformedComponentRequest(componentRequest);
	}

	@Test
	void addComponentMalformedRequestAclsEmptyFail() {
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequest(1L, 1L);
		componentRequest.setAcls(new ArrayList<>());
		malformedComponentRequest(componentRequest);
	}

	void malformedComponentRequest(ComponentRequest componentRequest) {
		try {
			componentController.addComponent(componentRequest);
			fail("Should have received a ResponseStatusException when saving a malformed component");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_OBJECT_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}

	@Test
	void addComponentNameAlreadyExistsFail() {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Test exception")).when(componentService).saveFromRequest(componentRequest);

		try {
			componentController.addComponent(componentRequest);
			fail("Should have received a ResponseStatusException when saving a component with existing name");
		} catch (ResponseStatusException e) {
			assertEquals("409 CONFLICT \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}

	@Test
	void addComponentResponseStatusExceptionFail() {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR)).when(componentService).saveFromRequest(componentRequest);

		try {
			componentController.addComponent(componentRequest);
			fail("Should have received a ResponseStatusException when saving a component causing VempainComponentException in service");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}

	@Test
	void updateComponentOk() throws VempainAclException, VempainEntityNotFoundException, VempainComponentException, VempainAbstractException {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		when(componentService.updateFromRequest(componentRequest)).thenReturn(component);

		try {
			ResponseEntity<ComponentResponse> response = componentController.updateComponent(componentRequest);
			assertNotNull(response);
			ComponentResponse componentResponse = response.getBody();
			assertNotNull(componentResponse);
		} catch (Exception e) {
			fail("Should not have received any exceptions when updating successfully a component: " + e);
		}
	}

	@Test
	void updateMalformedIdNegativeFail() {
		updateComponentFailInvalidId(-1L);
	}

	private void updateComponentFailInvalidId(Long componentId) {
		var        component        = TestUTCTools.generateComponent(1L, 1L);
		var componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		componentRequest.setId(componentId);

		try {
			componentController.updateComponent(componentRequest);
			fail("Should have received an exception when using malformed number as component ID");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions when failing to update a component: " + e);
		}
	}

	@Test
	void updateAccessDeniedFail() throws VempainAclException, VempainEntityNotFoundException, VempainComponentException, VempainAbstractException {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.INVALID_USER_SESSION)).when(componentService).updateFromRequest(componentRequest);

		try {
			componentController.updateComponent(componentRequest);
			fail("Should have received an exception when updating an non-existing component");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"" + VempainMessages.INVALID_USER_SESSION + "\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}

	@Test
	void updateComponentVempainComponentExceptionFail() throws VempainAclException, VempainEntityNotFoundException, VempainComponentException,
															   VempainAbstractException {
		updateComponentUpdateFails(new VempainComponentException("test exception"));
	}

	@Test
	void updateComponentVempainAclExceptionFail() throws VempainAclException, VempainEntityNotFoundException, VempainComponentException,
														 VempainAbstractException {
		updateComponentUpdateFails(new VempainAclException("test exception"));
	}

	@Test
	void updateComponentVempainAbstractExceptionFail() throws VempainAclException, VempainEntityNotFoundException, VempainComponentException,
															  VempainAbstractException {
		updateComponentUpdateFails(new VempainAbstractException("test exception"));
	}

	void updateComponentUpdateFails(Exception ex) throws VempainAclException, VempainEntityNotFoundException, VempainComponentException, VempainAbstractException {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(ex).when(componentService).updateFromRequest(componentRequest);

		try {
			componentController.updateComponent(componentRequest);
			fail("Should have received an exception when updating an non-existing component");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"Component data failed validation\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}

	@Test
	void updateComponentNotFoundFail() throws VempainAclException, VempainEntityNotFoundException, VempainComponentException,
											  VempainAbstractException {
		Component        component        = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(new VempainEntityNotFoundException("test exception", "component"))
				.when(componentService).updateFromRequest(componentRequest);

		try {
			componentController.updateComponent(componentRequest);
			fail("Should have received an exception when updating an non-existing component");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}

	@Test
	void removeComponentByIdOk() throws VempainAclException {
		doNothing().when(componentService).deleteByUser(1L);

		try {
			ResponseEntity<DeleteResponse> response = componentController.deleteComponentById(1L);
			assertNotNull(response);
			DeleteResponse deleteResponse = response.getBody();
			assertNotNull(deleteResponse);
			assertEquals(1L, deleteResponse.getCount());
			assertEquals(HttpStatus.OK, deleteResponse.getHttpStatus());
			assertEquals("Component", deleteResponse.getName());
		} catch (Exception e) {
			fail("Should not have received any exceptions when deleting successfully a component: " + e);
		}
	}

	@Test
	void removeComponentMalformedIdNegativeFail() {
		removeComponentFailInvalidId(-1L);
	}

	private void removeComponentFailInvalidId(Long componentId) {
		try {
			componentController.deleteComponentById(componentId);
			fail("Should have received an exception when using malformed value as component ID");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_ID_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any exceptions when updating successfully a component: " + e);
		}
	}

	@Test
	void removeComponentByIdVempainAclExceptionFail() throws VempainEntityNotFoundException {
		doThrow(new VempainEntityNotFoundException("Test fail", "Component")).when(deleteService).deleteComponentById(1L);

		try {
			componentController.deleteComponentById(1L);
			fail("Should have received VempainEntityNotFoundException");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exceptions except ResponseStatusException: " + e);
		}
	}
}

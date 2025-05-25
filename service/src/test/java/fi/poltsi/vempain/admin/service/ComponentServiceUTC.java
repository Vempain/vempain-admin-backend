package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentServiceUTC {
	private final static int                 itemCount = 10;
	@Mock
	private              ComponentRepository componentRepository;
	@Mock
	private              AclService          aclService;
	@Mock
	private              AccessService       accessService;

	@InjectMocks
	private ComponentService componentService;

	@Test
	void findAllOk() {
		List<Component> inputComponents = new ArrayList<>();

		for (int i = 0; i < itemCount; i++) {
			Component component = Component.builder()
										   .id(1L)
										   .compName("Test component " + i)
										   .compData("<!-- component data " + i + " -->")
										   .locked(false)
										   .aclId(1L)
										   .creator(1L)
										   .created(Instant.now()
														   .minus(1, ChronoUnit.HOURS))
										   .modifier(1L)
										   .modified(Instant.now())
										   .build();
			inputComponents.add(component);
		}
		when(componentRepository.findAll())
				.thenReturn(inputComponents);

		List<Component> components = componentService.findAll();
		assertNotNull(components);
		assertEquals(itemCount, components.size());

		for (Component component : components) {
			assertComponent(component);
		}
	}

	@Test
	void findAllEmptyListOk() {
		when(componentRepository.findAll()).thenReturn(new ArrayList<>());

		List<Component> components = componentService.findAll();
		assertNotNull(components);
		assertEquals(0, components.size());
	}

	@Test
	void findAllByUserOk() {
		List<Component> inputComponents = new ArrayList<>();

		for (int i = 0; i < itemCount; i++) {
			Component component = Component.builder()
										   .id(1L)
										   .compName("Test component " + i)
										   .compData("<!-- component data " + i + " -->")
										   .locked(false)
										   .aclId(1L)
										   .creator(1L)
										   .created(Instant.now()
														   .minus(1, ChronoUnit.HOURS))
										   .modifier(1L)
										   .modified(Instant.now())
										   .build();
			inputComponents.add(component);
		}
		when(componentRepository.findAll())
				.thenReturn(inputComponents);
		when(accessService.hasReadPermission(anyLong()))
				.thenReturn(true);

		List<Component> components = componentService.findAllByUser();

		assertNotNull(components);
		assertEquals(itemCount, components.size());
	}

	@Test
	void findAllByUserNoAccessOk() {
		List<Component> components = componentService.findAllByUser();
		assertNotNull(components);
		assertEquals(0, components.size());
	}

	@Test
	void findByIdOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(anyLong())).thenReturn(optionalComponent);

		try {
			Component comp = componentService.findById(1L);
			assertNotNull(comp);
		} catch (Exception e) {
			fail("Should not have received any exception when requesting for an existing component: " + e.getMessage());
		}
	}

	@Test
	void findByIdNoneFoundFail() {
		when(componentRepository.findById(anyLong())).thenReturn(Optional.empty());

		try {
			componentService.findById(1L);
			fail("Should have received a VempainComponentException exception when none is found");
		} catch (VempainComponentException e) {
			assertEquals("Failed to find component", e.getMessage());
		} catch (Exception e) {
			fail("Should not have received any other exception when requesting for a non-existing component: " + e.getMessage());
		}
	}

	@Test
	void findByIdByUserOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(anyLong())).thenReturn(optionalComponent);
		when(accessService.hasReadPermission(anyLong())).thenReturn(true);
		when(accessService.getValidUserId()).thenReturn(1L);

		try {
			Component comp = componentService.findByIdByUser(1L);
			assertNotNull(comp);
		} catch (Exception e) {
			fail("We should not have received any exception when searching for granted components: " + e.getMessage());
		}
	}

	@Test
	void findByIdByUserNoneFoundFail() {
		when(componentRepository.findById(anyLong())).thenReturn(Optional.empty());

		try {
			componentService.findByIdByUser(1L);
			fail("Fetching an non-existing component should have triggered a ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"Object not found\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have received any other exception when searching for a non-existent component: " + e);
		}
	}

	@Test
	void findByIdByUserNoSessionFail() {
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			componentService.findByIdByUser(1L);
			fail("We should have received a ResponseStatusException when we don't have a valid session");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have received any other exception when searching for a component with no valid session: " + e);
		}
	}

	@Test
	void findByIdByUserNoAccessFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(anyLong())).thenReturn(optionalComponent);
		when(accessService.hasReadPermission(anyLong())).thenReturn(false);
		when(accessService.getValidUserId()).thenReturn(1L);

		try {
			componentService.findByIdByUser(1L);
			fail("We should have received a ResponseStatusException when we don't have read permission");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have received any other exception when searching for a component with no read permission: " + e.getMessage());
		}
	}

	@Test
	void findByNameOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		String componentName = component.getCompName();
		when(componentRepository.findByCompName(componentName)).thenReturn(component);

		try {
			componentService.findByName(componentName);
		} catch (VempainComponentException e) {
			fail("Should not have received any exception when fetching component by name: " + e.getMessage());
		}
	}

	@Test
	void findByNameNotFoundFail() {
		when(componentRepository.findByCompName(anyString())).thenReturn(null);

		try {
			componentService.findByName("componentName");
			fail("Should have received VempainComponentException when fetching component by name that does not exist");
		} catch (VempainComponentException e) {
			assertEquals("Failed to find component by name", e.getMessage());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e.getMessage());
		}
	}

	@Test
	void deleteByIdOk() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(anyLong())).thenReturn(optionalComponent);
		doNothing().when(aclService)
				   .deleteByAclId(1L);
		doNothing().when(componentRepository)
				   .deleteById(1L);

		try {
			componentService.deleteById(1L);
		} catch (Exception e) {
			fail("Should not have received any exception when deleting existing component: " + e.getMessage());
		}
	}

	@Test
	void deleteByIdNoComponentFoundFail() {
		when(componentRepository.findById(anyLong())).thenReturn(Optional.empty());

		try {
			componentService.deleteById(1L);
			fail("Deleting a non-existing component by ID should have failed");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Component not found for deletion", e.getMessage());
			assertEquals("component", e.getEntityName());
		} catch (Exception e) {
			fail("Should have received only EntityNotFoundException when deleting non-existing component by ID: " + e);
		}
	}

	@Test
	void deleteByIdAclDeleteFail() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(anyLong())).thenReturn(optionalComponent);
		doThrow(new VempainEntityNotFoundException("ACL not found for deletion", "test acl")).when(aclService)
																							 .deleteByAclId(1L);

		try {
			componentService.deleteById(1L);
		} catch (VempainEntityNotFoundException e) {
			assertEquals("ACL not found for deletion", e.getMessage());
			assertEquals("test acl", e.getEntityName());
		} catch (Exception e) {
			fail("Should not have received any other exception when deleting existing component with no ACL: " + e.getMessage());
		}
	}

	@Test
	void saveOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		when(componentRepository.save(component)).thenReturn(component);
		try {
			Component comp = componentService.save(component);
			assertNotNull(comp);
		} catch (Exception e) {
			fail("Should not have received any exception when saving a component: " + e.getMessage());
		}
	}

	@Test
	void saveNullModifierAndModifierOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setModifier(null);
		component.setModified(null);
		when(componentRepository.save(component)).thenReturn(component);

		try {
			Component comp = componentService.save(component);
			assertNotNull(comp);
		} catch (Exception e) {
			fail("Should not have received any exception when saving a component with null modifier and modified: " + e.getMessage());
		}
	}

	@Test
	void saveNullEmptyBlankCompNameFail() {
		String[] compNames = {null, "", " ", "\t"};

		for (String name : compNames) {
			Component component = TestUTCTools.generateComponent(1L, 1L);
			component.setCompName(name);

			try {
				componentService.save(component);
				fail("Saving component with '" + name + "' component name should have failed");
			} catch (VempainComponentException e) {
				assertEquals("Component name is not set", e.getMessage());
			} catch (Exception e) {
				fail("Should have received VempainComponentException when saving a component with '" + name + "' component name: " + e.getMessage());
			}
		}
	}

	@Test
	void saveNullEmptyBlankCompDataFail() {
		String[] compDatas = {null, "", " ", "\t"};

		for (String data : compDatas) {
			Component component = TestUTCTools.generateComponent(1L, 1L);
			component.setCompData(data);

			try {
				componentService.save(component);
				fail("Saving component with '" + data + "' component data should have failed");
			} catch (VempainComponentException e) {
				assertEquals("Component data is not set", e.getMessage());
			} catch (Exception e) {
				fail("Should have received VempainComponentException when saving a component with '" + data + "' component data: " + e.getMessage());
			}
		}
	}

	@Test
	void saveInvalidAclIdFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setAclId(0L);
		doThrow(new VempainAbstractException("ACL ID is invalid")).when(aclService)
																  .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with invalid ACL ID should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("ACL ID is invalid", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with invalid ACL ID: " + e.getMessage());
		}
	}

	@Test
	void saveNullCreatorFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setCreator(null);
		doThrow(new VempainAbstractException("Creator is missing or invalid")).when(aclService)
																			  .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with null creator should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Creator is missing or invalid", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with null creator: " + e.getMessage());
		}
	}

	@Test
	void saveInvalidCreatorFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setCreator(0L);
		doThrow(new VempainAbstractException("Creator is missing or invalid")).when(aclService)
																			  .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with invalid creator should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Creator is missing or invalid", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with invalid creator: " + e.getMessage());
		}
	}

	@Test
	void saveNullCreatedDateFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setCreated(null);
		doThrow(new VempainAbstractException("Created datetime is missing")).when(aclService)
																			.validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with null created date should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Created datetime is missing", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with null created date: " + e.getMessage());
		}
	}

	@Test
	void saveModifiedSetModifierNullFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setModifier(null);
		doThrow(new VempainAbstractException("Modifier is missing while modified is set")).when(aclService)
																						  .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with null modifier while modified date is set should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Modifier is missing while modified is set", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with null modifier: " + e.getMessage());
		}
	}

	@Test
	void saveModifierSetModifiedNullFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setModified(null);
		doThrow(new VempainAbstractException("Modified datetime is missing while modifier is set")).when(aclService)
																								   .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with null modified date while modifier is set should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Modified datetime is missing while modifier is set", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with null modified date: " + e.getMessage());
		}
	}

	@Test
	void saveModifierSetWithInvalidValueFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setModifier(-1L);
		doThrow(new VempainAbstractException("Entity modifier is invalid")).when(aclService)
																		   .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with invalid modifier should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Entity modifier is invalid", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainComponentException when saving a component with invalid modifier: " + e.getMessage());
		}
	}

	@Test
	void saveModifiedSetBeforeCreatedFail() throws VempainAbstractException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		component.setModified(Instant.now()
									 .minus(1, ChronoUnit.HOURS));
		component.setCreated(Instant.now());
		doThrow(new VempainAbstractException("Created datetime is more recent than modified")).when(aclService)
																							  .validateAbstractData(component);

		try {
			componentService.save(component);
			fail("Saving component with modified date before created date should have failed");
		} catch (VempainAbstractException e) {
			assertEquals("Created datetime is more recent than modified", e.getMessage());
		} catch (Exception e) {
			fail("Should have received VempainAbstractException when saving a component with modified before created date: " + e.getMessage());
		}
	}

	@Test
	void saveFromRequestOk() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		when(componentRepository.findByCompName(componentRequest.getCompName())).thenReturn(null);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(componentRepository.save(any(Component.class))).thenReturn(component);

		try {
			Component comp = componentService.saveFromRequest(componentRequest);
			assertNotNull(comp);
		} catch (Exception e) {
			fail("No exception should have been received when saving a correctly formed component request: " + e.getMessage());
		}
	}

	@Test
	void saveFromRequestComponentExistsFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		when(componentRepository.findByCompName(componentRequest.getCompName())).thenReturn(component);

		try {
			componentService.saveFromRequest(componentRequest);
			fail("Saving a component request which already exists should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("409 CONFLICT \"" + VempainMessages.OBJECT_NAME_ALREADY_EXISTS + "\"", e.getMessage());
			assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
		} catch (Exception e) {
			fail("No other exception should have been received when saving a component request with existing name: " + e.getMessage());
		}
	}

	@Test
	void saveFromRequestNoAuthorizationFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			componentService.saveFromRequest(componentRequest);
			fail("Saving a component request without permission should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("No other exception should have been received when trying to save a component request without authorization: " + e.getMessage());
		}
	}

	@Test
	void saveFromRequestInvalidAclIdGeneratedFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		when(componentRepository.findByCompName(componentRequest.getCompName())).thenReturn(null);
		when(accessService.getValidUserId()).thenReturn(1L);
		doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid ACL ID")).when(aclService)
																								.saveNewAclForObject(any());

		try {
			componentService.saveFromRequest(componentRequest);
			fail("Saving a component request with malformed ACL ID should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"Invalid ACL ID\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("No other exception should have been received when trying to save a component request with malformed ACL ID: " + e.getMessage());
		}
	}

	@Test
	void saveFromRequestNoAclListFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		componentRequest.setAcls(null);
		when(componentRepository.findByCompName(componentRequest.getCompName())).thenReturn(null);
		when(accessService.getValidUserId()).thenReturn(1L);
		doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request contains no ACL list")).when(aclService)
																									.saveNewAclForObject(componentRequest.getAcls());

		try {
			componentService.saveFromRequest(componentRequest);
			fail("Saving a component request without ACL list should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"Request contains no ACL list\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("No other exception should have been received when trying to save a component request without an ACL list: " + e.getMessage());
		}
	}

	@Test
	void saveFromRequestUnknownVempainAclExceptionFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		when(componentRepository.findByCompName(componentRequest.getCompName())).thenReturn(null);
		when(accessService.getValidUserId()).thenReturn(1L);
		doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error")).when(aclService)
																							   .saveNewAclForObject(componentRequest.getAcls());

		try {
			componentService.saveFromRequest(componentRequest);
			fail("Saving a component request and getting a ResponseStatusException should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"Unknown error\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("No other exception should have been received when trying to save a component request with unknown VempainAclException: " + e.getMessage());
		}
	}

	@Test
	void updateFromRequestOk() throws VempainAclException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(accessService.hasModifyPermission(component.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .updateFromRequestList(componentRequest.getAcls());
		when(componentRepository.save(any(Component.class))).thenReturn(component);

		try {
			Component returnValue = componentService.updateFromRequest(componentRequest);
			assertNotNull(returnValue);
			assertEquals(component, returnValue);
		} catch (Exception e) {
			fail("Updating component by well formed request should have succeeded");
		}
	}

	@Test
	void updateFromRequestNoFormFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);

		when(componentRepository.findById(component.getId())).thenReturn(Optional.empty());

		try {
			componentService.updateFromRequest(componentRequest);
			fail("Updating a non-existing component should have failed");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Component can not be updated because it does not exist", e.getMessage());
			assertEquals("component", e.getEntityName());
		} catch (Exception e) {
			fail("Updating component with a non-existing component request should have resulted in EntityNotFoundException: " + e.getMessage());
		}
	}

	@Test
	void updateFromRequestIncorrectAuthenticationFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);

		Optional<Component> optionalComponent = Optional.of(component);
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			componentService.updateFromRequest(componentRequest);
			fail("Updating a component while not authenticated correctly should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Updating component with a component request while not correctly authenticated should have resulted in ResponseStatusException: " + e.getMessage());
		}
	}

	@Test
	void updateFromRequestNoModifyPermissionFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(accessService.hasModifyPermission(component.getAclId())).thenReturn(false);

		try {
			componentService.updateFromRequest(componentRequest);
			fail("Updating a component while not authenticated correctly should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"User has no permission to update component\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Updating component with a component request while not having modify permission should have resulted in ResponseStatusException: " + e.getMessage());
		}
	}

	@Test
	void updateFromRequestNoAcl() throws VempainAclException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		ComponentRequest componentRequest = TestUTCTools.generateComponentRequestFromComponent(component);
		componentRequest.setAcls(null);
		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(accessService.hasModifyPermission(component.getAclId())).thenReturn(true);
		doThrow(new VempainAclException("No ACL entries in request")).when(aclService)
																	 .updateFromRequestList(componentRequest.getAcls());
		// when(componentRepository.save(any(Component.class))).thenReturn(component);

		try {
			componentService.updateFromRequest(componentRequest);
			fail("Updating a component with a request which has no ACL list should have failed");
		} catch (VempainAclException e) {
			assertEquals("No ACL entries in request", e.getMessage());
		} catch (Exception e) {
			fail("Updating component with no ACL list should have resulted in VempainAclException");
		}
	}

	@Test
	void deleteByUserOk() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.hasDeletePermission(component.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(component.getAclId());
		doNothing().when(componentRepository)
				   .delete(component);

		try {
			componentService.deleteByUser(1L);
		} catch (Exception e) {
			fail("Deleting an existing component with correct permission should have succeeded");
		}
	}

	@Test
	void deleteByUserNoComponentFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);

		when(componentRepository.findById(component.getId())).thenReturn(Optional.empty());

		try {
			componentService.deleteByUser(1L);
			fail("Deleting a non-existing component should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("Deleting a non-existing component should have resulted in EntityNotFoundException");
		}
	}

	@Test
	void deleteByIdByUserNoSessionFail() {
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			componentService.deleteByUser(1L);
			fail("We should have received a ResponseStatusException when we don't have a valid session");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have received any other exception when deleting a component with no valid session: " + e);
		}
	}

	@Test
	void deleteByUserNoAccessFail() {
		Component component = TestUTCTools.generateComponent(1L, 1L);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.hasDeletePermission(component.getAclId())).thenReturn(false);

		try {
			componentService.deleteByUser(1L);
			fail("Deleting a existing component with no access should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have received any other exception when deleting a component with no no permission: " + e);
		}
	}

	@Test
	void deleteByUserDeleteAclFail() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.hasDeletePermission(component.getAclId())).thenReturn(true);
		doThrow(new VempainEntityNotFoundException("ACL not found for deletion", "acltest")).when(aclService)
																							.deleteByAclId(component.getAclId());

		try {
			componentService.deleteByUser(1L);
		} catch (Exception e) {
			fail("Deleting a existing component with no ACLs should have succeeded");
		}
	}

	@Test
	void deleteByUserDeleteAclUnknownExceptionFail() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.hasDeletePermission(component.getAclId())).thenReturn(true);
		doThrow(new RuntimeException("Test exception")).when(aclService)
													   .deleteByAclId(component.getAclId());

		try {
			componentService.deleteByUser(1L);
			fail("Deleting with unknown exceptions should have failed");
		} catch (VempainAclException e) {
			assertEquals("Failed to remove ACL", e.getMessage());
		} catch (Exception e) {
			fail("We should not have received any other exception when deleting a component and fail to delete ACLs: " + e);
		}
	}

	@Test
	void deleteByUserComponentNullFail() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);

		Optional<Component> optionalComponent = Optional.of(component);
		when(componentRepository.findById(component.getId())).thenReturn(optionalComponent);
		when(accessService.hasDeletePermission(component.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(component.getAclId());
		doThrow(new IllegalArgumentException("component is null")).when(componentRepository)
																  .delete(component);

		try {
			componentService.deleteByUser(1L);
			fail("Deleting a null component should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Deleting a null component should have resulted in ResponseStatusException");
		}
	}

	private void assertComponent(Component component) {
		assertNotNull(component);
		assertTrue(component.getId() > 0);
		assertNotNull(component.getCompName());
		assertNotNull(component.getCompData());
		assertTrue(component.getAclId() > 0);
		assertNotNull(component.getCreator());
		assertTrue(component.getCreator() > 0);
		assertNotNull(component.getCreated());
		assertNotNull(component.getModifier());
		assertTrue(component.getModifier() > 0);
		assertNotNull(component.getModified());
		assertTrue(component.getModified()
							.isAfter(component.getCreated()));
	}
}

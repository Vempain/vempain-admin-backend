package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.request.FormRequest;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.exception.EntityAlreadyExistsException;
import fi.poltsi.vempain.admin.exception.InvalidRequestException;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.AclService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormServiceUTC {
	private final static long itemCount = 10L;

	@Mock
	private AclService       aclService;
	@Mock
	private ComponentService componentService;
	@Mock
	private AccessService    accessService;
	@Mock
	private FormRepository       formRepository;
	@Mock
	private FormComponentService formComponentService;

	@InjectMocks
	private FormService formService;
	;

	@Test
	void findAllOk() {
		List<Form> forms = TestUTCTools.generateFormList(itemCount);
		when(formRepository.findAll()).thenReturn(forms);

		try {
			Iterable<Form> formList = formService.findAll();
			assertNotNull(formList);
			assertEquals(itemCount, StreamSupport.stream(formList.spliterator(), false)
												 .count());
		} catch (Exception e) {
			fail("We should not have received an exception when retrieving all forms: " + e.getMessage());
		}
	}

	@Test
	void findAllNullResultOk() {
		when(formRepository.findAll()).thenReturn(null);

		try {
			Iterable<Form> formList = formService.findAll();
			assertNull(formList);
		} catch (Exception e) {
			fail("We should not have received an exception when retrieving all forms and null returned: " + e.getMessage());
		}
	}

	@Test
	void findAllEmptyResultOk() {
		when(formRepository.findAll()).thenReturn(new ArrayList<>());

		try {
			Iterable<Form> formList = formService.findAll();
			assertNotNull(formList);
			assertEquals(0, StreamSupport.stream(formList.spliterator(), false)
										 .count());
		} catch (Exception e) {
			fail("We should not have received an exception when retrieving all forms and null returned: " + e.getMessage());
		}
	}

	@Test
	void findAllAsResponsesForUserOk() {
		List<Form> forms = TestUTCTools.generateFormList(itemCount);
		when(formRepository.findAll()).thenReturn(forms);
		when(accessService.hasReadPermission(anyLong())).thenReturn(true);

		try {
			List<FormResponse> responses = formService.findAllAsResponsesForUser(QueryDetailEnum.FULL);
			assertNotNull(responses);
			assertEquals(itemCount, responses.size());
		} catch (Exception e) {
			fail("We should not have received an exception when retrieving all forms as FormResponses: " + e.getMessage());
		}
	}

	@Test
	void findAllAsResponsesForUserNoAccessOk() {
		List<Form> forms = TestUTCTools.generateFormList(itemCount);
		when(formRepository.findAll()).thenReturn(forms);
		when(accessService.hasReadPermission(anyLong())).thenReturn(false);

		try {
			List<FormResponse> responses = formService.findAllAsResponsesForUser(QueryDetailEnum.FULL);
			assertNotNull(responses);
			assertEquals(0, responses.size());
		} catch (Exception e) {
			fail("We should not have received an exception when retrieving all forms as FormResponses: " + e.getMessage());
		}
	}

	@Test
	void findByIdOk() {
		var formId = 1L;
		Form form = TestUTCTools.generateForm(1L, 1L);
		form.setId(formId);
		when(formRepository.findById(formId)).thenReturn(Optional.of(form));

		try {
			Form returnForm = formService.findById(formId);
			assertNotNull(returnForm);
			assertEquals(1L, returnForm.getId());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void findByIdNullFail() {
		var formId = 1L;
		when(formRepository.findById(formId)).thenReturn(Optional.empty());

		try {
			formService.findById(formId);
			fail("Should have received an EntityNotFoundException");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Failed to find form by name", e.getMessage());
			assertEquals("form", e.getEntityName());
		} catch (Exception e) {
			fail("Should not have received any other exception for a null form");
		}
	}

	@Test
	void findByIdForUserOk() {
		var formId = 1L;
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.findById(formId)).thenReturn(Optional.of(form));
		when(accessService.hasReadPermission(anyLong())).thenReturn(true);

		try {
			FormResponse formResponse = formService.findByIdForUser(formId);
			assertNotNull(formResponse);
		} catch (Exception e) {
			fail("We should have received a form response instead of an exception: " + e.getMessage());
		}
	}

	@Test
	void findByIdForUserNullFormFail() {
		var formId = 1L;
		when(formRepository.findById(formId)).thenReturn(Optional.empty());

		try {
			formService.findByIdForUser(formId);
			fail("We should have received an EntityNotFoundException");
		} catch (VempainComponentException componentException) {
			fail("We should have received only an EntityNotFoundException");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Failed to find form by name", e.getMessage());
		}
	}

	@Test
	void findByIdForUserNoSessionFail() {
		var formId = 1L;
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			formService.findByIdForUser(formId);
			fail("Accessing form without session should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should have received an ResponseStatusException instead of some other exception: " + e);
		}
	}

	@Test
	void findByIdForUserNoAccessFail() {
		var formId = 1L;
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.findById(formId)).thenReturn(Optional.of(form));
		when(accessService.hasReadPermission(anyLong())).thenReturn(false);

		try {
			formService.findByIdForUser(formId);
			fail("Accessing form without permission should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should have received an ResponseStatusException instead of some other exception: " + e);
		}
	}

	@Test
	void findByFormNameOk() {
		Form form = TestUTCTools.generateForm(1L, 1L);
		String formName = form.getFormName();
		when(formRepository.findByFormName(formName)).thenReturn(form);

		try {
			Form returnForm = formService.findByFormName(formName);
			assertNotNull(returnForm);
		} catch (Exception e) {
			fail("We should have received a form response instead of an exception: " + e.getMessage());
		}
	}

	@Test
	void findByFormNameNullFail() {
		try {
			formService.findByFormName("not exist");
			fail("Should not have received any form");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Failed to find form by name", e.getMessage());
			assertEquals("form", e.getEntityName());
		}
	}

	@Test
	void deleteOk() throws VempainEntityNotFoundException {
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.findById(form.getId())).thenReturn(Optional.of(form));
		when(accessService.hasDeletePermission(anyLong())).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(form.getAclId());
		doNothing().when(formRepository)
				   .deleteById(form.getId());

		try {
			formService.delete(form.getId());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void deleteNullFormFoundFail() {
		when(formRepository.findById(anyLong())).thenReturn(Optional.empty());

		try {
			formService.delete(1L);
			fail("Deleting non-existing form ID should have produced an exception");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("No form can be found for deletion", e.getMessage());
			assertEquals("form", e.getEntityName());
		} catch (Exception e) {
			fail("Should have received EntityNotFoundException instead of: " + e);
		}
	}

	@Test
	void deleteNoDeletePermissionFail() {
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.findById(form.getId())).thenReturn(Optional.of(form));
		when(accessService.hasDeletePermission(anyLong())).thenReturn(false);

		try {
			formService.delete(1L);
			fail("Should not have been able to delete a form to which we don't have access");
		} catch (AccessDeniedException e) {
			assertEquals("User does not have permission to delete form", e.getMessage());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Disabled(value = "We have disabled the ACL check in form deletion for the time being")
	@Test
	void deleteNoAclFoundFail() throws VempainEntityNotFoundException {
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.findById(form.getId())).thenReturn(Optional.of(form));
		when(accessService.hasDeletePermission(anyLong())).thenReturn(true);
		doThrow(new VempainEntityNotFoundException("ACL not found for deletion", "acl")).when(aclService)
																						.deleteByAclId(anyLong());

		try {
			formService.delete(form.getId());
			fail("Should not be able to delete a form with a non-existing ACL ID");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Failed to remove Acl", e.getMessage());
			assertEquals("acl", e.getEntityName());
		} catch (Exception e) {
			fail("Should have received EntityNotFoundException exception: " + e.getMessage());
		}
	}

	@Test
	void deleteExceptionFromRepositoryFail() throws VempainEntityNotFoundException {
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.findById(form.getId())).thenReturn(Optional.of(form));
		when(accessService.hasDeletePermission(anyLong())).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(form.getAclId());
		doThrow(new NullPointerException()).when(formRepository)
										   .deleteById(form.getId());

		try {
			formService.delete(form.getId());
			fail("Should have received a RuntimeException");
		} catch (ProcessingFailedException e) {
			assertEquals("Unknown exception when deleting form", e.getMessage());
		} catch (Exception e) {
			fail("Should have received ProcessingFailedException exception: " + e.getMessage());
		}
	}

	@Test
	void saveOk() {
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(formRepository.save(form)).thenReturn(form);

		try {
			Form myForm = formService.save(form);
			assertNotNull(myForm);
			assertEquals(1L, myForm.getId());
		} catch (Exception e) {
			fail("Saving a form should not have created an exception");
		}
	}

	@Test
	void saveRequestOk() throws VempainAclException, VempainComponentException {
		FormRequest formRequest = setupSaveRequest();

		try {
			FormResponse formResponse = formService.saveRequest(formRequest);
			assertNotNull(formResponse);
		} catch (Exception e) {
			fail("We should not have received an exception when saving a correctly formed FormRequest, but we got: " + e.getMessage());
		}
	}

	@Test
	void saveRequestFormNameAlreadyExistsFail() throws VempainAclException, VempainComponentException {
		List<AclRequest> aclRequests = TestUTCTools.generateAclRequestList(1L, 1L);
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		var formRequest = FormRequest.builder()
									 .id(1L)
									 .name("Test form")
									 .layoutId(1L)
									 .acls(aclRequests)
									 .locked(false)
									 .components(componentRequests)
									 .build();
		when(formRepository.findByFormName(formRequest.getName())).thenReturn(null);
		when(formRepository.findByFormName(formRequest.getName())).thenReturn(Form.builder()
																				  .build());

		try {
			formService.saveRequest(formRequest);
			fail("We should have received an exception when saving a form with an already existing form name");
		} catch (EntityAlreadyExistsException e) {
			assertEquals("Tried to save a form with an existing name", e.getMessage());
			assertEquals("form", e.getEntityName());
		} catch (Exception e) {
			fail("We should have received an EntityAlreadyExistsException exception: " + e.getMessage());
		}
	}

	@Test
	void saveRequestNullComponentsListFail() throws VempainAclException, VempainComponentException {
		List<AclRequest> aclRequests = TestUTCTools.generateAclRequestList(1L, 1L);
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		var formRequest = FormRequest.builder()
									 .id(1L)
									 .name("Test form")
									 .layoutId(1L)
									 .acls(aclRequests)
									 .locked(false)
									 .components(componentRequests)
									 .build();
		when(formRepository.findByFormName(formRequest.getName())).thenReturn(null);
		testFormServiceWithComponentList(formRequest, null);
	}

	@Test
	void saveRequestEmptyComponentsListFail() throws VempainAclException, VempainComponentException {
		List<AclRequest> aclRequests = TestUTCTools.generateAclRequestList(1L, 1L);
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		var formRequest = FormRequest.builder()
									 .id(1L)
									 .name("Test form")
									 .layoutId(1L)
									 .acls(aclRequests)
									 .locked(false)
									 .components(componentRequests)
									 .build();
		when(formRepository.findByFormName(formRequest.getName())).thenReturn(null);

		testFormServiceWithComponentList(formRequest, new ArrayList<>());
	}

	private void testFormServiceWithComponentList(FormRequest formRequest, List<ComponentRequest> components) {
		formRequest.setComponents(components);

		try {
			formService.saveRequest(formRequest);
			fail("We should have received an exception when saving a form with null or empty component list");
		} catch (InvalidRequestException e) {
			assertEquals("Missing component list in form request", e.getMessage());
		} catch (Exception e) {
			fail("We should have received an InvalidRequestException exception: " + e.getMessage());
		}
	}

	@Test
	void saveRequestAclIdFail() throws VempainAclException, VempainComponentException {
		List<AclRequest> aclRequests = TestUTCTools.generateAclRequestList(1L, 1L);
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		var formRequest = FormRequest.builder()
									 .id(1L)
									 .name("Test form")
									 .layoutId(1L)
									 .acls(aclRequests)
									 .locked(false)
									 .components(componentRequests)
									 .build();
		when(formRepository.findByFormName(formRequest.getName())).thenReturn(null);
		when(aclService.getNextAclId()).thenReturn(1L);

		doThrow(new VempainAclException("New ACL ID is invalid")).when(aclService)
																 .saveAclRequests(anyLong(), any());

		try {
			formService.saveRequest(formRequest);
			fail("We should have received an exception when saving a form when failing to generate new ACL ID");
		} catch (InvalidRequestException e) {
			assertEquals("Failed to save the ACLs of request", e.getMessage());
		} catch (Exception e) {
			fail("We should have received an InvalidRequestException exception: " + e.getMessage());
		}
	}

	@Test
	void saveRequestInvalidComponentIdFail() throws VempainAclException, VempainComponentException {
		FormRequest formRequest = setupSaveRequest();
		ComponentRequest componentRequest = ComponentRequest.builder()
															.id(-1L)
															.build();
		formRequest.getComponents()
				   .add(componentRequest);

		try {
			formService.saveRequest(formRequest);
			fail("We should have received an exception when saving a form when request contains invalid component ID");
		} catch (InvalidRequestException e) {
			assertEquals("Malformed component ID in form request", e.getMessage());
		} catch (Exception e) {
			fail("We should have received an InvalidRequestException exception: " + e.getMessage());
		}
	}

	@Test
	void saveRequestNoComponentFoundFail() throws VempainAclException, VempainComponentException {
		FormService spyService = spy(formService);
		doThrow(new VempainComponentException("Failed to find component")).when(spyService)
																		  .getFormResponse(any(Form.class));
		FormRequest formRequest = setupSaveRequest();

		try {
			spyService.saveRequest(formRequest);
			fail("We should have received an exception when saving a form and component does not exist");
		} catch (ProcessingFailedException e) {
			assertEquals("Failed to retrieve the saved form", e.getMessage());
		} catch (Exception e) {
			fail("We should have received an ProcessingFailedException exception: " + e.getMessage());
		}
	}

	private FormRequest setupSaveRequest() throws VempainComponentException {
		var aclRequests = TestUTCTools.generateAclRequestList(1L, 1L);
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		var formRequest = FormRequest.builder()
									 .id(1L)
									 .name("Test form")
									 .layoutId(1L)
									 .acls(aclRequests)
									 .locked(false)
									 .components(componentRequests)
									 .build();
		when(formRepository.findByFormName(formRequest.getName())).thenReturn(null);
		when(aclService.getNextAclId()).thenReturn(1L);
		// The return value is not actually used anywhere
		when(formRepository.save(any(Form.class))).thenReturn(Form.builder()
																  .build());
		when(componentService.findById(anyLong())).thenReturn(Component.builder()
																	   .build());

		return formRequest;
	}

	@Test
	void saveFormComponentsOk() throws VempainComponentException {
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		Form form = TestUTCTools.generateForm(1L, 1L);
		when(componentService.findById(anyLong())).thenReturn(Component.builder()
																	   .build());

		try {
			formService.saveFormComponents(componentRequests, form);
		} catch (Exception e) {
			fail("Saving well formed component list and form should have succeeded, instead we got: " + e.getMessage());
		}
	}

	@Test
	void saveFormComponentsNoComponentFail() throws VempainComponentException {
		var componentRequests = TestUTCTools.generateComponentRequestList(4L);
		Form form = TestUTCTools.generateForm(1L, 1L);
		doThrow(new VempainComponentException("Failed to find component")).when(componentService)
																		  .findById(anyLong());

		try {
			formService.saveFormComponents(componentRequests, form);
			fail("Saving form component should have failed due to reference to non-existing component");
		} catch (InvalidRequestException e) {
			assertEquals("Malformed component ID in form request", e.getMessage());
		} catch (Exception e) {
			fail("Saving form component with non-existing component should have failed with InvalidRequestException");
		}
	}
}

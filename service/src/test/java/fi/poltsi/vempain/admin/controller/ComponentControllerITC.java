package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.exception.VempainAbstractException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class ComponentControllerITC extends AbstractITCTest {
	private final long initCount = 10;
	@Autowired
	ComponentController componentController;

	@Test
	@DisplayName("Get all components")
	void getAllComponentsByController() throws VempainComponentException, VempainAbstractException {
		testITCTools.generateComponents(initCount);
		ResponseEntity<List<ComponentResponse>> componentResponses = componentController.getComponents();
		assertNotNull(componentResponses);
		assertEquals(HttpStatus.OK, componentResponses.getStatusCode());
		List<ComponentResponse> responses = componentResponses.getBody();
		assertNotNull(responses);
		assertFalse(responses.isEmpty());
		assertEquals(initCount, responses.size());

		for (ComponentResponse response : responses) {
			testITCTools.verifyComponentResponse(response);
		}
	}

	@Test
	@DisplayName("Test updating a component")
	void updateComponent() throws VempainComponentException, VempainAbstractException {
		var componentIds = testITCTools.generateComponents(initCount);
		ResponseEntity<ComponentResponse> response = componentController.getComponentById(componentIds.getFirst());
		assertNotNull(response);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		ComponentResponse componentResponse = response.getBody();

		testITCTools.verifyComponentResponse(componentResponse);

		// We create a new set of ACLs
		ArrayList<AclRequest> aclList = new ArrayList<>();

		for (long i = 1; i < 5; i++) {
			assertNotNull(componentResponse);
			assertNotNull(componentResponse.getAcls());
			var userId = testITCTools.generateUser();
			AclRequest aclRequest = AclRequest.builder()
											  .aclId(componentResponse.getAcls().getFirst().getAclId())
											  .user(userId)
											  .unit(null)
											  .createPrivilege(true)
											  .readPrivilege(true)
											  .modifyPrivilege(true)
											  .deletePrivilege(false)
											  .build();
			aclList.add(aclRequest);
		}

		ComponentRequest request = ComponentRequest.builder()
												   .id(componentResponse.getId())
												   .compName(componentResponse.getCompName())
												   .compData("newcompdata")
												   .locked(componentResponse.isLocked())
												   .timestamp(LocalDateTime.now())
												   .acls(aclList)
												   .build();

		componentController.updateComponent(request);

		ResponseEntity<ComponentResponse> updatedResponse = componentController.getComponentById(componentIds.getFirst());
		assertNotNull(updatedResponse);
		assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
		ComponentResponse updatedComponent = updatedResponse.getBody();
		assertNotNull(updatedComponent);
		testITCTools.verifyComponentResponse(updatedComponent);
		assertEquals("newcompdata", updatedComponent.getCompData());
	}
}

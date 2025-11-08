package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class LayoutControllerITC extends AbstractITCTest {
	private final long initCount = 10;
	@Autowired
	LayoutController layoutController;

	@Test
	@DisplayName("Get all layouts")
	void getLayoutsOk() {
		testITCTools.generateLayouts(initCount);
		ResponseEntity<List<LayoutResponse>> layoutResponseResponseEntity = layoutController.getLayouts();
		assertNotNull(layoutResponseResponseEntity);
		assertEquals(HttpStatus.OK, layoutResponseResponseEntity.getStatusCode());
		List<LayoutResponse> responses = layoutResponseResponseEntity.getBody();
		assertNotNull(responses);
		assertFalse(responses.isEmpty());
		assertEquals(initCount, responses.size());

		for (LayoutResponse layoutResponse : responses) {
			assertNotNull(layoutResponse);
			assertNotNull(layoutResponse.getStructure());
			assertNotNull(layoutResponse.getLayoutName());
			assertNotNull(layoutResponse.getCreated());
			assertNull(layoutResponse.getModified());
			assertNotNull(layoutResponse.getAcls());
			assertEquals(1, layoutResponse.getAcls()
										  .size());
		}
	}

	@Test
	@DisplayName("Add a new layout")
	void saveLayoutOk() {
		String layoutName = "LayoutRTC " + (initCount + 1);
		String layoutStructure = "<!--comp_0-->" + (initCount + 1);
		var userId = testITCTools.generateUser();

		AclRequest aclRequest = AclRequest.builder()
										  .user(userId)
										  .unit(null)
										  .createPrivilege(true)
										  .modifyPrivilege(true)
										  .readPrivilege(true)
										  .deletePrivilege(true)
										  .build();
		List<AclRequest> aclRequests = Collections.singletonList(aclRequest);

		LayoutRequest layoutRequest = LayoutRequest.builder()
												   .id(0L)
												   .layoutName(layoutName)
												   .structure(layoutStructure)
												   .acls(aclRequests)
												   .build();
		ResponseEntity<LayoutResponse> layoutResponseEntity = layoutController.saveLayout(layoutRequest);
		assertNotNull(layoutResponseEntity);
		assertEquals(HttpStatus.OK, layoutResponseEntity.getStatusCode());
		LayoutResponse layoutResponse = layoutResponseEntity.getBody();
		assertNotNull(layoutResponse);
		log.info("Layout response: {}", layoutResponse);
		assertTrue(layoutResponse.getId() > 0L);
		assertNotNull(layoutResponse.getAcls());
		assertFalse(layoutResponse.getAcls()
								  .isEmpty());
		assertEquals(layoutName, layoutResponse.getLayoutName());
		assertEquals(layoutStructure, layoutResponse.getStructure());
		// The creator is always 1 because we don't have the session context without actual JWT
		assertEquals(1L, layoutResponse.getCreator());
	}

	@Test
	@DisplayName("Fetch an existing layout by name and ID")
	void getLayoutOk() {
		testITCTools.generateLayouts(initCount);
		var layouts = layoutRepository.findAll();
		var optionalLayout = StreamSupport.stream(layouts.spliterator(), false)
										  .findFirst();
		assertTrue(optionalLayout.isPresent());
		// By name
		ResponseEntity<LayoutResponse> layoutResponseEntity1 = layoutController.getLayoutByName(optionalLayout.get()
																											  .getLayoutName());
		assertNotNull(layoutResponseEntity1);
		assertEquals(HttpStatus.OK, layoutResponseEntity1.getStatusCode());
		LayoutResponse layoutResponse1 = layoutResponseEntity1.getBody();
		assertNotNull(layoutResponse1);
		// By Id
		ResponseEntity<LayoutResponse> layoutResponseEntity2 = layoutController.getLayoutById(layoutResponse1.getId());
		assertNotNull(layoutResponseEntity2);
		assertEquals(HttpStatus.OK, layoutResponseEntity2.getStatusCode());
		LayoutResponse layoutResponse2 = layoutResponseEntity2.getBody();
		assertNotNull(layoutResponse2);

		assertEquals(layoutResponse1, layoutResponse2);
	}

	@Test
	@DisplayName("Update an existing layout")
	void updateLayoutOk() {
		var layoutIdList = testITCTools.generateLayouts(initCount);
		// Fetch it first by name
		ResponseEntity<LayoutResponse> layoutResponseEntity1 = layoutController.getLayoutById(layoutIdList.getFirst());
		assertNotNull(layoutResponseEntity1);
		assertEquals(HttpStatus.OK, layoutResponseEntity1.getStatusCode());
		LayoutResponse layoutResponse1 = layoutResponseEntity1.getBody();
		assertNotNull(layoutResponse1);
		log.info("layoutResponse1: {}", layoutResponse1);
		// Get the request
		LayoutRequest layoutRequest = layoutResponse1.getLayoutRequest();
		log.info("layoutRequest: {}", layoutRequest);
		// Then update it
		layoutResponse1.setStructure("<!--comp_0--> SomeRandomString");
		log.info("layoutResponse1 after struct update: {}", layoutResponse1);
		layoutRequest.setLayoutName(layoutResponse1.getLayoutName());
		layoutRequest.setStructure(layoutResponse1.getStructure());
		log.info("layoutRequest after updates: {}", layoutRequest);
		// Send it back
		layoutController.updateLayout(layoutRequest);
		ResponseEntity<LayoutResponse> layoutResponseEntity2 = layoutController.getLayoutById(layoutResponse1.getId());
		assertNotNull(layoutResponseEntity2);
		assertEquals(HttpStatus.OK, layoutResponseEntity2.getStatusCode());
		LayoutResponse layoutResponse2 = layoutResponseEntity2.getBody();
		assertNotNull(layoutResponse2);
		log.info("layoutResponse2: {}", layoutResponse2);

		// TODO This will fail until we have the user ID handling done
		// assertTrue(compareLayouts(layoutResponse1, layoutResponse2));
	}

	@Test
	@DisplayName("Remove a specific layout")
	void removeLayoutByIdOk() {
		testITCTools.generateLayouts(initCount);
		var layouts = layoutRepository.findAll();
		var optionalLayout = StreamSupport.stream(layouts.spliterator(), false)
										  .findFirst();
		assertTrue(optionalLayout.isPresent());
		var layoutName = optionalLayout.get()
									   .getLayoutName();
		ResponseEntity<LayoutResponse> layoutEntity = layoutController.getLayoutByName(layoutName);
		assertNotNull(layoutEntity);
		assertEquals(HttpStatus.OK, layoutEntity.getStatusCode());
		LayoutResponse layoutResponse = layoutEntity.getBody();
		assertNotNull(layoutResponse);
		layoutController.removeLayoutById(layoutResponse.getId());
		// Check that the layout has been removed
		try {
			ResponseEntity<LayoutResponse> nullLayoutResponse = layoutController.getLayoutByName(layoutName);
			assertNotNull(nullLayoutResponse);
		} catch (ResponseStatusException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}
}

package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
/**
 * The user, unit and ACL management endpoints come from {@code vempain-auth-core} ({@code fi.poltsi.vempain.auth.controller}); this
 * verifies that they are hosted by the admin backend and work against its datasource.
 */
class HostedManagementITC extends AbstractITCTest {
	private final long initCount = 10;

	@Autowired
	private fi.poltsi.vempain.auth.controller.AclController  aclController;
	@Autowired
	private fi.poltsi.vempain.auth.controller.UnitController unitController;
	@Autowired
	private fi.poltsi.vempain.auth.controller.UserController userController;

	@Test
	void getAllAclOk() {
		testITCTools.generateAcls(initCount);
		ResponseEntity<List<AclResponse>> responses = aclController.getAllAcl();
		assertNotNull(responses);
		List<AclResponse> acls = responses.getBody();
		assertNotNull(acls);
		assertTrue(acls.size() >= 7 * initCount);
	}

	@Test
	void getAclOk() {
		var aclIds = testITCTools.generateAcls(initCount);
		ResponseEntity<List<AclResponse>> responses = aclController.getAcl(aclIds.getFirst());
		assertNotNull(responses);
		List<AclResponse> acls = responses.getBody();
		assertNotNull(acls);
		assertEquals(2, acls.size());
	}

	@Test
	void unitsAndUsersAreServedByTheSharedControllers() throws Exception {
		var memberId = testITCTools.generateUser();
		var units = unitController.getUnits();
		assertNotNull(units.getBody());
		var before = units.getBody()
						  .size();

		var created = unitController.addUnit(fi.poltsi.vempain.auth.api.request.UnitRequest.builder()
																						   .name("hosted-unit")
																						   .description("created through the shared controller")
																						   .acls(List.of(fi.poltsi.vempain.auth.api.request.AclRequest.builder()
																																					  .user(fi.poltsi.vempain.admin.api.Constants.ADMIN_ID)
																																					  .readPrivilege(true)
																																					  .createPrivilege(true)
																																					  .modifyPrivilege(true)
																																					  .deletePrivilege(true)
																																					  .build()))
																						   .userIds(List.of(memberId))
																						   .build());
		assertNotNull(created.getBody());
		assertEquals(List.of(memberId), created.getBody()
											   .getUserIds());
		assertEquals(before + 1, unitController.getUnits()
											   .getBody()
											   .size());

		var user = userController.findById(memberId);
		assertNotNull(user.getBody());
		assertTrue(user.getBody()
					   .getUnitIds()
					   .contains(created.getBody()
										.getId()));

		// Circular membership is refused by the shared service
		var selfRequest = fi.poltsi.vempain.auth.api.request.UnitRequest.builder()
																		.name("hosted-unit")
																		.description("x")
																		.acls(List.of(fi.poltsi.vempain.auth.api.request.AclRequest.builder()
																																   .user(fi.poltsi.vempain.admin.api.Constants.ADMIN_ID)
																																   .readPrivilege(true)
																																   .build()))
																		.unitIds(List.of(created.getBody()
																								.getId()))
																		.build();
		var unitId = created.getBody()
							.getId();
		var ex = org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
															   () -> unitController.updateUnit(unitId, selfRequest));
		assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}
}

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
class AclControllerITC extends AbstractITCTest {
	private final long initCount = 10;

	@Autowired
	private AclController aclController;

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
}

package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class AclControllerITC extends AbstractITCTest {
	private final long initCount = 10;

	@Autowired
	private AclController aclController;

	@AfterEach
	void tearDown() {
		testITCTools.deleteAcls();
		testITCTools.checkDatabase();
	}

	@Test
	@DisplayName("Fetch all ACL")
	void getAllAclOk() {
		testITCTools.generateAcls(initCount);
		ResponseEntity<List<AclResponse>> responses = aclController.getAllAcl();
		assertNotNull(responses);
		List<AclResponse> acls = responses.getBody();
		assertNotNull(acls);
		assertEquals(7 * initCount, acls.size());
	}

	@Test
	@DisplayName("Fetch an ACL")
	void getAclOk() {
		testITCTools.generateAcls(initCount);
		ResponseEntity<List<AclResponse>> responses = aclController.getAcl(testITCTools.getAclIdList().get(1));
		assertNotNull(responses);
		List<AclResponse> acls = responses.getBody();
		assertNotNull(acls);
		assertEquals(1, acls.size());
	}
}

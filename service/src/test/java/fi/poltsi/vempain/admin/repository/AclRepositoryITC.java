package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Acl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class AclRepositoryITC extends AbstractITCTest {
	private final static long initCount = 10L;

	@AfterEach
	void tearDown() {
		testITCTools.deleteAcls();
	}

	@Test
	void getNextAclId() {
		testITCTools.generateAcls(initCount);
		Long nextId = aclRepository.getNextAvailableAclId();
		assertNotNull(nextId);
		log.info("Next ID: {}", nextId);

		assertTrue(nextId > 0);
		aclRepository.deleteAll();

		nextId = aclRepository.getNextAvailableAclId();
		assertNotNull(nextId);
		log.info("Next ID: {}", nextId);

		assertEquals(1L, nextId);
	}

	@Test
	void getAllAclOk() {
		testITCTools.generateAcls(initCount);
		var aclList = aclRepository.findAll();
		assertNotNull(aclList);
		assertEquals(7 * initCount, aclList.size());
		testITCTools.cleanupCreatedAcls();
	}

	@Test
	@Transactional
	void deleteAcl() {
		var userId = testITCTools.generateUser();
		var aclId = testITCTools.generateAcl(userId, null, true, true, true, true);

		List<Acl> aclList = aclRepository.getAclByAclId(aclId);
		assertNotNull(aclList);
		assertEquals(1, aclList.size());
		assertEquals(aclId, aclList.getFirst().getAclId());
		log.info("Found acl from database with ID: {}", aclList.getFirst().getAclId());
		aclRepository.deleteAclsByAclId(aclId);
		List<Acl> emptyAclList = aclRepository.getAclByAclId(aclId);
		assertNotNull(emptyAclList);
		assertTrue(emptyAclList.isEmpty());

		testITCTools.cleanupCreatedAcls();
	}
}

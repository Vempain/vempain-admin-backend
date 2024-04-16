package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class AclServiceITC extends AbstractITCTest {
	static final long initCount = 5;

	@AfterEach
	void tearDown() {
		testITCTools.checkDatabase();
		testITCTools.cleanupCreatedAcls();
	}

	@Test
	void getAllOk() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAll();
		assertEquals((7 * initCount), StreamSupport.stream(acls.spliterator(), false).count());
		long userCount = 0;
		long unitCount = 0;

		for (Acl acl : acls) {
			if (acl.getUnitId() != null) {
				unitCount++;
				assertNull(acl.getUserId());
			}

			if (acl.getUserId() != null) {
				userCount++;
				assertNull(acl.getUnitId());
			}
		}

		// Generation creates one for both user and unit. Every user creates another ACL as well as a unit, and every unit creates both an
		// ACL and a user, with a separate ACL, so 6 in total
		assertEquals(initCount * 6, userCount);
		assertEquals(initCount, unitCount);
	}

	@Test
	void getAclByAclIdOk() {
		var aclIds = testITCTools.generateAcls(initCount);
		assertNotNull(aclIds);
		assertEquals(initCount, aclIds.size());
		var acls = aclService.findAclByAclId(aclIds.get(0));
		assertNotNull(acls);
		assertEquals(2L, acls.size());

		long userCount = 0;
		long unitCount = 0;

		for (Acl acl : acls) {
			if (acl.getUnitId() != null) {
				unitCount++;
				assertNull(acl.getUserId());
			}

			if (acl.getUserId() != null) {
				userCount++;
				assertNull(acl.getUnitId());
			}

		}

		assertEquals(1, userCount);
		assertEquals(1, unitCount);
	}

	@Test
	void deleteByAclIdOk() {
		testITCTools.generateAcls(initCount);

		try {
			Long aclId = testITCTools.getAclIdList().get(0);
			aclService.deleteByAclId(aclId);
		} catch (VempainEntityNotFoundException e) {
			fail("Should have been able to delete Acl with id: " + (initCount - 1));
		}

		testITCTools.getAclIdList().remove(0);
		Iterable<Acl> acls = aclService.findAll();
		assertEquals((7 * initCount - 1), StreamSupport.stream(acls.spliterator(), false).count());
	}

	@Test
	void saveOk() {
		var userId = testITCTools.generateUser();
		var nextAcl = aclService.getNextAclId();
		Acl acl = Acl.builder()
					 .aclId(nextAcl)
					 .userId(userId)
					 .unitId(null)
					 .readPrivilege(true)
					 .createPrivilege(true)
					 .modifyPrivilege(true)
					 .deletePrivilege(true)
					 .build();

		try {
			aclService.save(acl);
			testITCTools.getAclIdList().add(nextAcl);
		} catch (Exception e) {
			fail("Creating ACL should have succeeded: " + e);
		}
	}


	@Test
	void updateOk() {
		var aclList = testITCTools.generateAcls(initCount);
		assertFalse(aclList.isEmpty());
		var acls = aclService.findAclByAclId(aclList.get(0));
		assertNotNull(acls);
		assertEquals(2L, acls.size());

		try {
			for (Acl acl : acls) {
				acl.setReadPrivilege(false);
				aclService.update(acl);
			}
		} catch (VempainAclException e) {
			fail("Updating ACL should have succeeded");
		}

		var checkAcls = aclService.findAclByAclId(aclList.get(0));
		assertNotNull(checkAcls);
		assertEquals(2L, checkAcls.size());

		for (Acl acl : checkAcls) {
			assertFalse(acl.isReadPrivilege());
			assertTrue(acl.isCreatePrivilege());
			assertTrue(acl.isModifyPrivilege());
			assertTrue(acl.isDeletePrivilege());
		}
	}

	@Test
	void updateFailsWithZeroAclId() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(testITCTools.getAclIdList().get(1));

		for (Acl acl : acls) {
			// The aclId is set to zero
			try {
				acl.setAclId(0L);
				aclService.update(acl);
				fail("AclId 0 should have caused an exception");
			} catch (VempainAclException e) {
				log.info("Exception message: {}", e.getMessage());
				assertTrue(e.getMessage().contains("Incorrect aclId value"));
			}
		}
	}

	@Test
	void updateFailsWithNegativeAclId() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(initCount - 1);

		for (Acl acl : acls) {
			// The aclId is set to zero
			try {
				acl.setAclId(-1L);
				aclService.update(acl);
				fail("AclId 0 should have caused an exception");
			} catch (VempainAclException e) {
				log.info("Exception message: {}", e.getMessage());
				assertTrue(e.getMessage().contains("Incorrect aclId value"));
			}
		}
	}

	@Test
	void updateFailsWithNullUserAndUnit() {
		var aclIdList = testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(aclIdList.get(0));

		for (Acl acl : acls) {
			// Update with both null user and unit
			try {
				acl.setUserId(null);
				acl.setUnitId(null);
				aclService.update(acl);
				fail("Null user and unit should have caused an exception");
			} catch (VempainAclException e) {
				log.info("Exception message: {}", e.getMessage());
				assertTrue(e.getMessage().contains("Both user and unit is null"));
			}
		}
	}

	@Test
	void updateFailsWithUserAndUnitSet() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(initCount - 1);

		for (Acl acl : acls) {
			// Update with both user and unit set
			try {
				acl.setUserId(1L);
				acl.setUnitId(1L);
				aclService.update(acl);
				fail("Set user and unit should have caused an exception");
			} catch (VempainAclException e) {
				log.info("Exception message: {}", e.getMessage());
				assertTrue(e.getMessage().contains("Both user and unit are set"));
			}
		}
	}

	@Test
	void updateWithAllPermissionsNoFail() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(initCount - 1);

		for (Acl acl : acls) {
			// All permissions set to no does not make sense
			try {
				acl.setReadPrivilege(false);
				acl.setCreatePrivilege(false);
				acl.setModifyPrivilege(false);
				acl.setDeletePrivilege(false);
				aclService.update(acl);
				fail("All-NO permission should have caused an exception");
			} catch (VempainAclException e) {
				log.info("Exception message: {}", e.getMessage());
				assertTrue(e.getMessage().contains("All permissions set to false, this acl does not make any sense"));
			}
		}
	}
}

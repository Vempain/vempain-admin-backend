package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AccessService} against real ACL rows: there is no test-mode bypass, so both granted and denied decisions are
 * exercised for the administrator and for an ordinary user.
 */
class AccessServiceITC extends AbstractITCTest {

	@Autowired
	private AccessService accessService;

	@Test
	void administratorHoldsTheReservedAdministratorAcl() {
		accessService.checkAdminAccess();
		assertEquals(ADMIN_ID, accessService.getUserId());
		assertEquals(ADMIN_ID, accessService.getValidUserId());
	}

	@Test
	void ordinaryUserIsNotAdministrator() {
		var userId = testITCTools.generateUser();
		authenticateAs(userId);

		var exception = assertThrows(ResponseStatusException.class, () -> accessService.checkAdminAccess());
		assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
	}

	@Test
	void permissionsFollowTheAclRowsOfTheResource() {
		var ownerId = testITCTools.generateUser();
		var otherId = testITCTools.generateUser();
		var aclId = testITCTools.generateAclForOwnerOnly(ownerId, null, true, false, true, false);

		authenticateAs(ownerId);
		assertTrue(accessService.hasReadPermission(aclId));
		assertTrue(accessService.hasCreatePermission(aclId));
		assertFalse(accessService.hasModifyPermission(aclId));
		assertFalse(accessService.hasDeletePermission(aclId));

		authenticateAs(otherId);
		assertFalse(accessService.hasReadPermission(aclId));
		assertFalse(accessService.hasCreatePermission(aclId));

		// The administrator has no row for this ACL either: no role based fallback exists
		authenticateAs(ADMIN_ID);
		assertFalse(accessService.hasReadPermission(aclId));
	}

	@Test
	void generatedTestAclsGrantTheAdministratorAsWell() {
		var ownerId = testITCTools.generateUser();
		var aclId = testITCTools.generateAcl(ownerId, null, true, true, false, false);

		assertTrue(accessService.hasReadPermission(aclId));
		assertTrue(accessService.hasModifyPermission(aclId));
		assertFalse(accessService.hasDeletePermission(aclId));
	}

	@Test
	void unassignedAndUnknownAclIdsAreDenied() {
		assertFalse(accessService.hasReadPermission(0L));
		assertFalse(accessService.hasReadPermission(aclService.getNextAclId() + 1_000L));
	}

	@Test
	void missingSessionIsRejected() {
		SecurityContextHolder.clearContext();

		assertThrows(SessionAuthenticationException.class, () -> accessService.hasReadPermission(1L));
		assertThrows(SessionAuthenticationException.class, () -> accessService.getUserId());
		var exception = assertThrows(ResponseStatusException.class, () -> accessService.getValidUserId());
		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
		var adminException = assertThrows(ResponseStatusException.class, () -> accessService.checkAdminAccess());
		assertEquals(HttpStatus.UNAUTHORIZED, adminException.getStatusCode());
	}

	@Test
	void siteFileListingContainsOnlyReadableFiles() {
		var ownerId = testITCTools.generateUser();
		var otherId = testITCTools.generateUser();

		var readable = saveSiteFile("acl-readable.jpg", testITCTools.generateAclForOwnerOnly(ownerId, null, true, true, true, true));
		var hidden = saveSiteFile("acl-hidden.jpg", testITCTools.generateAclForOwnerOnly(otherId, null, true, true, true, true));
		var noRead = saveSiteFile("acl-no-read.jpg", testITCTools.generateAclForOwnerOnly(ownerId, null, false, true, true, true));

		authenticateAs(ownerId);
		var request = new SiteFilePagedRequest();
		request.setFileType(FileTypeEnum.IMAGE);
		request.setPage(0);
		request.setSize(50);
		request.setSearch("acl-");
		request.setFilterColumn("file_name");

		var response = fileService.findAllSiteFilesAsPageableResponseFiltered(request);
		var ids = response.getContent()
						  .stream()
						  .map(file -> file.getId())
						  .toList();

		assertTrue(ids.contains(readable.getId()));
		assertFalse(ids.contains(hidden.getId()));
		assertFalse(ids.contains(noRead.getId()));
		assertEquals(1, response.getTotalElements());

		authenticateAs(ADMIN_ID);
		assertEquals(0, fileService.findAllSiteFilesAsPageableResponseFiltered(request)
								   .getTotalElements());
	}

	@Test
	void unitMembershipGrantsAccessThroughUnitAclRows() {
		var memberId = testITCTools.generateUser();
		var outsiderId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		var unit = unitRepository.findById(unitId)
								 .orElseThrow();
		var member = userAccountRepository.findById(memberId)
										  .orElseThrow();
		if (member.getUnits() == null) {
			member.setUnits(new java.util.HashSet<>());
		}
		member.getUnits()
			  .add(unit);
		userAccountRepository.save(member);

		var unitAclId = testITCTools.generateAclForOwnerOnly(null, unitId, true, false, false, false);
		var unitFile = saveSiteFile("acl-unit.jpg", unitAclId);

		authenticateAs(memberId);
		assertTrue(accessService.hasReadPermission(unitAclId));
		assertFalse(accessService.hasModifyPermission(unitAclId));
		var request = new SiteFilePagedRequest();
		request.setFileType(FileTypeEnum.IMAGE);
		request.setPage(0);
		request.setSize(50);
		request.setSearch("acl-unit");
		request.setFilterColumn("file_name");
		assertEquals(1, fileService.findAllSiteFilesAsPageableResponseFiltered(request)
								   .getTotalElements());
		assertEquals(unitFile.getId(), fileService.findAllSiteFilesAsPageableResponseFiltered(request)
												  .getContent()
												  .getFirst()
												  .getId());

		authenticateAs(outsiderId);
		assertFalse(accessService.hasReadPermission(unitAclId));
		assertEquals(0, fileService.findAllSiteFilesAsPageableResponseFiltered(request)
								   .getTotalElements());
	}

	private SiteFile saveSiteFile(String fileName, long aclId) {
		var siteFile = SiteFile.builder()
							   .aclId(aclId)
							   .fileId(fileName.hashCode() & 0xffff)
							   .fileName(fileName)
							   .filePath("/itc/acl/")
							   .mimeType("image/jpeg")
							   .size(1024L)
							   .fileType(FileTypeEnum.IMAGE)
							   .comment("")
							   .metadata("{}")
							   .sha256sum("sha-" + fileName)
							   .creator(ADMIN_ID)
							   .created(Instant.now())
							   .build();
		return fileService.saveSiteFile(siteFile);
	}
}

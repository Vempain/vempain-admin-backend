package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.auth.entity.Acl;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ingests files end to end against the real ACL tables: the ingesting administrator keeps every privilege and the additional grantees of
 * the request get their own rows on the ACL of the site file and of the gallery, on creation as well as on update.
 */
class FileIngestServiceITC extends AbstractITCTest {
	// A real image, because ingesting into a gallery generates a thumbnail of the stored file
	private static final byte[] CONTENT = readTestImage();

	@Autowired
	private FileIngestService fileIngestService;

	@Test
	void additionalGranteesGetRowsOnTheSiteFileAndGalleryAcls() throws Exception {
		var granteeId = testITCTools.generateUser();
		var request = request("itc-acl.jpeg", "gallery-with-acl", List.of(acl(granteeId, true, false, true, false)));

		var response = fileIngestService.ingest(request, multipart("itc-acl.jpeg"));

		assertNotNull(response.getSiteFileId());
		assertNotNull(response.getGalleryId());
		var siteFile = siteFileRepository.findById(response.getSiteFileId())
		                                 .orElseThrow();
		var gallery = galleryRepository.findById(response.getGalleryId())
		                               .orElseThrow();

		for (var aclId : List.of(siteFile.getAclId(), gallery.getAclId())) {
			var rows = aclRepository.getAclByAclId(aclId);
			assertEquals(2, rows.size(), "owner row + grantee row on ACL " + aclId);
			var owner = rowOf(rows, ADMIN_ID);
			assertTrue(owner.isReadPrivilege() && owner.isCreatePrivilege() && owner.isModifyPrivilege() && owner.isDeletePrivilege());
			var grantee = rowOf(rows, granteeId);
			assertTrue(grantee.isReadPrivilege());
			assertFalse(grantee.isCreatePrivilege());
			assertTrue(grantee.isModifyPrivilege());
			assertFalse(grantee.isDeletePrivilege());
		}

		// Re-ingesting the same file into the same gallery with changed privileges updates the rows instead of duplicating them
		var secondGrantee = testITCTools.generateUser();
		var update = request("itc-acl.jpeg", "gallery-with-acl",
							 List.of(acl(granteeId, true, false, false, false), acl(secondGrantee, true, true, true, true)));
		update.setGalleryId(response.getGalleryId());

		var updated = fileIngestService.ingest(update, multipart("itc-acl.jpeg"));

		assertEquals(response.getSiteFileId(), updated.getSiteFileId());
		assertTrue(updated.isUpdated());
		for (var aclId : List.of(siteFile.getAclId(), gallery.getAclId())) {
			var rows = aclRepository.getAclByAclId(aclId);
			assertEquals(3, rows.size(), "owner + two grantees on ACL " + aclId);
			assertFalse(rowOf(rows, granteeId).isModifyPrivilege(), "privileges of the first grantee were replaced");
			assertTrue(rowOf(rows, secondGrantee).isDeletePrivilege());
		}
	}

	@Test
	void unknownGranteeIsRejectedAndNothingIsStored() {
		var request = request("itc-rejected.jpeg", "never-created", List.of(acl(987654L, true, false, false, false)));
		var galleriesBefore = galleryRepository.count();
		var siteFilesBefore = siteFileRepository.count();

		assertThrows(IllegalArgumentException.class, () -> fileIngestService.ingest(request, multipart("itc-rejected.jpeg")));

		assertEquals(galleriesBefore, galleryRepository.count());
		assertEquals(siteFilesBefore, siteFileRepository.count());
		assertTrue(galleryRepository.findByShortname("never-created")
		                            .isEmpty());
	}

	private static byte[] readTestImage() {
		try {
			return Files.readAllBytes(Path.of("src/test/resources/files/Norja-2019-0097.jpeg"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Acl rowOf(List<Acl> rows, long userId) {
		return rows.stream()
				   .filter(row -> Objects.equals(row.getUserId(), userId))
				   .findFirst()
				   .orElseThrow(() -> new AssertionError("No ACL row for user " + userId));
	}

	private static FileIngestAclRequest acl(long userId, boolean read, boolean create, boolean modify, boolean delete) {
		return FileIngestAclRequest.builder()
								   .userId(userId)
								   .readPrivilege(read)
								   .createPrivilege(create)
								   .modifyPrivilege(modify)
								   .deletePrivilege(delete)
								   .build();
	}

	private static MockMultipartFile multipart(String fileName) {
		return new MockMultipartFile("site_file", fileName, "image/jpeg", CONTENT);
	}

	private static FileIngestRequest request(String fileName, String galleryName, List<FileIngestAclRequest> acls) {
		return FileIngestRequest.builder()
								.fileName(fileName)
								.filePath("itc/acl")
								.mimeType("image/jpeg")
								.comment("")
								.metadata("{}")
								.sha256sum(DigestUtils.sha256Hex(CONTENT))
								.galleryName(galleryName)
								.galleryDescription("ITC gallery")
								.tags(List.of())
								.acls(acls)
								.build();
	}
}

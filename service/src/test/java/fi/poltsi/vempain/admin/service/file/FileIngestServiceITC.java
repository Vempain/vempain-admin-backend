package fi.poltsi.vempain.admin.service.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.auth.entity.Acl;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ingests files end to end against the real ACL tables: the ingesting administrator keeps every privilege and the additional grantees of the request get their
 * own rows on the ACL of the site file and of the gallery, on creation as well as on update.
 */
class FileIngestServiceITC extends AbstractITCTest {

    // A real image, because ingesting into a gallery generates a thumbnail of the stored file
    private static final byte[] CONTENT = readTestImage();
    private static final long FILE_ID = 4321L;

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
        assertEquals(FILE_ID, siteFile.getFileId());

        for (var aclId : List.of(siteFile.getAclId(), gallery.getAclId())) {
            var rows = aclRepository.getAclByAclId(aclId);
            assertEquals(2, rows.size(), "owner row + grantee row on ACL " + aclId);
            var owner = rowOf(rows, ADMIN_ID);
            assertTrue(owner.isReadPrivilege()
                       && owner.isCreatePrivilege()
                       && owner.isModifyPrivilege()
                       && owner.isDeletePrivilege());
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
    void theAclLookupsAnswerTheAdditionalGranteesAndTheGallerySiteFiles() throws Exception {
        var granteeId = testITCTools.generateUser();
        var request = request("itc-lookup.jpeg", "gallery-lookup", List.of(acl(granteeId, true, false, true, false)));

        var response = fileIngestService.ingest(request, multipart("itc-lookup.jpeg"));

        assertEquals(List.of(response.getSiteFileId()), response.getGallerySiteFileIds());

        var siteFileAcls = fileIngestService.getSiteFileAcls(response.getSiteFileId());
        assertEquals(response.getSiteFileId(), siteFileAcls.getSiteFileId());
        assertEquals(FILE_ID, siteFileAcls.getFileId());
        // The ingesting administrator's own row is not an additional grantee
        assertEquals(1, siteFileAcls.getAcls()
            .size());
        var grantee = siteFileAcls.getAcls()
            .getFirst();
        assertEquals(granteeId, grantee.getUserId());
        assertNull(grantee.getUnitId());
        assertTrue(grantee.isReadPrivilege());
        assertFalse(grantee.isCreatePrivilege());
        assertTrue(grantee.isModifyPrivilege());
        assertFalse(grantee.isDeletePrivilege());

        var galleryAcls = fileIngestService.getGalleryAcls(response.getGalleryId());
        assertEquals("gallery-lookup", galleryAcls.getShortname());
        assertEquals(List.of(response.getSiteFileId()), galleryAcls.getSiteFileIds());
        assertEquals(1, galleryAcls.getAcls()
            .size());
        assertEquals(granteeId, galleryAcls.getAcls()
            .getFirst()
            .getUserId());

        // A second file of the same group extends the gallery's site file list, in gallery order
        var second = request("itc-lookup-2.jpeg", "gallery-lookup", null);
        second.setFileId(FILE_ID + 1);
        second.setGalleryId(response.getGalleryId());
        second.setSortOrder(1);
        var secondResponse = fileIngestService.ingest(second, multipart("itc-lookup-2.jpeg"));

        assertEquals(List.of(response.getSiteFileId(), secondResponse.getSiteFileId()), secondResponse.getGallerySiteFileIds());
        assertEquals(secondResponse.getGallerySiteFileIds(), fileIngestService.getGalleryAcls(response.getGalleryId())
            .getSiteFileIds());
        assertTrue(fileIngestService.getSiteFileAcls(secondResponse.getSiteFileId())
            .getAcls()
            .isEmpty(), "the second ingest granted nobody");

        // The source file ID (unique per site file) finds the same answer, for files published before their site file ID was recorded
        var byFileId = fileIngestService.getSiteFileAclsByFileId(FILE_ID);
        assertEquals(response.getSiteFileId(), byFileId.getSiteFileId());
        assertEquals(granteeId, byFileId.getAcls()
            .getFirst()
            .getUserId());
        assertThrows(ResponseStatusException.class, () -> fileIngestService.getSiteFileAclsByFileId(FILE_ID + 2));

        assertThrows(ResponseStatusException.class, () -> fileIngestService.getSiteFileAcls(987654321L));
        assertThrows(ResponseStatusException.class, () -> fileIngestService.getGalleryAcls(987654321L));
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
            .fileId(FILE_ID)
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

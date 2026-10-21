package fi.poltsi.vempain.admin.api.request.file;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.admin.api.response.file.FileIngestAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestGalleryAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestSiteFileAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUnitResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUserResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON contract of the ingest ACL extension consumed by the file backend: {@link FileIngestAclRequest} inside {@link FileIngestRequest}, the
 * {@link FileIngestUserResponse} list of grantable users, the {@link FileIngestResponse} of an ingest and the ACL lookups of an ingested site file
 * ({@link FileIngestSiteFileAclResponse}) and gallery ({@link FileIngestGalleryAclResponse}).
 */
class FileIngestAclContractJTC {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aclEntriesAreReadFromAndWrittenAsSnakeCase() throws Exception {
        String json = """
                {
                  "file_name": "img_001.png",
                  "mime_type": "image/png",
                  "sha256sum": "abc",
                  "acls": [
                    {"user_id": 12, "read_privilege": true, "create_privilege": false, "modify_privilege": true, "delete_privilege": false}
                  ]
                }
                """;

        var request = objectMapper.readValue(json, FileIngestRequest.class);

        assertEquals(1, request.getAcls()
            .size());
        var acl = request.getAcls()
            .getFirst();
        assertEquals(12L, acl.getUserId());
        assertTrue(acl.isReadPrivilege());
        assertFalse(acl.isCreatePrivilege());
        assertTrue(acl.isModifyPrivilege());
        assertFalse(acl.isDeletePrivilege());

        var tree = objectMapper.readTree(objectMapper.writeValueAsString(request));
        var keys = new java.util.HashSet<>(tree.path("acls")
            .get(0)
            .propertyNames());
        assertEquals(Set.of("user_id", "unit_id", "read_privilege", "create_privilege", "modify_privilege", "delete_privilege"), keys);
    }

    @Test
    void aUnitEntryIsReadFromSnakeCase() throws Exception {
        var acl = objectMapper.readValue("{\"unit_id\": 3, \"read_privilege\": true}", FileIngestAclRequest.class);

        assertEquals(null, acl.getUserId());
        assertEquals(3L, acl.getUnitId());
        assertTrue(acl.isUserOrUnit());
        assertTrue(acl.isReadPrivilege());
        assertFalse(objectMapper.writeValueAsString(acl)
            .contains("user_or_unit"));
    }

    @Test
    void unitResponseExposesOnlyTheReducedSnakeCaseFields() throws Exception {
        var response = FileIngestUnitResponse.builder()
            .id(3L)
            .name("Editors")
            .description("Site editors")
            .build();

        var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
        var keys = new java.util.HashSet<>(tree.propertyNames());

        assertEquals(Set.of("id", "name", "description"), keys);
        assertEquals("Editors",
                objectMapper.readValue("{\"id\": 3, \"name\": \"Editors\", \"description\": null, \"user_ids\": [1]}", FileIngestUnitResponse.class)
                    .getName());
    }

    @Test
    void aclsAreOptionalAndDefaultToNull() throws Exception {
        var request = objectMapper.readValue("{\"file_name\": \"a.png\", \"mime_type\": \"image/png\", \"sha256sum\": \"abc\"}", FileIngestRequest.class);
        assertEquals(null, request.getAcls());
    }

    @Test
    void userResponseExposesOnlyTheReducedSnakeCaseFields() throws Exception {
        var response = FileIngestUserResponse.builder()
            .id(12L)
            .loginName("arnold")
            .name("Arnold Dunkelswetter")
            .nick("Ahnold")
            .build();

        var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
        var keys = new java.util.HashSet<>(tree.propertyNames());

        assertEquals(Set.of("id", "login_name", "name", "nick"), keys);
        assertEquals("arnold",
                objectMapper
                    .readValue("{\"id\": 12, \"login_name\": \"arnold\", \"name\": \"A\", \"nick\": \"B\", \"email\": \"x\"}", FileIngestUserResponse.class)
                    .getLoginName());
    }

    @Test
    void ingestResponseWritesSnakeCaseAndStillReadsTheFormerCamelCaseKeys() throws Exception {
        var response = FileIngestResponse.builder()
            .galleryId(7L)
            .siteFileId(42L)
            .updated(true)
            .gallerySiteFileIds(List.of(41L, 42L))
            .build();

        var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertEquals(Set.of("gallery_id", "site_file_id", "updated", "gallery_site_file_ids"), new java.util.HashSet<>(tree.propertyNames()));
        assertEquals(42L, tree.path("site_file_id")
            .asLong());
        assertEquals(2, tree.path("gallery_site_file_ids")
            .size());

        // A file backend of this release still understands the answer of an admin backend that wrote camelCase
        var legacy = objectMapper.readValue("{\"galleryId\": 7, \"siteFileId\": 42, \"updated\": false}", FileIngestResponse.class);
        assertEquals(7L, legacy.getGalleryId());
        assertEquals(42L, legacy.getSiteFileId());
        assertFalse(legacy.isUpdated());
        assertEquals(null, legacy.getGallerySiteFileIds());

        var current = objectMapper.readValue(objectMapper.writeValueAsString(response), FileIngestResponse.class);
        assertEquals(response, current);
    }

    @Test
    void siteFileAclResponseListsTheGranteesInTheShapeOfTheIngestRequest() throws Exception {
        var response = FileIngestSiteFileAclResponse.builder()
            .siteFileId(42L)
            .fileId(678L)
            .acls(List.of(FileIngestAclResponse.builder()
                .userId(12L)
                .readPrivilege(true)
                .modifyPrivilege(true)
                .build(),
                    FileIngestAclResponse.builder()
                        .unitId(3L)
                        .readPrivilege(true)
                        .build()))
            .build();

        var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertEquals(Set.of("site_file_id", "file_id", "acls"), new java.util.HashSet<>(tree.propertyNames()));
        var row = tree.path("acls")
            .get(0);
        assertEquals(Set.of("user_id", "unit_id", "read_privilege", "create_privilege", "modify_privilege", "delete_privilege"),
                new java.util.HashSet<>(row.propertyNames()));
        // The rows can be sent back unchanged as the acls of the next ingest request
        var resent = objectMapper.readValue(row.toString(), FileIngestAclRequest.class);
        assertEquals(12L, resent.getUserId());
        assertTrue(resent.isUserOrUnit());
        assertTrue(resent.isModifyPrivilege());
        assertEquals(response, objectMapper.readValue(objectMapper.writeValueAsString(response), FileIngestSiteFileAclResponse.class));
    }

    @Test
    void galleryAclResponseCarriesTheLinkedSiteFilesAndTheGrantees() throws Exception {
        var response = FileIngestGalleryAclResponse.builder()
            .galleryId(7L)
            .shortname("Summer")
            .siteFileIds(List.of(41L, 42L))
            .acls(List.of())
            .build();

        var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertEquals(Set.of("gallery_id", "shortname", "site_file_ids", "acls"), new java.util.HashSet<>(tree.propertyNames()));
        assertEquals(41L, tree.path("site_file_ids")
            .get(0)
            .asLong());
        assertEquals(response, objectMapper.readValue(objectMapper.writeValueAsString(response), FileIngestGalleryAclResponse.class));
    }
}

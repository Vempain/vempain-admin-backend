package fi.poltsi.vempain.admin.api.request.file;

import fi.poltsi.vempain.admin.api.response.file.FileIngestUserResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON contract of the ingest ACL extension consumed by the file backend: {@link FileIngestAclRequest} inside {@link FileIngestRequest}
 * and the {@link FileIngestUserResponse} list of grantable users.
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
		assertEquals(Set.of("user_id", "read_privilege", "create_privilege", "modify_privilege", "delete_privilege"), keys);
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
		assertEquals("arnold", objectMapper.readValue("{\"id\": 12, \"login_name\": \"arnold\", \"name\": \"A\", \"nick\": \"B\", \"email\": \"x\"}",
													  FileIngestUserResponse.class)
		                                   .getLoginName());
	}
}

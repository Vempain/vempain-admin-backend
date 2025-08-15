package fi.poltsi.vempain.admin.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class LayoutRTC {
	@Autowired
	private MockMvc mockMvc;

	final static private String payload = """
			{
			  "id": 0,
			  "layout_name": "New layout-111",
			  "structure": "asdasdasd",
			  "locked": 0,
			  "acls": [
			    {
			      "permission_id": 32634545,
			      "acl_id": 32500773,
			      "userAccount": 1,
			      "unit": null,
			      "read_privilege": 0,
			      "modify_privilege": 0,
			      "create_privilege": "1",
			      "delete_privilege": 0
			    }
			  ],
			  "creator": 0,
			  "created": "2021-06-11T14:26:07.983Z",
			  "modifier": 0,
			  "modified": "2021-06-11T14:26:07.983Z"
			}
			""";

	@Disabled("Somehow the JSON becomes mangled in the process")
	@Test
	void addLayout() throws Exception {
		MvcResult result = mockMvc.perform(post("/content-management/layouts").contentType(MediaType.APPLICATION_JSON).content(payload))
								  .andExpect(status().isOk())
								  .andExpect(content().contentType(MediaType.APPLICATION_JSON))
								  .andReturn();
		assertNotNull(result);
		assertNotNull(result.getResponse());
		ObjectMapper objectMapper = new ObjectMapper();
		LayoutResponse layoutResponse = objectMapper.readValue(result.getResponse().getContentAsString(), LayoutResponse.class);
		assertNotNull(layoutResponse);
		assertNotNull(layoutResponse.getAcls());
		List<AclResponse> aclResponses = layoutResponse.getAcls();
		assertFalse(aclResponses.isEmpty());
		assertEquals(1, aclResponses.size());
		assertTrue(aclResponses.getFirst().isReadPrivilege());
		assertTrue(aclResponses.getFirst().isModifyPrivilege());
		assertFalse(aclResponses.getFirst().isCreatePrivilege());
		assertFalse(aclResponses.getFirst().isDeletePrivilege());
		assertEquals(1, aclResponses.getFirst().getUser());
		assertNull(aclResponses.getFirst().getUnit());
	}
}

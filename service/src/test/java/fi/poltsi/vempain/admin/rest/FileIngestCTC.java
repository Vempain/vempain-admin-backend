package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for the service-to-service user listing of {@code FileIngestAPI}: the file backend fetches it to let its
 * users pick additional ACL grantees before publishing.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileIngestCTC {
	@Autowired
	private MockMvc  mockMvc;
	@Autowired
	private JwtUtils jwtUtils;

	@Test
	void listsTheGrantableUsersWithReducedFieldsOnly() throws Exception {
		mockMvc.perform(get("/content-management/file/site-file/users").header("Authorization", adminBearerToken()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$[?(@.login_name == 'admin')]", hasSize(1)))
			   .andExpect(jsonPath("$[?(@.login_name == 'admin')].id").value(1))
			   .andExpect(jsonPath("$[0].name").exists())
			   .andExpect(jsonPath("$[0].nick").exists())
			   .andExpect(jsonPath("$[0].email").doesNotExist())
			   .andExpect(jsonPath("$[0].password").doesNotExist())
			   .andExpect(jsonPath("$[0].birthday").doesNotExist());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/content-management/file/site-file/users"))
			   .andExpect(status().isUnauthorized());
	}

	private String adminBearerToken() {
		return "Bearer " + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
								   .getTokenString();
	}
}

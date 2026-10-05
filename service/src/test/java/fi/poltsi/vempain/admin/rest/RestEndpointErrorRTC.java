package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RestEndpointErrorRTC extends AbstractITCTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtUtils jwtUtils;

	@Test
	void unknownAuthenticatedRouteReturnsNotFoundWithProblemDetails() throws Exception {
		mockMvc.perform(get("/schedule-management/does-not-exist")
								.header("Authorization", adminBearerToken())
								.accept(MediaType.APPLICATION_JSON))
			   .andExpect(status().isNotFound());
	}

	private String adminBearerToken() {
		return "Bearer " + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
								   .getTokenString();
	}
}

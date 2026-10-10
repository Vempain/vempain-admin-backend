package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.ApiTokenRequest;
import fi.poltsi.vempain.admin.repository.ApiTokenRepository;
import fi.poltsi.vempain.admin.security.ApiTokenAuthenticationFilter;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for the service-to-service API tokens: management through {@code /admin-management/api-tokens} and the
 * {@code X-Vempain-Api-Token} authentication of the endpoints the file backend calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTokenCTC {
	@Autowired
	private MockMvc            mockMvc;
	@Autowired
	private ObjectMapper       objectMapper;
	@Autowired
	private JwtUtils           jwtUtils;
	@Autowired
	private ApiTokenRepository apiTokenRepository;

	@AfterEach
	void cleanUp() {
		apiTokenRepository.deleteAll();
	}

	private String adminBearerToken() {
		return "Bearer " + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
								   .getTokenString();
	}

	private static RequestPostProcessor from(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	private String createToken(String network, Instant expiresAt) throws Exception {
		var body = objectMapper.writeValueAsString(ApiTokenRequest.builder()
																  .description("file backend")
																  .network(network)
																  .expiresAt(expiresAt)
																  .build());
		var json = mockMvc.perform(post("/admin-management/api-tokens").header("Authorization", adminBearerToken())
																	   .contentType(MediaType.APPLICATION_JSON)
																	   .content(body))
						  .andExpect(status().isOk())
						  .andExpect(jsonPath("$.token", startsWith("vat_")))
						  .andExpect(jsonPath("$.api_token.network").value(network.contains("/") ? network : network + "/32"))
						  .andExpect(jsonPath("$.api_token.owner_user_id").value(1))
						  .andReturn()
						  .getResponse()
						  .getContentAsString();
		return objectMapper.readTree(json)
						   .path("token")
						   .asText();
	}

	@Test
	void tokensAreCreatedListedWithoutTheSecretAndDeleted() throws Exception {
		var token = createToken("10.1.2.0/24", Instant.now()
													  .plus(7, ChronoUnit.DAYS));

		var list = mockMvc.perform(get("/admin-management/api-tokens").header("Authorization", adminBearerToken()))
						  .andExpect(status().isOk())
						  .andExpect(jsonPath("$", hasSize(1)))
						  .andExpect(jsonPath("$[0].token_prefix").value(token.substring(0, 12)))
						  .andExpect(jsonPath("$[0].description").value("file backend"))
						  .andExpect(jsonPath("$[0].expired").value(false))
						  .andReturn()
						  .getResponse()
						  .getContentAsString();
		assertFalse(list.contains(token), "the token string is never listed");
		assertTrue(apiTokenRepository.findAll()
									 .stream()
									 .noneMatch(stored -> stored.getTokenHash()
																.equals(token)), "only the hash is stored");

		var id = objectMapper.readTree(list)
							 .get(0)
							 .path("id")
							 .asLong();
		mockMvc.perform(delete("/admin-management/api-tokens/" + id).header("Authorization", adminBearerToken()))
			   .andExpect(status().isNoContent());
		mockMvc.perform(delete("/admin-management/api-tokens/" + id).header("Authorization", adminBearerToken()))
			   .andExpect(status().isNotFound());
		mockMvc.perform(get("/admin-management/api-tokens"))
			   .andExpect(status().isUnauthorized());
	}

	@Test
	void creationIsValidated() throws Exception {
		var past = objectMapper.writeValueAsString(ApiTokenRequest.builder()
																  .description("x")
																  .network("10.1.2.3")
																  .expiresAt(Instant.now()
																					.minus(1, ChronoUnit.DAYS))
																  .build());
		mockMvc.perform(post("/admin-management/api-tokens").header("Authorization", adminBearerToken())
															.contentType(MediaType.APPLICATION_JSON)
															.content(past))
			   .andExpect(status().isBadRequest());

		var badNetwork = objectMapper.writeValueAsString(ApiTokenRequest.builder()
																		.description("x")
																		.network("file-backend.local")
																		.expiresAt(Instant.now()
																						  .plus(1, ChronoUnit.DAYS))
																		.build());
		mockMvc.perform(post("/admin-management/api-tokens").header("Authorization", adminBearerToken())
															.contentType(MediaType.APPLICATION_JSON)
															.content(badNetwork))
			   .andExpect(status().isBadRequest());
	}

	@Test
	void tokenAuthenticatesServiceEndpointsOnlyFromItsNetwork() throws Exception {
		var token = createToken("10.1.2.0/24", Instant.now()
													  .plus(7, ChronoUnit.DAYS));

		// The grantable user listing is one of the service-to-service endpoints
		mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, token)
																	   .with(from("10.1.2.44")))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$[?(@.login_name == 'admin')]", hasSize(1)));
		// Outside the network, expired or unknown tokens are refused outright, never silently ignored
		mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, token)
																	   .with(from("10.1.3.44")))
			   .andExpect(status().isUnauthorized());
		mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, "vat_not-a-token")
																	   .with(from("10.1.2.44")))
			   .andExpect(status().isUnauthorized());
		// A token can not administer the service: anything outside the service-to-service endpoints is forbidden
		mockMvc.perform(get("/content-management/pages").header(Constants.API_TOKEN_HEADER, token)
														.with(from("10.1.2.44")))
			   .andExpect(status().isForbidden());
		mockMvc.perform(get("/admin-management/api-tokens").header(Constants.API_TOKEN_HEADER, token)
														   .with(from("10.1.2.44")))
			   .andExpect(status().isForbidden());
		// Last use is recorded
		assertTrue(apiTokenRepository.findAll()
									 .getFirst()
									 .getLastUsed() != null);

		// Deleting the token revokes it immediately
		var id = apiTokenRepository.findAll()
								   .getFirst()
								   .getId();
		mockMvc.perform(delete("/admin-management/api-tokens/" + id).header("Authorization", adminBearerToken()))
			   .andExpect(status().isNoContent());
		mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, token)
																	   .with(from("10.1.2.44")))
			   .andExpect(status().isUnauthorized());
	}

	@Test
	void ipv6NetworksWork() throws Exception {
		var token = createToken("2001:db8::/32", Instant.now()
														.plus(1, ChronoUnit.DAYS));
		mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, token)
																	   .with(from("2001:db8:1::7")))
			   .andExpect(status().isOk());
		mockMvc.perform(get("/content-management/file/site-file/users").header(Constants.API_TOKEN_HEADER, token)
																	   .with(from("2001:db9::7")))
			   .andExpect(status().isUnauthorized());
		assertTrue(ApiTokenAuthenticationFilter.isAllowed("POST", "/content-management/file/site-file"));
		assertTrue(ApiTokenAuthenticationFilter.isAllowed("DELETE", "/content-management/data/music_library"));
		assertFalse(ApiTokenAuthenticationFilter.isAllowed("POST", "/content-management/data/music_library/publish"));
		assertFalse(ApiTokenAuthenticationFilter.isAllowed("GET", "/content-management/users"));
	}

	@Test
	void defaultNetworkIsSuggestedFromThePrivateNetworkOrConfiguration() throws Exception {
		// The test properties declare a private network, so the suggestion comes from the host's private interfaces (if it has any)
		var json = mockMvc.perform(get("/admin-management/api-tokens/default-network").header("Authorization", adminBearerToken()))
						  .andExpect(status().isOk())
						  .andReturn()
						  .getResponse()
						  .getContentAsString();
		var tree = objectMapper.readTree(json);
		var candidates = tree.path("candidates");
		if (candidates.isEmpty()) {
			assertTrue(tree.path("network")
						   .isNull());
			assertTrue("NONE".equals(tree.path("source")
										 .asText()));
		} else {
			assertTrue("PRIVATE_NETWORK".equals(tree.path("source")
													.asText()));
			assertTrue(tree.path("network")
						   .asText()
						   .equals(candidates.get(0)
											 .path("network")
											 .asText()));
			assertTrue(candidates.get(0)
								 .path("interface_name")
								 .asText()
								 .length() > 0);
		}
		mockMvc.perform(get("/admin-management/api-tokens/default-network"))
			   .andExpect(status().isUnauthorized());
	}
}

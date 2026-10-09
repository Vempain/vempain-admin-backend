package fi.poltsi.vempain.admin.api;

import fi.poltsi.vempain.admin.api.request.ApiTokenRequest;
import fi.poltsi.vempain.admin.api.response.ApiTokenCreatedResponse;
import fi.poltsi.vempain.admin.api.response.ApiTokenResponse;
import fi.poltsi.vempain.admin.rest.ApiTokenAPI;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JSON contract of the service-to-service API token management.
 */
class ApiTokenContractJTC {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void pathAndHeaderRemainStable() {
		assertEquals("/admin-management/api-tokens", ApiTokenAPI.MAIN_PATH);
		assertEquals("X-Vempain-Api-Token", Constants.API_TOKEN_HEADER);
	}

	@Test
	void requestAndResponsesAreSnakeCase() throws Exception {
		var request = objectMapper.readValue("{\"description\": \"d\", \"network\": \"10.0.0.0/8\", \"expires_at\": \"2027-01-01T00:00:00Z\"}",
											 ApiTokenRequest.class);
		assertEquals("10.0.0.0/8", request.getNetwork());
		assertEquals(Instant.parse("2027-01-01T00:00:00Z"), request.getExpiresAt());

		var response = ApiTokenResponse.builder()
		                               .id(1)
		                               .tokenPrefix("vat_abc")
		                               .description("d")
		                               .network("10.0.0.0/8")
									   .expiresAt(Instant.parse("2027-01-01T00:00:00Z"))
		                               .ownerUserId(1)
		                               .created(Instant.now())
		                               .build();
		var keys = new HashSet<>(objectMapper.readTree(objectMapper.writeValueAsString(response))
		                                     .propertyNames());
		assertEquals(Set.of("id", "token_prefix", "description", "network", "expires_at", "owner_user_id", "created", "last_used", "expired"), keys);

		var created = ApiTokenCreatedResponse.builder()
		                                     .token("vat_secret")
		                                     .apiToken(response)
		                                     .build();
		var createdKeys = new HashSet<>(objectMapper.readTree(objectMapper.writeValueAsString(created))
		                                            .propertyNames());
		assertEquals(Set.of("token", "api_token"), createdKeys);
	}

	@Test
	void networkSuggestionIsSnakeCase() throws Exception {
		var response = fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.builder()
																				   .network("10.0.9.0/24")
																				   .source(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Source.PRIVATE_NETWORK)
																				   .candidates(java.util.List.of(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Candidate.builder()
																																													   .network("10.0.9.0/24")
																																													   .interfaceName("eth1")
																																													   .address("10.0.9.5")
																																													   .build()))
																				   .build();
		var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
		assertEquals(Set.of("network", "source", "candidates"), new HashSet<>(tree.propertyNames()));
		assertEquals(Set.of("network", "interface_name", "address"), new HashSet<>(tree.path("candidates")
		                                                                               .get(0)
		                                                                               .propertyNames()));
		assertEquals("PRIVATE_NETWORK", tree.path("source")
		                                    .asText());
	}
}

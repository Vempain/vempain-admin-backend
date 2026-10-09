package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.request.ApiTokenRequest;
import fi.poltsi.vempain.admin.entity.ApiToken;
import fi.poltsi.vempain.admin.repository.ApiTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceUTC {
	@Mock
	private ApiTokenRepository apiTokenRepository;
	@InjectMocks
	private ApiTokenService    apiTokenService;

	private static ApiTokenRequest request(String network, Instant expiresAt) {
		return ApiTokenRequest.builder()
							  .description("file backend")
							  .network(network)
							  .expiresAt(expiresAt)
							  .build();
	}

	@Test
	void createStoresOnlyTheHashAndReturnsTheTokenOnce() {
		when(apiTokenRepository.save(any(ApiToken.class))).thenAnswer(invocation -> {
			ApiToken token = invocation.getArgument(0);
			token.setId(5L);
			return token;
		});

		var created = apiTokenService.create(request("10.1.2.3", Instant.now()
																		.plus(30, ChronoUnit.DAYS)), 1L);

		assertTrue(created.getToken()
						  .startsWith("vat_"));
		assertTrue(created.getToken()
						  .length() > 40);
		var captor = ArgumentCaptor.forClass(ApiToken.class);
		verify(apiTokenRepository).save(captor.capture());
		var stored = captor.getValue();
		assertEquals(ApiTokenService.hash(created.getToken()), stored.getTokenHash());
		assertNotEquals(created.getToken(), stored.getTokenHash());
		assertEquals(64, stored.getTokenHash()
							   .length());
		assertEquals("10.1.2.3/32", stored.getNetwork(), "a single address is stored as a host network");
		assertEquals(1L, stored.getOwnerUserId());
		assertEquals(created.getToken()
							.substring(0, 12), stored.getTokenPrefix());
		assertEquals(5L, created.getApiToken()
								.getId());
		assertFalse(created.getApiToken()
						   .isExpired());
	}

	@Test
	void createRejectsPastExpiryBadNetworkAndBlankDescription() {
		var future = Instant.now()
							.plus(1, ChronoUnit.DAYS);
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
														  () -> apiTokenService.create(request("10.1.2.3", Instant.now()
																												  .minus(1, ChronoUnit.MINUTES)), 1L)).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
														  () -> apiTokenService.create(request("10.1.2.3", null), 1L)).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
														  () -> apiTokenService.create(request("vempain-file.example.com", future), 1L)).getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
														  () -> apiTokenService.create(request("10.0.0.0/40", future), 1L)).getStatusCode());
		var blank = request("10.1.2.3", future);
		blank.setDescription("  ");
		assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> apiTokenService.create(blank, 1L)).getStatusCode());
		verify(apiTokenRepository, never()).save(any());
	}

	@Test
	void authenticateAcceptsOnlyLiveTokensFromTheirNetwork() {
		var token = "vat_abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
		var stored = ApiToken.builder()
							 .id(7L)
							 .tokenHash(ApiTokenService.hash(token))
							 .tokenPrefix("vat_abcdefgh")
							 .network("10.1.2.0/24")
							 .expiresAt(Instant.now()
											   .plus(1, ChronoUnit.HOURS))
							 .ownerUserId(1L)
							 .build();
		when(apiTokenRepository.findByTokenHash(ApiTokenService.hash(token))).thenReturn(Optional.of(stored));
		when(apiTokenRepository.save(any(ApiToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertTrue(apiTokenService.authenticate(token, "10.1.2.77")
								  .isPresent());
		assertNotNull(stored.getLastUsed(), "a successful use is recorded");
		assertTrue(apiTokenService.authenticate(token, "10.1.3.1")
								  .isEmpty(), "outside the network");
		assertTrue(apiTokenService.authenticate(token, "2001:db8::1")
								  .isEmpty(), "IPv6 client against an IPv4 network");

		stored.setExpiresAt(Instant.now()
								   .minus(1, ChronoUnit.SECONDS));
		assertTrue(apiTokenService.authenticate(token, "10.1.2.77")
								  .isEmpty(), "expired");
	}

	@Test
	void authenticateRejectsUnknownAndMalformedTokensWithoutQueryingNonsense() {
		when(apiTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertTrue(apiTokenService.authenticate("vat_unknown", "10.1.2.3")
								  .isEmpty());
		assertTrue(apiTokenService.authenticate(null, "10.1.2.3")
								  .isEmpty());
		assertTrue(apiTokenService.authenticate("", "10.1.2.3")
								  .isEmpty());
		assertTrue(apiTokenService.authenticate("Bearer eyJ...", "10.1.2.3")
								  .isEmpty(), "a JWT is never looked up as a token");
		verify(apiTokenRepository, never()).save(any());
	}

	@Test
	void deleteRemovesTheTokenOrAnswersNotFound() {
		var stored = ApiToken.builder()
		                     .id(3L)
		                     .tokenPrefix("vat_x")
		                     .description("d")
		                     .build();
		when(apiTokenRepository.findById(3L)).thenReturn(Optional.of(stored));
		when(apiTokenRepository.findById(4L)).thenReturn(Optional.empty());

		apiTokenService.delete(3L);
		verify(apiTokenRepository).delete(stored);
		assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> apiTokenService.delete(4L)).getStatusCode());
	}

	@Test
	void defaultNetworkFollowsTheConfigurationAndTheDetectedPrivateNetworks() {
		// Nothing configured, no private network declared: no suggestion
		var none = apiTokenService.defaultNetwork();
		assertEquals(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Source.NONE, none.getSource());
		assertTrue(none.getNetwork() == null && none.getCandidates()
													.isEmpty());

		// A configured network wins and is normalised
		org.springframework.test.util.ReflectionTestUtils.setField(apiTokenService, "configuredDefaultNetwork", " 10.20.0.0/16 ");
		var configured = apiTokenService.defaultNetwork();
		assertEquals(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Source.CONFIGURED, configured.getSource());
		assertEquals("10.20.0.0/16", configured.getNetwork());

		// An invalid configured network is ignored
		org.springframework.test.util.ReflectionTestUtils.setField(apiTokenService, "configuredDefaultNetwork", "not-a-network");
		assertEquals(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Source.NONE, apiTokenService.defaultNetwork()
																											  .getSource());

		// With a private network declared the candidates are the host's private networks (whatever the test host has)
		org.springframework.test.util.ReflectionTestUtils.setField(apiTokenService, "configuredDefaultNetwork", "");
		org.springframework.test.util.ReflectionTestUtils.setField(apiTokenService, "privateNetwork", true);
		var detected = apiTokenService.defaultNetwork();
		if (detected.getCandidates()
					.isEmpty()) {
			assertEquals(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Source.NONE, detected.getSource());
		} else {
			assertEquals(fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse.Source.PRIVATE_NETWORK, detected.getSource());
			assertEquals(detected.getCandidates()
								 .getFirst()
								 .getNetwork(), detected.getNetwork());
		}
	}
}

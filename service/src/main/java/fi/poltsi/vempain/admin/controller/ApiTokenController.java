package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.request.ApiTokenRequest;
import fi.poltsi.vempain.admin.api.response.ApiTokenCreatedResponse;
import fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse;
import fi.poltsi.vempain.admin.api.response.ApiTokenResponse;
import fi.poltsi.vempain.admin.rest.ApiTokenAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.ApiTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ApiTokenController implements ApiTokenAPI {
	private final ApiTokenService apiTokenService;
	private final AccessService   accessService;

	@Override
	public ResponseEntity<List<ApiTokenResponse>> getApiTokens() {
		accessService.checkAdminAccess();
		return ResponseEntity.ok(apiTokenService.findAll());
	}

	@Override
	public ResponseEntity<ApiTokenNetworkResponse> getDefaultNetwork() {
		accessService.checkAdminAccess();
		return ResponseEntity.ok(apiTokenService.defaultNetwork());
	}

	@Override
	public ResponseEntity<ApiTokenCreatedResponse> createApiToken(ApiTokenRequest request) {
		accessService.checkAdminAccess();
		return ResponseEntity.ok(apiTokenService.create(request, accessService.getValidUserId()));
	}

	@Override
	public ResponseEntity<Void> deleteApiToken(long tokenId) {
		accessService.checkAdminAccess();
		apiTokenService.delete(tokenId);
		return ResponseEntity.noContent()
							 .build();
	}
}

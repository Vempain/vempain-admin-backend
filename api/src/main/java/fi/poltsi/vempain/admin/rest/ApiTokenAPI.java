package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.ApiTokenRequest;
import fi.poltsi.vempain.admin.api.response.ApiTokenCreatedResponse;
import fi.poltsi.vempain.admin.api.response.ApiTokenNetworkResponse;
import fi.poltsi.vempain.admin.api.response.ApiTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Management of the service-to-service API tokens other services (the file backend) present in the {@link Constants#API_TOKEN_HEADER}
 * header instead of logging in. Administrator ACL required; the token endpoints themselves can never be called with a token.
 */
@Tag(name = "ApiTokenAPI", description = "Service-to-service API tokens")
public interface ApiTokenAPI {
	String MAIN_PATH = Constants.REST_ADMIN_PREFIX + "/api-tokens";

	@Operation(summary = "List the API tokens", description = "Every token without its secret, newest first", tags = "ApiTokenAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List of tokens",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = ApiTokenResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<ApiTokenResponse>> getApiTokens();

	@Operation(summary = "Suggested network for a new token",
			   description = "The private network the admin backend shares with the calling services (detected from its own interfaces when "
							 + "vempain.admin.api-token.private-network is true, or vempain.admin.api-token.default-network), plus every private network "
							 + "it is attached to. Source NONE means the administrator has to enter the network by hand.", tags = "ApiTokenAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Suggestion",
										content = {@Content(schema = @Schema(implementation = ApiTokenNetworkResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/default-network", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ApiTokenNetworkResponse> getDefaultNetwork();

	@Operation(summary = "Create an API token",
			   description = "Generates a random token bound to the creating administrator, the given network and expiry. The token string is "
							 + "returned once and only its hash is stored.", tags = "ApiTokenAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Token created",
										content = {@Content(schema = @Schema(implementation = ApiTokenCreatedResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Blank description, expiry not in the future or an invalid network",
										content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ApiTokenCreatedResponse> createApiToken(@Valid @RequestBody ApiTokenRequest request);

	@Operation(summary = "Delete (revoke) an API token", description = "The token stops working immediately", tags = "ApiTokenAPI")
	@Parameter(name = "token_id", example = "3", description = "ID of the token", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Token deleted", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "403", description = "Administrator access is required", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No such token", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/{token_id}")
	ResponseEntity<Void> deleteApiToken(@PathVariable("token_id") long tokenId);
}

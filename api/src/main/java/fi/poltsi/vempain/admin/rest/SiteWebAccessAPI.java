package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.site.request.WebSiteAclRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteUserRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclUsersResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResourcesResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "SiteWebAccess", description = "Manage site web users and their access control")
@SecurityRequirement(name = "Bearer Authentication")
public interface SiteWebAccessAPI {
	String MAIN_PATH = Constants.REST_ADMIN_PREFIX + "/site-access";

	@Operation(summary = "List all site web users", description = "Returns a list of all site web users")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebSiteUserResponse.class)),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = MAIN_PATH + "/users", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<WebSiteUserResponse>> getAllUsers();

	@Operation(summary = "Get a site web user by ID", description = "Returns details of a specific site web user")
	@Parameter(name = "userId", description = "Site web user ID", required = true, example = "7")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved user",
						 content = @Content(schema = @Schema(implementation = WebSiteUserResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = MAIN_PATH + "/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteUserResponse> getUserById(@PathVariable("userId") Long userId);

	@Operation(summary = "Create a new site web user", description = "Creates a new site web user with username and password")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User details including username and password", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User created successfully",
						 content = @Content(schema = @Schema(implementation = WebSiteUserResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "409", description = "Username already exists", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping(value = MAIN_PATH + "/users", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteUserResponse> createUser(@Valid @RequestBody WebSiteUserRequest request);

	@Operation(summary = "Update a site web user", description = "Updates username and/or password of an existing site web user")
	@Parameter(name = "userId", description = "Site web user ID to update", required = true, example = "7")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user details", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User updated successfully",
						 content = @Content(schema = @Schema(implementation = WebSiteUserResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "Username already exists", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping(value = MAIN_PATH + "/users/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteUserResponse> updateUser(@PathVariable("userId") Long userId, @Valid @RequestBody WebSiteUserRequest request);

	@Operation(summary = "Delete a site web user", description = "Deletes a site web user and all associated ACL entries")
	@Parameter(name = "userId", description = "Site web user ID to delete", required = true, example = "7")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "User deleted successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@DeleteMapping(value = MAIN_PATH + "/users/{userId}")
	ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId);

	@Operation(summary = "List all site ACL entries", description = "Returns all ACL assignments")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebSiteAclResponse.class)),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = MAIN_PATH + "/acls", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<WebSiteAclResponse>> getAllAcls();

	@Operation(summary = "Get users for an ACL ID", description = "Returns all users who have access via a specific ACL ID")
	@Parameter(name = "aclId", description = "Site ACL ID", required = true, example = "42")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved users",
						 content = @Content(schema = @Schema(implementation = WebSiteAclUsersResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = MAIN_PATH + "/acls/{aclId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteAclUsersResponse> getUsersByAclId(@PathVariable("aclId") Long aclId);

	@Operation(summary = "Get resources for a user", description = "Returns all resources accessible to a specific user")
	@Parameter(name = "userId", description = "Site web user ID", required = true, example = "7")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved resources",
						 content = @Content(schema = @Schema(implementation = WebSiteUserResourcesResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = MAIN_PATH + "/users/{userId}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteUserResourcesResponse> getResourcesByUserId(@PathVariable("userId") Long userId);

	@Operation(summary = "Create a site ACL entry", description = "Links a site web user to an ACL ID")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ACL assignment details", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "ACL entry created successfully",
						 content = @Content(schema = @Schema(implementation = WebSiteAclResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "ACL assignment already exists", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PostMapping(value = MAIN_PATH + "/acls", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteAclResponse> createAcl(@Valid @RequestBody WebSiteAclRequest request);

	@Operation(summary = "Delete a site ACL entry", description = "Removes a specific ACL assignment")
	@Parameter(name = "id", description = "Site ACL entry ID to delete", required = true, example = "12")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "ACL entry deleted successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "ACL entry not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@DeleteMapping(value = MAIN_PATH + "/acls/{id}")
	ResponseEntity<Void> deleteAcl(@PathVariable("id") Long id);
}


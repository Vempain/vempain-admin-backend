package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteAclRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteUserRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclUsersResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteConfigurationResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourcePageResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
import fi.poltsi.vempain.file.api.FileTypeEnum;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "SiteWebAccess", description = "Manage site web users and their access control")
@SecurityRequirement(name = "Bearer Authentication")
public interface WebSiteManagementAPI {
	String MAIN_PATH         = Constants.REST_ADMIN_PREFIX + "/site";
	String USER_ADMIN_PATH   = MAIN_PATH + "/users";
	String ACL_ADMIN_PATH    = MAIN_PATH + "/acls";
	String CONFIG_ADMIN_PATH = MAIN_PATH + "/config";

	// ////////////////////////////// Site Web Users APIs //////////////////////////////
	@Operation(summary = "List all site web users", description = "Returns a list of all site web users")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebSiteUserResponse.class)),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = USER_ADMIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
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
	@GetMapping(value = USER_ADMIN_PATH + "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
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
	@PostMapping(value = USER_ADMIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
	@PutMapping(value = USER_ADMIN_PATH + "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteUserResponse> updateUser(@PathVariable("userId") Long userId, @Valid @RequestBody WebSiteUserRequest request);

	@Operation(summary = "Delete a site web user", description = "Deletes a site web user and all associated ACL entries")
	@Parameter(name = "userId", description = "Site web user ID to delete", required = true, example = "7")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "User deleted successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@DeleteMapping(value = USER_ADMIN_PATH + "/{userId}")
	ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId);

	@Operation(summary = "Get resources for a user", description = "Returns all resources accessible to a specific user")
	@Parameter(name = "userId", description = "Site web user ID", required = true, example = "7")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved resources",
						 content = @Content(schema = @Schema(implementation = WebSiteUserResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "User not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = USER_ADMIN_PATH + "/{userId}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteUserResponse> getResourcesByUserId(@PathVariable("userId") Long userId);

	// ////////////////////////////// Site ACL APIs //////////////////////////////
	@Operation(summary = "List all site ACL entries", description = "Returns all ACL assignments")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebSiteAclResponse.class)),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = ACL_ADMIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
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
	@GetMapping(value = ACL_ADMIN_PATH + "/{aclId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteAclUsersResponse> getUsersByAclId(@PathVariable("aclId") Long aclId);

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
	@PostMapping(value = ACL_ADMIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteAclResponse> createAcl(@Valid @RequestBody WebSiteAclRequest request);

	@Operation(summary = "Delete a site ACL entry", description = "Removes a specific ACL assignment")
	@Parameter(name = "id", description = "Site ACL entry ID to delete", required = true, example = "12")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "ACL entry deleted successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "ACL entry not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@DeleteMapping(value = ACL_ADMIN_PATH + "/{id}")
	ResponseEntity<Void> deleteAcl(@PathVariable("id") Long id);

	// ////////////////////////////// Site Resource APIs //////////////////////////////
	@Operation(summary = "List available site resources", description = "Returns paginated list of site resources that can be linked to ACLs")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved resources",
						 content = @Content(schema = @Schema(implementation = WebSiteResourcePageResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = MAIN_PATH + "/resources", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteResourcePageResponse> getResources(@RequestParam(value = "type", required = false) WebSiteResourceEnum resourceType,
															 @RequestParam(value = "file_type", required = false) FileTypeEnum fileType,
															 @RequestParam(value = "query", required = false) String query,
															 @RequestParam(value = "acl_id", required = false) Long aclId,
															 @RequestParam(value = "sort", defaultValue = "id") String sort,
															 @RequestParam(value = "direction", defaultValue = "asc") String direction,
															 @RequestParam(value = "page", defaultValue = "0") int page,
															 @RequestParam(value = "size", defaultValue = "25") int size);

	// ////////////////////////////// Site Configuration APIs //////////////////////////////
	@Operation(summary = "List all site configuration entries", description = "Returns all configuration assignments")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list",
						 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WebSiteAclResponse.class)),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = CONFIG_ADMIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<WebSiteConfigurationResponse>> getAllSiteConfigurations();

	@Operation(summary = "Fetch existing site configuration by id", description = "Returns configuration entry for a specific key")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "404", description = "Configuration entry not found", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@GetMapping(value = CONFIG_ADMIN_PATH + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteConfigurationResponse> getSiteConfigurationById(@PathVariable("id") Long id);

	@Operation(summary = "Update existing site configuration entry", description = "Updates value of an existing configuration entry")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Configuration entry updated successfully",
						 content = @Content(schema = @Schema(implementation = WebSiteConfigurationResponse.class),
											mediaType = MediaType.APPLICATION_JSON_VALUE)),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
			@ApiResponse(responseCode = "404", description = "Configuration entry not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@PutMapping(value = CONFIG_ADMIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<WebSiteConfigurationResponse> updateSiteConfiguration(@RequestBody @Valid WebSiteConfigurationRequest request);
}

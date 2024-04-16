package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.UserRequest;
import fi.poltsi.vempain.admin.api.response.UserResponse;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600) // TODO Remove before going to production
@Tag(name = "User", description = "User API for Vempain page objects")
public interface UserAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/users";

	@Operation(summary = "Fetch list of all users", description = "Returns a list of all vempain users", tags = "User")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Returned a list of users",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No users found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<UserResponse>> getUsers();

	@Operation(summary = "Fetch a specific user by user ID", description = "Returns details of a specific user", tags = "User")
	@Parameter(name = "user_id", example = "12", description = "User ID to be fetched", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the details of a user",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{user_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UserResponse> findById(@PathVariable("user_id") Long userId);

	@Operation(summary = "Add a new user", description = "Add a new user to the system", tags = "User")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New user details, the login and email can not be empty nor null",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "User account created",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UserResponse> addUser(@Valid @RequestBody UserRequest userRequest);

	@Operation(summary = "Update a specific user", description = "Update the details of a specific user", tags = "User")
	@Parameter(name = "user_id", example = "12", description = "User ID to be updated", required = true)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user, the name can not be empty nor null", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "User details updated",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Form not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH + "/{user_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UserResponse> updateUser(@PathVariable("user_id") Long userId, @Valid @RequestBody UserRequest userRequest);
}

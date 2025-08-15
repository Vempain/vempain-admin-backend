package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
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
@Tag(name = "Unit", description = "Unit API for Vempain page objects")
public interface UnitAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/units";

	@Operation(summary = "Fetch list of all units", description = "Returns a list of all vempain units", tags = "Unit")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of units",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnitResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No units found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<UnitResponse>> getUnits();

	@Operation(summary = "Fetch a specific unit by unit ID", description = "Returns details of a specific unit", tags = "Unit")
	@Parameter(name = "unit_id", example = "12", description = "Unit ID to be fetched", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the details of a unit",
										content = {@Content(schema = @Schema(implementation = UnitResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Unit not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{unit_id}", produces =
			MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UnitResponse> findById(@PathVariable("unit_id") Long unitId);


	@Operation(summary = "Add a new unit", description = "Add a new unit to the system", tags = "User")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New unit details, the name can not be empty nor null",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Unit created",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UnitResponse> addUnit(@Valid @RequestBody UnitRequest unitRequest);

	@Operation(summary = "Update a specific unit", description = "Update the details of a specific unit", tags = "User")
	@Parameter(name = "unit_id", example = "12", description = "Unit ID to be updated", required = true)
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated unit, the name can not be empty nor null", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Unit details updated",
										content = {@Content(schema = @Schema(implementation = UserResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Form not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH + "/{unit_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<UnitResponse> updateUser(@PathVariable("unit_id") Long unitId, @Valid @RequestBody UnitRequest unitRequest);
}

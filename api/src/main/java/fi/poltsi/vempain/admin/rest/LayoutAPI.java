package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600) // TODO Remove before going to production
@Tag(name = "Layout", description = "Layout API for Vempain layout objects")
public interface LayoutAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/layouts";

	@Operation(summary = "Fetch list of all layouts", description = "Returns a list of layouts", tags = "Layout")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of layouts",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = LayoutResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No layouts found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<LayoutResponse>> getLayouts();

	@Operation(summary = "Get a layout defined by id", description = "Returns a single layout based on id", tags = "Layout")
	@Parameter(name = "layout_id", example = "123", description = "ID of the layout to be retrieved", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the specific layout",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = LayoutResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No layout with given ID exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{layout_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<LayoutResponse> getLayoutById(@PathVariable(name = "layout_id") Long layoutId);

	@Operation(summary = "Get a layout defined by name", description = "Returns a single layout based on name", tags = "Layout")
	@Parameter(name = "layout_name", example = "My layout", description = "Name of the layout to be retrieved", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the specific layout",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = LayoutResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No layout with given name exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/name/{layout_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<LayoutResponse> getLayoutByName(@PathVariable(name = "layout_name") String layoutName);

	@Operation(summary = "Add a new layout", description = "Inserts a new layout", tags = "Layout")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Layout to be added, the name can not be empty nor null",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Layout created",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = LayoutResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Layout with same name already exists", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<LayoutResponse> saveLayout(@Valid @RequestBody LayoutRequest layoutRequest);

	@Operation(summary = "Update an existing layout", description = "Update an existing layout except if the layout name is empty or null",
			   tags = "Layout")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Layout to be updated, the name can not be empty nor null",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Layout updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = LayoutResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Layout did not exist", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE,
				consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<LayoutResponse> updateLayout(@Valid @RequestBody LayoutRequest layoutRequest);

	@Operation(summary = "Remove a layout", description = "Remove the layout given by the ID", tags = "Layout")
	@Parameter(name = "layout_id", example = "12", description = "Id of the layout to be deleted", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Layout removed",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DeleteResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No layout with the given id found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/{layout_id}")
	ResponseEntity<DeleteResponse> removeLayoutById(@PathVariable(name = "layout_id") Long layoutId);
}

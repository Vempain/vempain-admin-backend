package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
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
@Tag(name = "Component", description = "Component API for Vempain component objects")
public interface ComponentAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/components";

	@Operation(summary = "Fetch list of all components", description = "Returns a list of all components the user has read access to",
			   tags = "Component")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got list of components",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = ComponentResponse.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "No components found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<ComponentResponse>> getComponents();

	@Operation(summary = "Get a component defined by id", description = "Returns a single component based on id", tags = "Component")
	@Parameter(name = "component_id", example = "123", description = "ID of the component to be retrieved", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the specific component",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = ComponentResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No component with given ID exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{component_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ComponentResponse> getComponentById(@PathVariable(name = "component_id") Long componentId);

	@Operation(summary = "Add a new component", description = "Inserts a new component", tags = "Component")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Component to be added, the name can not be empty nor null",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Component created",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = ComponentResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Component with same name already exists", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ComponentResponse> addComponent(@Valid @RequestBody ComponentRequest componentRequest);

	@Operation(summary = "Update a component defined by id", description = "Returns the update component", tags = "Component")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Component to be updated, can not have an empty component name",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Component updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = ComponentResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No component with given ID exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE,
				consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ComponentResponse> updateComponent(@Valid @RequestBody ComponentRequest componentRequest);

	@Operation(summary = "Remove a component", description = "Remove the component given by the ID", tags = "Component")
	@Parameter(name = "component_id", example = "123", description = "Id of the component to be deleted", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Component removed",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DeleteResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No component with the given id found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/{component_id}")
	ResponseEntity<DeleteResponse> deleteComponentById(@PathVariable(name = "component_id") long componentId);
}

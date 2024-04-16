package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.FormRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.FormResponse;
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
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600) // TODO Remove before going to production
@Tag(name = "Form", description = "Form API for Vempain form objects")
public interface FormAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/forms";

	@Operation(summary = "Fetch list of all forms", description = "Returns a list of all forms the user has read access to", tags = "Form")
	@Parameter(name = "details", description = "How much details should be included", example = "FULL", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Got list of forms",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = FormResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No forms found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH)
	ResponseEntity<List<FormResponse>> getForms(@RequestParam(name = "details") @NotNull QueryDetailEnum requestForm);

	@Operation(summary = "Get all forms using a component", description = "Returns a list of form which uses a certain component", tags =
			"Form")
	@Parameter(name = "component_id", example = "123", description = "ID of the component for which the forms are retrieved", required =
			true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got a list of forms",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = FormResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No form with given ID exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/used-by-components/{component_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FormResponse>> getFormsByComponentId(@PathVariable(name = "component_id") long formId);

	@Operation(summary = "Get all forms using a layout", description = "Returns a list of form which uses a certain layout", tags =
			"Form")
	@Parameter(name = "layout_id", example = "123", description = "ID of the layout for which the forms are retrieved", required =
			true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got a list of forms",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = FormResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No form with given ID exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/used-by-layout/{layout_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FormResponse>> getFormsByLayoutId(@PathVariable(name = "layout_id") long layoutId);

	@Operation(summary = "Get a form defined by id", description = "Returns a single form based on id", tags = "Form")
	@Parameter(name = "form_id", example = "123", description = "ID of the form to be retrieved", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the specific form",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = FormResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No form with given ID exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{form_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<FormResponse> getFormById(@PathVariable(name = "form_id") Long formId);

	@Operation(summary = "Add a new form", description = "Inserts a new form", tags = "Form")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Form to be added, the name can not be empty nor null", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Form created",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = FormResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Form with same name already exists", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<FormResponse> addForm(@Valid @RequestBody FormRequest formRequest);

	@Operation(summary = "Update an existing form", description = "Updates a form", tags = "Form")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated form, the name can not be empty nor null", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Form updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = FormResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Form with same name already exists", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<FormResponse> updateForm(@Valid @RequestBody FormRequest formRequest);

	@Operation(summary = "Remove a form", description = "Remove the form given by the ID", tags = "Form")
	@Parameter(name = "form_id", example = "123", description = "Id of the form to be deleted", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Form removed",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DeleteResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No form with the given id found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/{form_id}")
	ResponseEntity<DeleteResponse> deleteFormById(@PathVariable(name = "form_id") long formId);
}

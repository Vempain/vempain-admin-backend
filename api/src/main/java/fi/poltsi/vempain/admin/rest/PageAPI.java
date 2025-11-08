package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.request.PublishRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

// TODO add validation annotation here
@CrossOrigin(origins = "*", maxAge = 3600) // TODO Remove before going to production
@Tag(name = "Page", description = "Page API for Vempain page objects")
public interface PageAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/pages";

	@Operation(summary = "Fetch list of all pages", description = "Returns a list of pages", tags = "Page")
	@Parameter(name = "details", description = "How much details should be included", example = "FULL", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of pages",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PageResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No pages found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<PageResponse>> getPages(@RequestParam(name = "details") @NotNull QueryDetailEnum requestForm);


	@Operation(summary = "Fetch list of all pages using a particular form", description = "Returns a list of pages", tags = "Page")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of pages",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PageResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No pages found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/by-form/{form_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<PageResponse>> getPagesByFormId(@PathVariable(name = "form_id") long formId);

	@Operation(summary = "Fetch a specific page", description = "Return the requested page", tags = "Page")
	@Parameter(name = "page_id", example = "123", description = "ID of the page to be retrieved", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Page found and returned",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = PageResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No page found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{page_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PageResponse> getPageById(@PathVariable(name = "page_id") long pageId);

	@Operation(summary = "Add a new page", description = "Inserts a new page", tags = "Page")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Page to be added, the title or header can not be empty nor null",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Page created",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = PageResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Page with same name already exists", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PageResponse> addPage(@Valid @RequestBody PageRequest pageRequest);

	@Operation(summary = "Update an existing page", description = "Update an existing page except if the page path, title or header is " +
																  "empty or null", tags = "Page")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Page to be updated, can not have an empty path, title or header",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Page updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = PageResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Page did not exist", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PageResponse> updatePage(@Valid @RequestBody PageRequest pageRequest);

	@Operation(summary = "Remove a page by ID", description = "Remove the page given by the ID", tags = "Page")
	@Parameter(name = "page_id", example = "123", description = "ID of the page to be deleted, can not be empty", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Page removed",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DeleteResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No page with the given id found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/{page_id}")
	ResponseEntity<DeleteResponse> deletePage(@PathVariable(name = "page_id") long pageId);

	// /////////////////////////// Publishing actions
	@Operation(summary = "Publish all pages", description = "Publish a new version of all pages", tags = "Page")
	@Parameter(name = "publish_date", description = "Date when the all the pages should be published, in YYYY-MM-DDTHH:mm:ss format",
			   example = "2027-12-31T23:59:59")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "All pages published",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PublishResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No pages found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishResponse> publishAll(@RequestParam(name = "publish_date", required = false) Instant publishDate);

	@Operation(summary = "Publish page", description = "Publish a new version of a page", tags = "Page")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Publish request with page ID and optional delay in seconds", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Page published, or will be published",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PublishResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No page found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PatchMapping(value = MAIN_PATH + "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishResponse> publishPage(@Valid @RequestBody PublishRequest publishRequest);

	@Operation(summary = "Delete", description = "Delete the page from site", tags = "Page")
	@Parameter(name = "page_id", description = "Page ID", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Page deleted",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PublishResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No page found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/publish/{page_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishResponse> deletePublishedPage(@PathVariable(name = "page_id") long pageId);
}

package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Gallery", description = "REST API for Vempain gallery objects")
public interface GalleryAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/galleries";

	@Operation(summary = "Get galleries", description = "Fetch all galleries", tags = "Gallery")
	@Parameter(name = "details", description = "How much details should be included", example = "FULL", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of galleries",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = GalleryResponse.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<GalleryResponse>> getGalleries(@RequestParam(name = "details") @NotNull QueryDetailEnum queryDetailEnum);

	@Operation(summary = "Get galleries linked to a page", description = "Fetch all galleries linked to a given page", tags = "Gallery")
	@Parameter(name = "details", description = "How much details should be included", example = "FULL", required = true)
	@Parameter(name = "pageId", description = "Page ID", example = "1", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of galleries",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = GalleryResponse.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/page/{pageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<GalleryResponse>> getGalleriesByPage(@PathVariable(name = "pageId") long pageId,
															 @RequestParam(name = "details") @NotNull QueryDetailEnum queryDetailEnum);

	@Operation(summary = "Fetch a specific gallery", description = "Return the requested gallery", tags = "Gallery")
	@Parameter(name = "gallery_id", example = "123", description = "ID of the gallery to be retrieved", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Gallery found and returned",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = PageResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No gallery found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{gallery_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<GalleryResponse> getGalleryById(@PathVariable(name = "gallery_id") long galleryId);

	@Operation(summary = "Create a new gallery", description = "Create a new gallery", tags = "Gallery")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Gallery to be created", required = true,
														  content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
																			 schema = @Schema(implementation = GalleryRequest.class)))
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Gallery updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = GalleryRequest.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<GalleryResponse> createGallery(@RequestBody @NotNull GalleryRequest galleryRequest);

	@Operation(summary = "Delete a gallery", description = "Delete a gallery", tags = "Gallery")
	@Parameter(name = "gallery_id", example = "123", description = "ID of the gallery to be deleted", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Gallery deleted",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = GalleryRequest.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/{gallery_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DeleteResponse> deleteGallery(@PathVariable(name = "gallery_id") long galleryId);

	@Operation(summary = "Update gallery", description = "Update an existing gallery", tags = "Gallery")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Gallery to be updated",
														  required = true,
														  content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
																			 schema = @Schema(implementation = GalleryRequest.class)))
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Gallery updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = GalleryRequest.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No gallery found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<GalleryResponse> updateGallery(@Valid @RequestBody GalleryRequest galleryRequest);

	@Operation(summary = "Set galleries to page", description = "Set the list of galleries belonging to a page", tags = "Gallery")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of gallery ID",
														  required = true,
														  content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
																			 schema = @Schema(implementation = List.class)))
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "List updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = List.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No page found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH + "/page/{pageId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<GalleryResponse>> setPageGalleries(@PathVariable(name = "pageId") long pageId,
														   @RequestBody @NotNull List<Long> galleryIdList);

	// /////////////////////////// Publishing actions

	@Operation(summary = "Publish all galleries", description = "Publish a new version of all galleries", tags = "Publish")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Galleries published",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PublishResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No gallery found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/publish", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishResponse> publishAll();

	@Operation(summary = "Publish gallery", description = "Publish a new version of a gallery", tags = "Publish")
	@Parameter(name = "gallery_id", description = "Gallery ID", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Gallery published",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PublishResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No gallery found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/publish/{gallery_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishResponse> publishGallery(@PathVariable(name = "gallery_id") Long galleryId);
}

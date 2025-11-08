package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import static fi.poltsi.vempain.admin.api.Constants.REST_FILE_PREFIX;

@Tag(name = "File", description = "REST API for Vempain image file objects")
public interface FileAPI {
	String MAIN_PATH = REST_FILE_PREFIX;

	/* Audio */
	@Operation(summary = "Get site files as a pageable", description = "Fetch all site files in pageable format", tags = "File")
	@Parameter(name = "page_number", description = "Page number", allowEmptyValue = true, example = "1")
	@Parameter(name = "page_size", description = "Page number", allowEmptyValue = true, example = "10")
	@Parameter(name = "sorting",
			   description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
			   example = "createdAt,desc")
	@Parameter(name = "filter", description = "Filter the column by the string", example = "Find me")
	@Parameter(name = "filter_column", description = "By which column should the filtering be done", example = "message")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of site files"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/site-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<SiteFileResponse>> getPageableSiteFiles(
			@RequestParam(name = "file_type") @NotNull FileClassEnum fileClassEnum,
			@RequestParam(name = "page_number") int pageNumber,
			@RequestParam(name = "page_size") int pageSize,
			@RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
			@RequestParam(name = "filter", defaultValue = "") String filter,
			@RequestParam(name = "filter_column", defaultValue = "") String filterColumn
	);

	/* Video */

	@Operation(summary = "Refresh the file information of a gallery", description = "Reload all the file data of the files belonging to a gallery",
			   tags = "File")
	@Parameter(name = "gallery_id", example = "123", description = "ID of the gallery to be refreshed", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Gallery files refreshed",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = GalleryRequest.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/refresh-gallery-files/{gallery_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<RefreshResponse> refreshGalleryFiles(@PathVariable(name = "gallery_id") long galleryId);


	@Operation(summary = "Refresh the file information of all galleries", description = "Reload all the file data of the files belonging any gallery",
			   tags = "File")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "All gallery files refreshed",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = GalleryRequest.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/refresh-all-gallery-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<RefreshResponse> refreshAllGalleryFiles();
}

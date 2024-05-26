package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.StringList;
import fi.poltsi.vempain.admin.api.response.file.FileAudioResponse;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.api.response.file.FileDocumentResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImageResponse;
import fi.poltsi.vempain.admin.api.response.file.FileVideoResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "File", description = "REST API for Vempain image file objects")
public interface FileAPI {
	String MAIN_PATH = Constants.REST_FILE_PREFIX;

	/* Audio */
	@Operation(summary = "Get audio files as a pageable", description = "Fetch all audio files in pageable format", tags = "File")
	@Parameter(name = "page_number", description = "Page number", allowEmptyValue = true, example = "1")
	@Parameter(name = "page_size", description = "Page number", allowEmptyValue = true, example = "10")
	@Parameter(name = "sorting",
			   description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
			   example = "createdAt,desc")
	@Parameter(name = "filter", description = "Filter the column by the string", example = "Find me")
	@Parameter(name = "filter_column", description = "By which column should the filtering be done", example = "message")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of audio files"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/audio-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<FileAudioResponse>> getPageableAudioFiles(
			@RequestParam(name = "page_number") @NonNull int pageNumber,
			@RequestParam(name = "page_size") @NonNull int pageSize,
			@RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
			@RequestParam(name = "filter", defaultValue = "") String filter,
			@RequestParam(name = "filter_column", defaultValue = "") String filterColumn
	);

	/* Document */
	@Operation(summary = "Get document files as a pageable", description = "Fetch all document files in pageable format", tags = "File")
	@Parameter(name = "page_number", description = "Page number", allowEmptyValue = true, example = "1")
	@Parameter(name = "page_size", description = "Page number", allowEmptyValue = true, example = "10")
	@Parameter(name = "sorting",
			   description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
			   example = "createdAt,desc")
	@Parameter(name = "filter", description = "Filter the column by the string", example = "Find me")
	@Parameter(name = "filter_column", description = "By which column should the filtering be done", example = "message")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of document files"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/document-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<FileDocumentResponse>> getPageableDocumentFiles(
			@RequestParam(name = "page_number") @NonNull int pageNumber,
			@RequestParam(name = "page_size") @NonNull int pageSize,
			@RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
			@RequestParam(name = "filter", defaultValue = "") String filter,
			@RequestParam(name = "filter_column", defaultValue = "") String filterColumn
	);

	/* Image */
	@Operation(summary = "Get image files as a pageable", description = "Fetch all image files in pageable format", tags = "File")
	@Parameter(name = "page_number", description = "Page number", allowEmptyValue = true, example = "1")
	@Parameter(name = "page_size", description = "Page number", allowEmptyValue = true, example = "10")
	@Parameter(name = "sorting",
			   description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
			   example = "createdAt,desc")
	@Parameter(name = "filter", description = "Filter the column by the string", example = "Find me")
	@Parameter(name = "filter_column", description = "By which column should the filtering be done", example = "message")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of image files"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/image-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<FileImageResponse>> getPageableImageFiles(
			@RequestParam(name = "page_number") @NonNull int pageNumber,
			@RequestParam(name = "page_size") @NonNull int pageSize,
			@RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
			@RequestParam(name = "filter", defaultValue = "") String filter,
			@RequestParam(name = "filter_column", defaultValue = "") String filterColumn
	);

	@Operation(summary = "Get image by the given ID", description = "Fetch image for the given ID", tags = "File")
	@Parameter(name = "image_id", description = "Image ID", example = "1")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got the image"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/image-files/{image_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<FileImageResponse> getImageById(@PathVariable(name = "image_id") long imageId);

	/* Video */
	@Operation(summary = "Get video files as a pageable", description = "Fetch all video files in pageable format", tags = "File")
	@Parameter(name = "page_number", description = "Page number", allowEmptyValue = true, example = "1")
	@Parameter(name = "page_size", description = "Page number", allowEmptyValue = true, example = "10")
	@Parameter(name = "sorting",
			   description = "What field the page should be sorted by, and which direction. The two values should be comma separated",
			   example = "createdAt,desc")
	@Parameter(name = "filter", description = "Filter the column by the string", example = "Find me")
	@Parameter(name = "filter_column", description = "By which column should the filtering be done", example = "message")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of video files"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/video-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<FileVideoResponse>> getPageableVideoFiles(
			@RequestParam(name = "page_number") @NonNull int pageNumber,
			@RequestParam(name = "page_size") @NonNull int pageSize,
			@RequestParam(name = "sorting", defaultValue = "createdAt,desc") String sorting,
			@RequestParam(name = "filter", defaultValue = "") String filter,
			@RequestParam(name = "filter_column", defaultValue = "") String filterColumn
	);

	@Operation(summary = "Process directory for files", description = "Add files in given directory and optionally generate a gallery",
			   tags = "File")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Processing request detailing the source and target directories",
														  required = true)
	@PostMapping(value = MAIN_PATH + "/add-directory", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FileCommonResponse>> addFilesFromDirectory(@Valid @RequestBody FileProcessRequest fileProcessRequest);

	@Operation(summary = "Receive upload files", description = "Receive upload files as well as optional data to create a gallery", tags
			= "File")
	@Parameter(name = "file_list", description = "Directory where the files should be stored", required = true)
	@Parameter(name = "destination_directory", description = "Directory where the files should be stored", example = "/example",
			   required = true)
	@Parameter(name = "gallery_shortname", description = "Gallery short name, empty if no gallery is to be created", example = "A gallery")
	@Parameter(name = "gallery_description",
			   description = "Gallery description, empty if no gallery is to be created",
			   example = "A small gallery")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of image files",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = FileImageResponse.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH + "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FileCommonResponse>> upload(
			@RequestParam(name = "file_list") MultipartFile[] multipartFiles,
			@RequestParam(name = "destination_directory") String destinationDirectory,
			@RequestParam(name = "gallery_shortname", required = false) String galleryShortname,
			@RequestParam(name = "gallery_description", required = false) String galleryDescription);

	@Operation(summary = "Get list of import directories",
			   description = "Scan locally the main import directory and return a list of all paths therein for autocompletion use",
			   tags = "File")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of directories",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = List.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(path = MAIN_PATH + "/import-directories", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<String>> importDirectories();

	@Operation(summary = "Return a list of matching import directories",
			   description = "Check if the given path is a valid and return a list of matching directories",
			   tags = "File")
	@Parameter(name = "path", description = "Path to be imported", example = "images/", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "The given path exists",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = List.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "404", description = "Given path does not exist", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(path = MAIN_PATH + "/import-directory", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<StringList> importDirectoryMatch(@NotNull @RequestParam(name = "path") String path);

	@Operation(summary = "Refresh the file information of a gallery", description = "Reload all the file data of the files belonging to a gallery",
			   tags ="File")
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
			   tags ="File")
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

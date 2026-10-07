package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static fi.poltsi.vempain.admin.api.Constants.REST_FILE_PREFIX;

@Tag(name = "FileIngestAPI", description = "Test API for multiple file uploads and JSON payloads")
public interface FileIngestAPI {
	String MAIN_PATH = REST_FILE_PREFIX;

	@Operation(
			summary = "Ingest a file to the site storage",
			description = "Service-to-service endpoint. Auth via Bearer token. Accepts metadata JSON and a multipart file. " +
			              "Places the file under vempain.admin.file.site-file-directory/<class>/<filePath>/<fileName> where class is derived from mimetype.",
			tags = "FileIngestApi"
	)
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(
			value = MAIN_PATH + "/site-file",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<FileIngestResponse> ingest(
			@RequestPart("request") final String fileIngestRequestJSON,
			@RequestPart(value = "site_file") final MultipartFile siteFile);

	@Operation(
			summary = "Delete an ingested site file",
			description = "Service-to-service endpoint. Removes a site file created by the ingest endpoint together with its stored file, gallery links, "
						  + "subjects, thumbnail and ACL. Used by the file backend to revert a cancelled publish.",
			tags = "FileIngestApi"
	)
	@ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Site file deleted", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Site file not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@DeleteMapping(value = MAIN_PATH + "/site-file/{siteFileId}")
	ResponseEntity<Void> deleteSiteFile(@PathVariable("siteFileId") long siteFileId);
}

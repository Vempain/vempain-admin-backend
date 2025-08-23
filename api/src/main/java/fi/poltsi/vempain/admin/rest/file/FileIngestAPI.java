package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import static fi.poltsi.vempain.admin.api.Constants.REST_FILE_PREFIX;

@Tag(name = "FileIngestApi", description = "Test API for multiple file uploads and JSON payloads")
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
			@RequestPart("request")
			@RequestBody(content = @Content(encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)))
			@Parameter(
					description = "File ingest request accompanying the file",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) final FileIngestRequest fileIngestRequest,
			@RequestPart(value = "site_file") @Parameter(description = "Site file") final MultipartFile siteFile);
}

package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Site File Ingest")
@RequestMapping(path = "/s2s/files")
public interface FileIngestApi {

	@Operation(
		summary = "Ingest a file to the site storage",
		description = "Service-to-service endpoint. Auth via PSK in X-PSK header. Accepts metadata JSON and a multipart file. " +
			"Places the file under vempain.admin.file.site-file-directory/<class>/<filePath>/<fileName> where class is derived from mimetype."
	)
	@io.swagger.v3.oas.annotations.responses.ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error"),
	})
	ResponseEntity<FileIngestResponse> ingest(
		@Parameter(description = "Pre-shared key", required = true)
		@RequestHeader("X-PSK") String preSharedKey,
		@RequestBody(
			required = true,
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = FileIngestRequest.class)
			)
		)
		@RequestPart("meta") FileIngestRequest meta,
		@Parameter(description = "Binary payload", required = true)
		@RequestPart("file") MultipartFile file
	);
}


package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.response.file.DirectoryNodeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Tag(name = "FileSystem", description = "REST API for Vempain image filesystem operations")
public interface FileSystemAPI {
	String MAIN_PATH = Constants.REST_FILE_PREFIX;

	@Operation(summary = "Return the site file directory tree",
			   description = "Return a tree structure of the site file directory sub directories",
			   tags = "FileSystem")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "The tree structure of the site file directory returned",
						 content = {@Content(array = @ArraySchema(schema = @Schema(implementation = DirectoryNodeResponse.class)),
											 mediaType = MediaType.APPLICATION_JSON_VALUE)}),
			@ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(path = MAIN_PATH + "/site-file-directory", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<DirectoryNodeResponse>> getConvertedDirectoryStructure();

}

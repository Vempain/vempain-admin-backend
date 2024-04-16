package fi.poltsi.vempain.admin.rest.file;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "CommonFile", description = "REST API for Vempain common file objects")
public interface CommonFileAPI {
	String MAIN_PATH = Constants.REST_FILE_PREFIX;

	@Operation(summary = "Get all common files", description = "Fetch all common files", tags = "CommonFile")
	@Parameter(name = "details", description = "How much details should be included", example = "FULL", required = true)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Got a list of common files"),
			@ApiResponse(responseCode = "400", description = "Invalid request issued"),
			@ApiResponse(responseCode = "401", description = "Unauthorized access"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/common-files", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FileCommonResponse>> getCommonFiles(@RequestParam(name = "details") @NotNull QueryDetailEnum requestForm);
}

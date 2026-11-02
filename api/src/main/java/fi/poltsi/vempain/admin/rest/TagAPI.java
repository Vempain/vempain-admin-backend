package fi.poltsi.vempain.admin.rest;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.response.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The tags known to the admin backend, for the tag suggestions of the page editor. The tags of the published files arrive with every publish of the file
 * backend and the tags of the pages are created when a page is saved, so this list is the union of both; it is not a way to create tags.
 */
@Tag(name = "TagAPI", description = "Tags known to the admin backend (published files and pages)")
public interface TagAPI {

    String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/tags";
    int DEFAULT_LIMIT = 20;
    int MAX_LIMIT = 50;

    @Operation(summary = "Find tags",
            description = "Tags whose name contains the search text (case-insensitive), alphabetically; every tag when the "
                    + "text is empty. Any signed-in user may ask.",
            tags = "TagAPI")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Matching tags",
                        content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagResponse.class)),
                                mediaType = MediaType.APPLICATION_JSON_VALUE)),
                @ApiResponse(responseCode = "401", description = "Not signed in", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TagResponse>> findTags(
            @Parameter(description = "Text the tag name must contain, case-insensitive; empty lists the first tags") @RequestParam(value = "search",
                    required = false) String search,
            @Parameter(description = "Maximum number of tags (1..50, default 20)") @RequestParam(value = "limit", required = false) Integer limit);
}

package fi.poltsi.vempain.admin.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.request.TagBlacklistRequest;
import fi.poltsi.vempain.admin.api.response.TagBlacklistResponse;
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
 * The tag blacklist: tags (subjects) that are removed from the files, their metadata and the other resources published to the Vempain Website. Managed by
 * administrators under Web Site Management.
 */
@Tag(name = "TagBlacklistAPI", description = "Tags that are never published to the website")
@SecurityRequirement(name = "Bearer Authentication")
public interface TagBlacklistAPI {

    String MAIN_PATH = Constants.REST_ADMIN_PREFIX + "/tag-blacklist";

    @Operation(summary = "List the blacklisted tags", description = "Every tag that is removed before publishing, in alphabetical order",
            tags = "TagBlacklistAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The blacklist",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagBlacklistResponse.class)),
                        mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not an administrator", content = @Content) })
    @GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TagBlacklistResponse>> getBlacklist();

    @Operation(summary = "Blacklist a tag",
            description = "Adds a tag; it is left out of every publish from now on. Already published resources are only "
                    + "cleaned when they are published again",
            tags = "TagBlacklistAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "The tag was added",
                content = @Content(schema = @Schema(implementation = TagBlacklistResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "400", description = "Blank tag", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not an administrator", content = @Content),
        @ApiResponse(responseCode = "409", description = "The tag is already blacklisted", content = @Content) })
    @PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TagBlacklistResponse> addToBlacklist(@Valid @RequestBody TagBlacklistRequest request);

    @Operation(summary = "Remove a tag from the blacklist", tags = "TagBlacklistAPI")
    @Parameter(name = "entry_id", description = "ID of the blacklist entry", required = true, example = "3")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "The tag was removed from the blacklist", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Not an administrator", content = @Content),
        @ApiResponse(responseCode = "404", description = "No such entry", content = @Content) })
    @DeleteMapping(value = MAIN_PATH + "/{entry_id}")
    ResponseEntity<Void> removeFromBlacklist(@PathVariable("entry_id") long entryId);
}

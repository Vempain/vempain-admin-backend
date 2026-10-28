package fi.poltsi.vempain.admin.rest.file;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.api.request.file.SiteFilePublishRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import static fi.poltsi.vempain.admin.api.Constants.REST_FILE_PREFIX;

@Tag(name = "FileAPI", description = "REST API for Vempain image file objects")
public interface FileAPI {

    String MAIN_PATH = REST_FILE_PREFIX;

    @Operation(summary = "Get site files as a pageable", description = "Fetch all site files in pageable format", tags = "FileAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Got a list of site files"),
        @ApiResponse(responseCode = "400", description = "Invalid request issued"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal server error") })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH + "/site-files/paged", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<SiteFileResponse>> getPageableSiteFiles(@Valid @RequestBody SiteFilePagedRequest request);

    @Operation(summary = "Publish selected site files",
            description = "Transfers the selected site files with their thumbnails to the site server and publishes their metadata, without any gallery. "
                    + "Files that do not exist or that the caller may not publish (modify privilege on the file) are skipped and counted in the task result.",
            tags = "FileAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of site file IDs to publish", required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SiteFilePublishRequest.class)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Publishing started as a background task carried in the response",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PublishResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH + "/site-files/publish-selected", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PublishResponse> publishSelectedSiteFiles(@Valid @RequestBody SiteFilePublishRequest request);

    @Operation(summary = "Delete a site file",
            description = "Removes the site file from the admin side (stored file, thumbnails, gallery links, subjects and ACL) and its metadata from the "
                    + "website. Requires the delete privilege on the file.",
            tags = "FileAPI")
    @Parameter(name = "site_file_id", example = "123", description = "ID of the site file to delete", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Site file deleted",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DeleteResponse.class)) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "403", description = "No delete privilege on the site file", content = @Content),
        @ApiResponse(responseCode = "404", description = "Site file not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(value = MAIN_PATH + "/site-files/{site_file_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DeleteResponse> deleteSiteFile(@PathVariable(name = "site_file_id") long siteFileId);

    @Operation(summary = "Refresh the file information of a gallery", description = "Reload all the file data of the files belonging to a gallery",
            tags = "FileAPI")
    @Parameter(name = "gallery_id", example = "123", description = "ID of the gallery to be refreshed", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Gallery files refreshed",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = GalleryRequest.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/refresh-gallery-files/{gallery_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<RefreshResponse> refreshGalleryFiles(@PathVariable(name = "gallery_id") long galleryId);

    @Operation(summary = "Refresh the file information of all galleries", description = "Reload all the file data of the files belonging any gallery",
            tags = "FileAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Refresh started as a background task carried in the response",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RefreshResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/refresh-all-gallery-files", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<RefreshResponse> refreshAllGalleryFiles();
}

package fi.poltsi.vempain.admin.rest.file;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import fi.poltsi.vempain.admin.api.response.file.FileIngestGalleryAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestSiteFileAclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUnitResponse;
import fi.poltsi.vempain.admin.api.response.file.FileIngestUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import static fi.poltsi.vempain.admin.api.Constants.REST_FILE_PREFIX;

@Tag(name = "FileIngestAPI", description = "Test API for multiple file uploads and JSON payloads")
public interface FileIngestAPI {

    String MAIN_PATH = REST_FILE_PREFIX;

    @Operation(summary = "Ingest a file to the site storage",
            description = "Service-to-service endpoint. Auth via Bearer token. Accepts metadata JSON and a multipart file. "
                    + "Places the file under vempain.admin.file.site-file-directory/<class>/<filePath>/<fileName> where class is derived from mimetype. "
                    + "The optional acls list grants additional admin users or units privileges on the site file and its gallery; it is validated before "
                    + "the file is stored and an invalid list answers 400.",
            tags = "FileIngestApi")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH + "/site-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FileIngestResponse> ingest(@RequestPart("request") final String fileIngestRequestJSON,
            @RequestPart(value = "site_file") final MultipartFile siteFile);

    @Operation(summary = "Delete an ingested site file",
            description = "Service-to-service endpoint. Removes a site file created by the ingest endpoint together with its stored file, gallery links, "
                    + "subjects, thumbnail and ACL. Used by the file backend to revert a cancelled publish.",
            tags = "FileIngestApi")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Site file deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Site file not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(value = MAIN_PATH + "/site-file/{siteFileId}")
    ResponseEntity<Void> deleteSiteFile(@PathVariable("siteFileId") long siteFileId);

    @Operation(summary = "List the admin users that can be granted privileges on ingested resources",
            description = "Service-to-service endpoint. Returns the active admin user accounts (ID, login name, name and nick only) so that the "
                    + "calling service can let its user pick additional ACL grantees for the site files and galleries it ingests.",
            tags = "FileIngestApi")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of grantable users", content = {
            @Content(array = @ArraySchema(schema = @Schema(implementation = FileIngestUserResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/site-file/users", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<FileIngestUserResponse>> listIngestUsers();

    @Operation(summary = "List the admin units that can be granted privileges on ingested resources",
            description = "Service-to-service endpoint. Returns the admin units (ID, name and description only) so that the calling service "
                    + "can let its user pick additional ACL grantees for the site files and galleries it ingests.",
            tags = "FileIngestApi")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of grantable units", content = {
            @Content(array = @ArraySchema(schema = @Schema(implementation = FileIngestUnitResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/site-file/units", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<FileIngestUnitResponse>> listIngestUnits();

    @Operation(summary = "Current ACL grantees of an ingested site file",
            description = "Service-to-service endpoint. Answers the admin users and units that hold privileges on the site file next to the ingesting "
                    + "account, in the shape of the acls list of the ingest request, so that the calling service can show and resend them when the same "
                    + "file is ingested again.",
            tags = "FileIngestApi")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "The grantees of the site file",
                        content = {
                            @Content(schema = @Schema(implementation = FileIngestSiteFileAclResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
                @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
                @ApiResponse(responseCode = "404", description = "Site file not found", content = @Content),
                @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/site-file/{siteFileId}/acls", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FileIngestSiteFileAclResponse> getSiteFileAcls(@PathVariable("siteFileId") long siteFileId);

    @Operation(summary = "Current ACL grantees of the site file ingested from a given source file",
            description = "Service-to-service endpoint. Same answer as the lookup by site file ID, but keyed by the file_id the ingest request carried "
                    + "(the files.id of the file backend, unique per site file), for files published before the calling service recorded the site "
                    + "file ID.",
            tags = "FileIngestApi")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "The grantees of the site file",
                        content = {
                            @Content(schema = @Schema(implementation = FileIngestSiteFileAclResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
                @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
                @ApiResponse(responseCode = "404", description = "No site file was ingested from that file", content = @Content),
                @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/site-file/by-file-id/{fileId}/acls", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FileIngestSiteFileAclResponse> getSiteFileAclsByFileId(@PathVariable("fileId") long fileId);

    @Operation(summary = "Current ACL grantees and site files of an ingested gallery",
            description = "Service-to-service endpoint. Answers the admin users and units that hold privileges on the gallery next to the ingesting "
                    + "account, plus the IDs of the site files linked to the gallery, so that the calling service can show and resend the grantees when "
                    + "the same file group is ingested again.",
            tags = "FileIngestApi")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "The grantees and site files of the gallery",
                        content = {
                            @Content(schema = @Schema(implementation = FileIngestGalleryAclResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
                @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
                @ApiResponse(responseCode = "404", description = "Gallery not found", content = @Content),
                @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/gallery/{galleryId}/acls", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FileIngestGalleryAclResponse> getGalleryAcls(@PathVariable("galleryId") long galleryId);
}

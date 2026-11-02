package fi.poltsi.vempain.admin.rest;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import fi.poltsi.vempain.admin.api.request.DataPagedRequest;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSetAclResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.common.api.response.TaskAcceptedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import static fi.poltsi.vempain.admin.api.Constants.REST_DATA_PREFIX;

@Tag(name = "DataAPI", description = "REST API for Vempain CSV data upload and publication")
public interface DataAPI {

    String MAIN_PATH = REST_DATA_PREFIX;

    @Operation(summary = "List all data sets", description = "Returns a list of all stored data sets with metadata but without the CSV data", tags = "DataAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Got list of data sets", content = {
            @Content(array = @ArraySchema(schema = @Schema(implementation = DataSummaryResponse.class)), mediaType = MediaType.APPLICATION_JSON_VALUE) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DataSummaryResponse>> getAllDataSets(@RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "identifier_prefix", required = false) String identifierPrefix,
            @RequestParam(name = "search", required = false) String search);

    @Operation(summary = "List data sets page by page",
            description = "The data sets the caller may read, without their CSV data, filtered, sorted and paged by the backend: search over identifier, "
                    + "type and description (or the one filter_column), an optional type, the modified-after-publish filter, and sort_by one of "
                    + "identifier, type, description, generated, published, created, modified, creator, modifier, acl_id or id",
            tags = "DataAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "A page of data sets",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid paging parameters", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH + "/paged", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedResponse<DataSummaryResponse>> getPagedDataSets(@Valid @RequestBody DataPagedRequest request);

    @Operation(summary = "Get a data set by identifier", description = "Returns the metadata and CSV data for the requested data set", tags = "DataAPI")
    @Parameter(name = "identifier", example = "cd_collection", description = "Unique identifier of the data set", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data set found and returned",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DataResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "403", description = "The caller may not read the data set", content = @Content),
        @ApiResponse(responseCode = "404", description = "No data set found for the given identifier", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DataResponse> getDataSetByIdentifier(@PathVariable(name = "identifier") String identifier);

    @Operation(summary = "Current ACL grantees of a data set",
            description = "Answers the admin users and units that hold privileges on the data set next to the calling account, in the shape of the acls list "
                    + "of the create and update requests, so that the service that created the set can show and resend them. Requires the read privilege "
                    + "on the data set.",
            tags = "DataAPI")
    @Parameter(name = "identifier", example = "music_library", description = "Unique identifier of the data set", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The grantees of the data set",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DataSetAclResponse.class)) }),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "403", description = "The caller may not read the data set", content = @Content),
        @ApiResponse(responseCode = "404", description = "No data set found for the given identifier", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = MAIN_PATH + "/{identifier}/acls", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DataSetAclResponse> getDataSetAcls(@PathVariable(name = "identifier") String identifier);

    @Operation(summary = "Create a new data set",
            description = "Stores the provided metadata and CSV data as a new data set owned by the caller; the optional acls grant other admin users "
                    + "and units privileges on it",
            tags = "DataAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Data set metadata and CSV content", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data set created",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DataResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid or malformed request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "409", description = "Data set with same identifier already exists", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DataResponse> createDataSet(@Valid @RequestBody DataRequest dataRequest);

    @Operation(summary = "Update an existing data set",
            description = "Replaces the metadata and CSV data of an existing data set; requires the modify privilege. A non-null acls list replaces the "
                    + "additional grantees of the set",
            tags = "DataAPI")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated data set metadata and CSV content", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data set updated",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DataResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid or malformed request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Data set not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DataResponse> updateDataSet(@Valid @RequestBody DataRequest dataRequest);

    @Operation(summary = "Publish a data set to the site database",
            description = "Starts a background task that creates or replaces the table in the site database and imports the CSV data; "
                    + "follow it through the task API, the finished task carries the DataResponse as result",
            tags = "DataAPI")
    @Parameter(name = "identifier", example = "cd_collection", description = "Unique identifier of the data set to publish", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Publishing started",
                content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskAcceptedResponse.class)) }),
        @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Data set not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = MAIN_PATH + "/{identifier}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TaskAcceptedResponse> publishDataSet(@PathVariable(name = "identifier") String identifier);

    @Operation(summary = "Delete a data set",
            description = "Removes the stored data set and drops its table from the site database. Used by the file backend to revert a cancelled "
                    + "data set publication.",
            tags = "DataAPI")
    @Parameter(name = "identifier", example = "cd_collection", description = "Unique identifier of the data set to delete", required = true)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Data set deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
        @ApiResponse(responseCode = "404", description = "Data set not found", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content) })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping(value = MAIN_PATH + "/{identifier}")
    ResponseEntity<Void> deleteDataSet(@PathVariable(name = "identifier") String identifier);
}

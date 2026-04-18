package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import static fi.poltsi.vempain.admin.api.Constants.REST_DATA_PREFIX;

@Tag(name = "DataAPI", description = "REST API for Vempain CSV data upload and publication")
public interface DataAPI {
	String MAIN_PATH = REST_DATA_PREFIX;

	@Operation(summary = "List all data sets", description = "Returns a list of all stored data sets with metadata but without the CSV data", tags = "DataAPI")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of data sets",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = DataSummaryResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<DataSummaryResponse>> getAllDataSets();

	@Operation(summary = "Get a data set by identifier", description = "Returns the metadata and CSV data for the requested data set", tags = "DataAPI")
	@Parameter(name = "identifier", example = "cd_collection", description = "Unique identifier of the data set", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Data set found and returned",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DataResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No data set found for the given identifier", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DataResponse> getDataSetByIdentifier(@PathVariable(name = "identifier") String identifier);

	@Operation(summary = "Create a new data set", description = "Stores the provided metadata and CSV data as a new data set", tags = "DataAPI")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Data set metadata and CSV content", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Data set created",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DataResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid or malformed request", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "409", description = "Data set with same identifier already exists", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DataResponse> createDataSet(@Valid @RequestBody DataRequest dataRequest);

	@Operation(summary = "Update an existing data set", description = "Replaces the metadata and CSV data of an existing data set", tags = "DataAPI")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated data set metadata and CSV content", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Data set updated",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DataResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid or malformed request", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Data set not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PutMapping(value = MAIN_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DataResponse> updateDataSet(@Valid @RequestBody DataRequest dataRequest);

	@Operation(summary = "Publish a data set to the site database",
			   description = "Creates or replaces the table in the site database and imports the CSV data", tags = "DataAPI")
	@Parameter(name = "identifier", example = "cd_collection", description = "Unique identifier of the data set to publish", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Data set published successfully",
										content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
															schema = @Schema(implementation = DataResponse.class))}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "Data set not found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = MAIN_PATH + "/{identifier}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<DataResponse> publishDataSet(@PathVariable(name = "identifier") String identifier);
}

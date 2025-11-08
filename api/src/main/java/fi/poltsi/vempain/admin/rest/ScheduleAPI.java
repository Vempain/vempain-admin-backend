package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.request.PublishScheduleRequest;
import fi.poltsi.vempain.admin.api.request.TriggerSystemScheduleRequest;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.ScheduleTriggerResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImportScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import static fi.poltsi.vempain.admin.api.Constants.REST_SCHEDULE_PREFIX;

@Tag(name = "Schedule", description = "Schedule API for Vempain schedules")
public interface ScheduleAPI {
	@Operation(summary = "Fetch list of all schedules", description = "Returns a list of all active schedules", tags = "Schedule")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of schedules",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = ScheduleTriggerResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX + "/system-schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<ScheduleTriggerResponse>> getSystemSchedules();

	@Operation(summary = "Fetch the specific system schedule", description = "Returns the DTO of a given system schedule", tags = "Schedule")
	@Parameter(name = "schedule_name", example = "fi.poltsi.vempain.admin.schedule.AclConsistencySchedule.verify",
			   description = "Name of the system schedule", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the details of the system schedule",
										content = {@Content(schema = @Schema(implementation = ScheduleTriggerResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX + "/system-schedules/{schedule_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ScheduleTriggerResponse> getSystemScheduleByName(@PathVariable("schedule_name") String systemScheduleName);

	@Operation(summary = "Trigger system schedule", description = "Trigger the selected system schedule now", tags = "Schedule")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "System schedule to be triggered", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Schedule triggered successfully",
										content = {@Content(schema = @Schema(implementation = ScheduleTriggerResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "404", description = "Given schedule does not exist", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = REST_SCHEDULE_PREFIX + "/system-schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ScheduleTriggerResponse> triggerSystemSchedule(@RequestBody @NotNull TriggerSystemScheduleRequest schedule);

	@Operation(summary = "View scheduled item publishing", description = "Lists all publishing that have been scheduled", tags = "Schedule")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "List retrieved successfully",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = PublishScheduleResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX + "/publishing", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<PublishScheduleResponse>> listPublishingSchedules();

	@Operation(summary = "Fetch the specific publish schedule", description = "Returns the DTO of a given publish schedule", tags = "Schedule")
	@Parameter(name = "id", example = "123", description = "ID of the publish schedule", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the details of the system schedule",
										content = {@Content(schema = @Schema(implementation = PublishScheduleResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX + "/publishing/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishScheduleResponse> getPublishingScheduleById(@PathVariable(value = "id") long id);

	@Operation(summary = "Trigger publish schedule", description = "Trigger the selected publish schedule now", tags = "Schedule")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Publish schedule to be triggered", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Schedule triggered successfully",
										content = {@Content(schema = @Schema(implementation = PublishScheduleResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "404", description = "Given schedule does not exist", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = REST_SCHEDULE_PREFIX + "/publishing", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PublishScheduleResponse> triggerPublishSchedule(@RequestBody @NotNull PublishScheduleRequest publishScheduleRequest);

	@Operation(summary = "View scheduled file imports", description = "Lists all file imports that have been scheduled", tags = "Schedule")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "List retrieved successfully",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = FileImportScheduleResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX + "/file-imports", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FileImportScheduleResponse>> listFileImportSchedules();

	@Operation(summary = "Fetch the specific file import schedule", description = "Returns the DTO of a given file import schedule", tags = "Schedule")
	@Parameter(name = "id", example = "123", description = "ID of the file import schedule", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got the details of the file import schedule",
										content = {@Content(schema = @Schema(implementation = FileImportScheduleResponse.class),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX + "/file-imports/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<FileImportScheduleResponse> getFileImportScheduleById(@PathVariable(value = "id") long id);
}

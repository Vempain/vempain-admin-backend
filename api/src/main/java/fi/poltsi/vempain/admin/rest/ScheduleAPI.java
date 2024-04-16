package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.request.ScheduleTriggerRequest;
import fi.poltsi.vempain.admin.api.response.ScheduleTriggerResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import static fi.poltsi.vempain.admin.api.Constants.REST_SCHEDULE_PREFIX;

@Tag(name = "Schedule", description = "Schedule API for Vempain schedules")
public interface ScheduleAPI {
	@Operation(summary = "Fetch list of all schedules", description = "Returns a list of all active schedules", tags = "Schedule")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of schedules",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = List.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse( responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = REST_SCHEDULE_PREFIX, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<ScheduleTriggerResponse>> getSchedules();

	@Operation(summary = "Trigger schedule", description = "Trigger the selected schedule now", tags = "Schedule")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Schedule to be triggered",
														  required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Schedule triggered successfully",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = ScheduleTriggerResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "404", description = "Given schedule does not exist", content = @Content),
						   @ApiResponse( responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping(value = REST_SCHEDULE_PREFIX, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ScheduleTriggerResponse> triggerSchedule(@RequestBody @NotNull ScheduleTriggerRequest schedule);
}

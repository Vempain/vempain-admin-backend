package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.common.api.response.TaskAcceptedResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@Getter
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Response of a publish request: either the background task that performs the publishing (HTTP 202) or the "
					  + "result of scheduling it (HTTP 200)")
public class PublishResponse {
	@Schema(name = "result", description = "Result of the action", requiredMode = Schema.RequiredMode.REQUIRED)
	private final PublishResultEnum result;
	@Schema(name = "message", description = "Message of the publishing", example = "Publishing successful", requiredMode = Schema.RequiredMode.REQUIRED)
	private final String            message;
	@Schema(name = "timestamp",
	        description = "Date time when publishing was done",
	        example = "2021-06-11T14:26:07.983Z",
	        requiredMode = Schema.RequiredMode.REQUIRED)
	private final Instant timestamp;
	@Schema(name = "task", description = "Background task that performs the publishing; absent when the publishing was scheduled for later",
			requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private final TaskAcceptedResponse task;
}

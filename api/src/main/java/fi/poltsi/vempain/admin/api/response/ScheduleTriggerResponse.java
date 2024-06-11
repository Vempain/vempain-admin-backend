package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Schedule response")
public class ScheduleTriggerResponse {
	@Schema(description = "Schedule id", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long id;
	@Schema(description = "Schedule name", example = "AclConsistencySchedule", requiredMode = Schema.RequiredMode.REQUIRED)
	private String scheduleName;
	@Schema(description = "Schedule status", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
	private String status;
}

package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Data
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Schedule response")
public class ScheduleTriggerResponse {
	@Schema(description = "Schedule name", example = "AclConsistencySchedule", requiredMode = Schema.RequiredMode.REQUIRED)
	private String scheduleName;
	@Schema(description = "Schedule status", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
	private String status;
}

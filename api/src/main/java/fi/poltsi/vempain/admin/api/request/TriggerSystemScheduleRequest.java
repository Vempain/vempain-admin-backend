package fi.poltsi.vempain.admin.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "System schedule trigger request")
public class TriggerSystemScheduleRequest {
	@Schema(description = "System schedule name", example = "AclConsistencySchedule", requiredMode = Schema.RequiredMode.REQUIRED)
	private String scheduleName;
	@Schema(description = "Trigger delay", example = "10")
	private Long delay;
}

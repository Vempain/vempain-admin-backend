package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Refresh response")
public class RefreshResponse {
	@Schema(description = "Result of the refresh", example = "OK", requiredMode = Schema.RequiredMode.REQUIRED)
	private PublishResultEnum           result;
	@Schema(description = "Number of refreshed items", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	private long                        refreshedItems;
	@Schema(description = "Number of failed items", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
	private long failedItems;
	@Schema(description = "List of refresh details", example = "List<RefreshDetailResponse>", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<RefreshDetailResponse> details;
}

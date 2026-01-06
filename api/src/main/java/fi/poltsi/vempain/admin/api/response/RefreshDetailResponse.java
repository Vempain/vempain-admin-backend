package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Refresh details response")
public class RefreshDetailResponse {
	@Schema(description = "Result of the refresh", example = "OK", requiredMode = Schema.RequiredMode.REQUIRED)
	private PublishResultEnum result;
	@Schema(description = "Item id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	private long   itemId;
	@Schema(description = "Item type", example = "GALLERY", requiredMode = Schema.RequiredMode.REQUIRED)
	private String itemType;
	@Schema(description = "Result description", example = "Image refreshed successfully", requiredMode = Schema.RequiredMode.REQUIRED)
	private String resultDescription;
}

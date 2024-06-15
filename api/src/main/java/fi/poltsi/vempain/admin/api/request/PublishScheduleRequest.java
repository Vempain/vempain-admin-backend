package fi.poltsi.vempain.admin.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "System schedule trigger request")
public class PublishScheduleRequest {
	@Schema(description = "ID of the publish schedule", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long id;

	@Schema(description = "Time when the item should be published", example = "2021-01-01T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
	private Instant publishTime;

	@Schema(description = "Status of the publish task", example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
	private PublishStatusEnum publishStatus;

	@Schema(description = "Message of the publish task", example = "Publishing item", requiredMode = Schema.RequiredMode.REQUIRED)
	private String publishMessage;

	@Schema(description = "Type of the item to be published", example = "PAGE", requiredMode = Schema.RequiredMode.REQUIRED)
	private ContentTypeEnum publishType;

	@Schema(description = "ID of the item to be published", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long publishId;
}

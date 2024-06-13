package fi.poltsi.vempain.admin.api.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Publish request")
public class PublishRequest {
	@Schema(description = "Publish ID", example = "123")
	private long id;
	@Schema(description = "Should the item have a specific publishing time", example = "true")
	private boolean publishSchedule;
	@Schema(description = "Publish date time", example = "2021.05.28T13:13:13.123Z+002")
	private Instant publishDateTime;
}

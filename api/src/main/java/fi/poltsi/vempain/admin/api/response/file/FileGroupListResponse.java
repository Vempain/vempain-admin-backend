package fi.poltsi.vempain.admin.api.response.file;

import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Lightweight gallery response for lists and selectors")
public class FileGroupListResponse extends AbstractResponse {
	@Schema(description = "Gallery short name", example = "Summer holiday")
	private String shortName;
	@Schema(description = "Gallery description", example = "Photos from the summer holiday")
	private String description;
	@Schema(description = "Number of files associated with the gallery", example = "42")
	private long   fileCount;
}

package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Abstract file response")
public abstract class AbstractFileResponse {
	@Schema(description = "File type specific ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long id;
	@Schema(description = "Common data of the file", example = "{FileCommonResponse}", requiredMode = Schema.RequiredMode.REQUIRED)
	private FileCommonResponse common;
}

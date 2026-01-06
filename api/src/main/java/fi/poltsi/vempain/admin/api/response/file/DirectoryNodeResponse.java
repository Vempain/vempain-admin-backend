package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Schema(description = "Directory node response")
public class DirectoryNodeResponse {
	@Schema(description = "Name of the directory", example = "someDirectory", requiredMode = Schema.RequiredMode.REQUIRED)
	private String                      directoryName;
	private List<DirectoryNodeResponse> children;
}

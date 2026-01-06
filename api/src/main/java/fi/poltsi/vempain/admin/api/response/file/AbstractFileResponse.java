package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Abstract class representing a file response in the Vempain Admin API.
 * Contains common properties for file responses, such as ID and site file data.
 */

@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Abstract file response")
public abstract class AbstractFileResponse {
	@Schema(description = "File type specific ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long             id;
	@Schema(description = "Site file data of the file", example = "{SiteFileResponse}", requiredMode = Schema.RequiredMode.REQUIRED)
	private SiteFileResponse siteFile;
}

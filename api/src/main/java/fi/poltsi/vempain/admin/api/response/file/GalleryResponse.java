package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Response containing the gallery data")
public class GalleryResponse extends AbstractResponse {
	@Schema(description = "Gallery short name", example = "Short name", requiredMode = Schema.RequiredMode.REQUIRED)
	private String shortName;
	@Schema(description = "Gallery description", example = "Gallery description string", requiredMode = Schema.RequiredMode.REQUIRED)
	private String                 description;
	@Schema(description = "List of file common ID belonging to this gallery", example = "{1, 2, 3, 4}", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<SiteFileResponse> siteFiles;
}

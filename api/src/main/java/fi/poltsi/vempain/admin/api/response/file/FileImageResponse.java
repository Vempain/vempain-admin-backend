package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Response containing the image file data")
public class FileImageResponse extends AbstractFileResponse {
	@Schema(description = "Parent ID of the image, in the common file table", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long parentId;
	@Schema(description = "Width of the image, in pixels", example = "800", requiredMode = Schema.RequiredMode.REQUIRED)
	private long width;
	@Schema(description = "Height of the image, in pixels", example = "600", requiredMode = Schema.RequiredMode.REQUIRED)
	private long height;
}

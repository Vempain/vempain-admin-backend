package fi.poltsi.vempain.admin.api.request.file;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paged site file request")
public class SiteFilePagedRequest extends PagedRequest {
	@Schema(description = "File type to list", example = "IMAGE", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private FileTypeEnum fileType;

	@Schema(description = "Site file field used by search", example = "file_name")
	private String filterColumn;
}

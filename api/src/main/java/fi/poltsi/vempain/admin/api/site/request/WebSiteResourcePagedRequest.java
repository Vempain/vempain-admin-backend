package fi.poltsi.vempain.admin.api.site.request;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paged site resource request")
public class WebSiteResourcePagedRequest extends PagedRequest {
	@Schema(description = "Resource type to list", example = "SITE_FILE")
	private WebSiteResourceEnum type;

	@Schema(description = "File type filter for site files", example = "IMAGE")
	private FileTypeEnum fileType;

	@Schema(description = "ACL ID filter", example = "42")
	private Long aclId;
}

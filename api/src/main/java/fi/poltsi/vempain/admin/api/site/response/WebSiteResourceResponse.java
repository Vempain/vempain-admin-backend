package fi.poltsi.vempain.admin.api.site.response;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Representation of a site resource that can be ACL protected")
@NoArgsConstructor
@AllArgsConstructor
public class WebSiteResourceResponse {
	@Schema(description = "Type of resource", example = "SITE_FILE")
	WebSiteResourceEnum resourceType;

	@Schema(description = "Identifier of the resource", example = "123")
	Long resourceId;

	@Schema(description = "Primary display name", example = "Talvirenkaat-2014")
	String name;

	@Schema(description = "Optional path or URL", example = "gallery/2025/08/Talvirenkaat-2014")
	String path;

	@Schema(description = "ACL identifier protecting the resource", example = "42")
	Long aclId;

	@Schema(description = "Optional file type (short name)", example = "image")
	String fileType;
}


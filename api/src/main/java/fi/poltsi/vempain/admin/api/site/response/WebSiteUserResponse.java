package fi.poltsi.vempain.admin.api.site.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Represents a site web user")
public class WebSiteUserResponse {
	@Schema(description = "User identifier", example = "3")
	Long id;

	@Schema(description = "Username used for basic authentication", example = "gallery-viewer")
	String username;

	@Schema(description = "Admin user ID who created this entry", example = "1")
	Long creator;

	@Schema(description = "Creation timestamp", example = "2025-11-04T16:24:42Z")
	Instant created;

	@Schema(description = "Admin user ID who last modified this entry", example = "1")
	Long modifier;

	@Schema(description = "Modification timestamp", example = "2025-11-04T16:30:00Z")
	Instant modified;

	@Schema(description = "Whether the user has global permission", example = "false")
	boolean globalPermission;

	@Schema(description = "List of web sources assigned to this user")
	List<WebSiteResourceResponse> resources;
}

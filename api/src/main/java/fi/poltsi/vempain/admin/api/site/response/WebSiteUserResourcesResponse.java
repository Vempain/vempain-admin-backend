package fi.poltsi.vempain.admin.api.site.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Lists resources that a site web user can access")
public class WebSiteUserResourcesResponse {
	@Schema(description = "Identifier of the site web user", example = "7")
	Long userId;

	@Schema(description = "Username of the site web user", example = "gallery-viewer")
	String username;

	@Schema(description = "Resources accessible for the user, grouped by ACL ID")
	List<ResourceAccess> resources;

	@Value
	@Builder
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class ResourceAccess {
		@Schema(description = "ACL ID protecting the resource", example = "42")
		Long aclId;

		@Schema(description = "Type of resource", example = "GALLERY")
		String resourceType;

		@Schema(description = "Identifier of the resource", example = "10")
		Long resourceId;

		@Schema(description = "Human readable name for the resource", example = "Talvirenkaat-2014")
		String resourceName;
	}
}


package fi.poltsi.vempain.admin.api.site.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Lists site web users assigned to a specific site ACL")
public class WebSiteAclUsersResponse {
	@Schema(description = "ACL ID protecting site resources", example = "42")
	Long aclId;

	@Schema(description = "Users who have access via this ACL")
	List<UserSummary> users;

	@Data
	@Builder
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class UserSummary {
		@Schema(description = "Identifier of the site web user", example = "7")
		Long userId;

		@Schema(description = "Username", example = "gallery-viewer")
		String username;
	}
}


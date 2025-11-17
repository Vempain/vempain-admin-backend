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
@Schema(description = "Lists site web users assigned to a specific site ACL")
public class WebSiteAclUsersResponse {
	@Schema(description = "ACL ID protecting site resources", example = "42")
	Long aclId;

	@Schema(description = "Users who have access via this ACL")
	List<UserSummary> users;

	@Value
	@Builder
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class UserSummary {
		@Schema(description = "Identifier of the site web user", example = "7")
		Long userId;

		@Schema(description = "Username", example = "gallery-viewer")
		String username;
	}
}


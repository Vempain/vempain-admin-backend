package fi.poltsi.vempain.admin.api.site.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Represents a site ACL assignment between a site ACL ID and a site web user")
public class WebSiteAclResponse {
	@Schema(description = "Unique identifier for this assignment", example = "12")
	Long id;

	@Schema(description = "Site ACL ID protecting site resources", example = "42")
	Long aclId;

	@Schema(description = "Site web user ID that receives this access", example = "3")
	Long userId;

	@Schema(description = "Admin user ID who created this entry", example = "1")
	Long creator;

	@Schema(description = "Creation timestamp", example = "2025-11-04T16:24:42Z")
	Instant created;

	@Schema(description = "Admin user ID who last modified this entry", example = "1")
	Long modifier;

	@Schema(description = "Modification timestamp", example = "2025-11-04T16:30:00Z")
	Instant modified;
}


package fi.poltsi.vempain.admin.api.site.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request payload for linking a site web user to a site ACL")
public class WebSiteAclRequest {
	@Schema(description = "Identifier of the ACL that protects site resources", example = "42")
	@NotNull(message = "ACL ID is required")
	@Min(value = 1, message = "ACL ID must be positive")
	private Long aclId;

	@Schema(description = "Identifier of the site web user receiving access", example = "7")
	@NotNull(message = "User ID is required")
	@Min(value = 1, message = "User ID must be positive")
	private Long userId;
}


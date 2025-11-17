package fi.poltsi.vempain.admin.api.site.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request payload for creating or updating a site web user")
public class WebSiteUserRequest {
	@Schema(description = "Username used for site authentication", example = "gallery-viewer")
	@NotBlank(message = "Username is required")
	@Size(max = 256, message = "Username can be at most 256 characters")
	private String username;

	@Schema(description = "Plaintext password; required when creating a user or when explicitly changing it", example = "s3cr3t")
	@Size(max = 255, message = "Password can be at most 255 characters")
	private String password;
}


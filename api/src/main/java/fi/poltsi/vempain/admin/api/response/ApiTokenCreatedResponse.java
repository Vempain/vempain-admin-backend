package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Answer to the creation of an API token: the only time the token string leaves the server. Only its hash is stored.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "ApiTokenCreatedResponse", description = "Newly created API token with its secret, shown once")
public class ApiTokenCreatedResponse {
	@Schema(description = "The token string to configure in the calling service; it can not be retrieved again", example = "vat_k3Jd9Qx2...")
	private String           token;
	@Schema(description = "The stored token")
	private ApiTokenResponse apiToken;
}

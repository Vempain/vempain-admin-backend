package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/**
 * A service-to-service API token as listed in the admin UI. The token string is never part of this; only its identifying prefix is.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "ApiTokenResponse", description = "Service-to-service API token (without the secret)")
public class ApiTokenResponse {
	@Schema(description = "Token ID", example = "3")
	private long    id;
	@Schema(description = "First characters of the token, to recognise it in configuration files", example = "vat_k3Jd9Qx2")
	private String  tokenPrefix;
	@Schema(description = "What the token is used for", example = "vempain-file-backend production")
	private String  description;
	@Schema(description = "Network the token may be used from (CIDR)", example = "10.0.0.0/24")
	private String  network;
	@Schema(description = "When the token stops working", example = "2027-01-01T00:00:00Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant expiresAt;
	@Schema(description = "Admin user the token acts as; resources created with the token belong to this user", example = "1")
	private long    ownerUserId;
	@Schema(description = "When the token was created", example = "2026-10-09T12:00:00Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant created;
	@Schema(description = "When the token was last used, null when never", example = "2026-10-09T12:30:00Z", nullable = true)
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant lastUsed;
	@Schema(description = "Whether the token has expired", example = "false")
	private boolean expired;
}

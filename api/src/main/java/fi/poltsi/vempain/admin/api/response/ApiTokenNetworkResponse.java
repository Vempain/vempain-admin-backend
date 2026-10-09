package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * The network the admin backend proposes for a new API token: the private network it shares with the calling services when the
 * deployment declares one, an explicitly configured network, or nothing (the administrator enters it by hand).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "ApiTokenNetworkResponse", description = "Suggested network for a new API token")
public class ApiTokenNetworkResponse {
	public enum Source {
		/**
		 * Taken from vempain.admin.api-token.default-network
		 */
		CONFIGURED,
		/**
		 * Detected from the admin backend's own private network interfaces (vempain.admin.api-token.private-network=true)
		 */
		PRIVATE_NETWORK,
		/**
		 * No suggestion; the deployment spans hosts or public networks
		 */
		NONE
	}

	@Schema(description = "Network to pre-fill, null when there is no suggestion", example = "10.0.9.0/24", nullable = true)
	private String          network;
	@Schema(description = "Where the suggestion comes from", example = "PRIVATE_NETWORK")
	private Source          source;
	@Schema(description = "Every private network the admin backend is attached to, for deployments with several overlay networks")
	private List<Candidate> candidates;

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	@Schema(name = "ApiTokenNetworkCandidate", description = "A private network the admin backend is attached to")
	public static class Candidate {
		@Schema(description = "Network in CIDR notation", example = "10.0.9.0/24")
		private String network;
		@Schema(description = "Interface the network is reached through", example = "eth1")
		private String interfaceName;
		@Schema(description = "The admin backend's own address on that network", example = "10.0.9.5")
		private String address;
	}
}

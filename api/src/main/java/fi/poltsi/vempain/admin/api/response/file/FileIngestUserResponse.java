package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * The subset of an admin user account that another service needs in order to offer it as an ACL grantee of ingested resources. It
 * deliberately carries no contact or address data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestUserResponse", description = "Admin user that can be granted privileges on ingested site files and galleries")
public class FileIngestUserResponse {
	@Schema(description = "User ID", example = "12")
	private long   id;
	@Schema(description = "Login name", example = "arnold")
	private String loginName;
	@Schema(description = "Full name", example = "Arnold Dunkelswetter")
	private String name;
	@Schema(description = "Nick name", example = "Ahnold")
	private String nick;
}

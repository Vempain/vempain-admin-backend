package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is a class used to define a user in the context of a {{aclLine}} where we only need a few fields from the actual {{User}} object
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Reduced AclUser response")
public class ReducedAclUser {
	@Schema(description = "User ID", example = "123")
	private Long   userId;
	@Schema(description = "User name", example = "John Doe")
	private String name;
	@Schema(description = "Nickname", example = "Johnnie")
	private String nick;
}

package fi.poltsi.vempain.admin.api.request.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * An additional admin-side user that is granted privileges on the resources (site file and gallery) created by a file ingest. The
 * ingesting service account always receives every privilege; these entries are added next to it on the same ACL.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestAclRequest", description = "Additional admin user and the privileges it gets on the ingested site file and gallery")
public class FileIngestAclRequest {
	@Schema(description = "ID of an existing, active admin user account", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Positive
	private Long userId;

	@Schema(description = "Privilege to read", example = "true")
	private boolean readPrivilege;

	@Schema(description = "Privilege to create", example = "false")
	private boolean createPrivilege;

	@Schema(description = "Privilege to modify", example = "false")
	private boolean modifyPrivilege;

	@Schema(description = "Privilege to delete", example = "false")
	private boolean deletePrivilege;

	@JsonIgnore
	@AssertTrue(message = "At least one privilege must be granted")
	public boolean isAnyPrivilege() {
		return readPrivilege || createPrivilege || modifyPrivilege || deletePrivilege;
	}
}

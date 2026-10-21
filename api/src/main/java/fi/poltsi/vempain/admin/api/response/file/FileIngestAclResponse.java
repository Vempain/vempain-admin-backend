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
 * One grantee row of the ACL of an ingested resource (site file or gallery): an admin user or an admin unit and its privileges. The row of the ingesting
 * account itself is never listed, because that account always holds every privilege and is not an additional grantee. The shape matches
 * {@code FileIngestAclRequest}, so a client can send the rows back unchanged with the next ingest.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestAclResponse", description = "Admin user or unit holding privileges on an ingested site file or gallery")
public class FileIngestAclResponse {

    @Schema(description = "ID of the admin user; exactly one of user_id and unit_id is set", example = "12", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long userId;

    @Schema(description = "ID of the admin unit; exactly one of user_id and unit_id is set", example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long unitId;

    @Schema(description = "Privilege to read", example = "true")
    private boolean readPrivilege;

    @Schema(description = "Privilege to create", example = "false")
    private boolean createPrivilege;

    @Schema(description = "Privilege to modify", example = "false")
    private boolean modifyPrivilege;

    @Schema(description = "Privilege to delete", example = "false")
    private boolean deletePrivilege;
}

package fi.poltsi.vempain.admin.api.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fi.poltsi.vempain.admin.api.response.file.FileIngestAclResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * The current additional grantees of a data set, in the shape of the {@code acls} list of {@code DataRequest}, so that the service that created the set (the
 * file backend) can show them to its user and send them back, changed or not, with the next creation of the same set. The calling account's own row is never
 * listed: it always holds every privilege.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "DataSetAclResponse", description = "Additional ACL grantees of a data set")
public class DataSetAclResponse {

    @Schema(description = "Unique identifier of the data set", example = "music_library")
    private String identifier;

    @Schema(description = "ID of the ACL protecting the data set", example = "42")
    private long aclId;

    @Schema(description = "Grantees on the ACL of the data set other than the calling account; empty when nothing was granted")
    private List<FileIngestAclResponse> acls;
}

package fi.poltsi.vempain.admin.api.response.file;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * The current ACL grantees of a site file created by an earlier ingest, so that the ingesting service can show its user what was granted the last time and
 * resend it with the next ingest of the same file.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestSiteFileAclResponse", description = "Additional ACL grantees of an ingested site file")
public class FileIngestSiteFileAclResponse {

    @Schema(description = "ID of the site file", example = "12345")
    private long siteFileId;

    @Schema(description = "ID of the source file in the ingesting service (files.id of the file backend)", example = "678")
    private long fileId;

    @Schema(description = "Grantees on the ACL of the site file other than the ingesting account; empty when nothing was granted")
    private List<FileIngestAclResponse> acls;
}

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
 * The current ACL grantees of a gallery created by an earlier ingest together with the site files linked to it, so that the ingesting service can show its user
 * what a file group's previous publish granted and resend it with the next publish.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestGalleryAclResponse", description = "Additional ACL grantees and site files of an ingested gallery")
public class FileIngestGalleryAclResponse {

    @Schema(description = "ID of the gallery", example = "1001")
    private long galleryId;

    @Schema(description = "Short name of the gallery", example = "Summer 2025")
    private String shortname;

    @Schema(description = "IDs of the site files linked to the gallery, in gallery order", example = "[12345, 12346]")
    private List<Long> siteFileIds;

    @Schema(description = "Grantees on the ACL of the gallery other than the ingesting account; empty when nothing was granted")
    private List<FileIngestAclResponse> acls;
}

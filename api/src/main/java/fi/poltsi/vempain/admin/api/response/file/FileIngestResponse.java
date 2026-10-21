package fi.poltsi.vempain.admin.api.response.file;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Answer of a file ingest. The keys are snake_case like every other Vempain contract; the camelCase keys that earlier releases wrote are still read, so a file
 * backend built against this version also understands the answer of an older admin backend.
 */
@Schema(name = "FileIngestResponse", description = "Result of file ingest operation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FileIngestResponse {

    @Schema(description = "ID of the optional gallery created or added on Vempain Admin-side", example = "12345")
    @JsonAlias("galleryId")
    private Long galleryId;

    @Schema(description = "ID of the stored site file", example = "12345")
    @JsonAlias("siteFileId")
    private Long siteFileId;

    @Schema(description = "Whether the operation updated an existing file", example = "true")
    private boolean updated;

    @Schema(description = "IDs of every site file linked to the gallery after this ingest, in gallery order; null when no gallery was involved",
            example = "[12345, 12346]")
    @JsonAlias("gallerySiteFileIds")
    private List<Long> gallerySiteFileIds;
}

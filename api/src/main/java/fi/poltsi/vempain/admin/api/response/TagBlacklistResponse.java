package fi.poltsi.vempain.admin.api.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One blacklisted tag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A tag that is removed from everything published to the website")
public class TagBlacklistResponse {

    @Schema(description = "Entry ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private long id;
    @Schema(description = "The blacklisted tag", example = "private", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tagName;
    @Schema(description = "Admin user who added the tag", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long createdBy;
    @Schema(description = "When the tag was added", example = "2026-10-26T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant created;
}

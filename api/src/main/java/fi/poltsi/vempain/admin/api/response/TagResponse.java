package fi.poltsi.vempain.admin.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One tag known to the admin backend: the tags of the published files (sent by the file backend with every publish) and the tags given to pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A tag known to the admin backend, from the published files or the pages")
public class TagResponse {

    @Schema(description = "Tag ID", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private long id;
    @Schema(description = "The tag", example = "summer", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tagName;
}

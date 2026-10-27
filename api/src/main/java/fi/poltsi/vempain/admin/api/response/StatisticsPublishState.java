package fi.poltsi.vempain.admin.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Published versus not yet published resources of one publishable type (pages, galleries and site files).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Published and unpublished resources of one type")
public class StatisticsPublishState {

    @Schema(description = "Resource type: page, gallery or site_file", example = "page", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceType;
    @Schema(description = "Resources that have been published to the website", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
    private long published;
    @Schema(description = "Resources that have never been published", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    private long notPublished;
}

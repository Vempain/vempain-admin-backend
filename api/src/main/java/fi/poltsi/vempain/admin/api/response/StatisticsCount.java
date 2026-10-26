package fi.poltsi.vempain.admin.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One row of a statistics breakdown: how many resources share a name (a file type, a form, a layout, a component, a path, ...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Number of resources sharing a name")
public class StatisticsCount {

    @Schema(description = "The grouping value: a resource type, file type, form, layout or component name, or a path", example = "IMAGE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    @Schema(description = "Number of resources with that value", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private long count;
}

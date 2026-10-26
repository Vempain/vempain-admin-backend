package fi.poltsi.vempain.admin.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One point of a monthly time series: how many resources of a type were created or published in a calendar month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Resources of one type in one calendar month")
public class StatisticsMonthlyCount {

    @Schema(description = "Resource type: page, gallery, site_file, component, form or layout", example = "site_file",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceType;
    @Schema(description = "Calendar month in UTC as YYYY-MM", example = "2026-10", requiredMode = Schema.RequiredMode.REQUIRED)
    private String month;
    @Schema(description = "Resources created or published in that month", example = "35", requiredMode = Schema.RequiredMode.REQUIRED)
    private long count;
}

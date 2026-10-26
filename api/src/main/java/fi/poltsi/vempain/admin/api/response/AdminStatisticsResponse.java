package fi.poltsi.vempain.admin.api.response;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Current counts of the resources managed by the admin backend (pages, galleries, site files, components, forms and layouts) with their breakdowns. The counts
 * are system wide and not filtered by ACL, like the inventory metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Counts and breakdowns of the resources of the admin backend")
public class AdminStatisticsResponse {

    @Schema(description = "When the counts were taken", example = "2026-10-26T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant generatedAt;
    @Schema(description = "Total number of resources by type (page, gallery, site_file, component, form, layout)")
    private List<StatisticsCount> resourceCounts;
    @Schema(description = "Site files by file type")
    private List<StatisticsCount> filesByType;
    @Schema(description = "Published versus unpublished pages, galleries and site files")
    private List<StatisticsPublishState> publishStates;
    @Schema(description = "Publications per calendar month and resource type")
    private List<StatisticsMonthlyCount> publishedByMonth;
    @Schema(description = "Created resources per calendar month and resource type")
    private List<StatisticsMonthlyCount> createdByMonth;
    @Schema(description = "Resources per creating user and resource type")
    private List<StatisticsUserCount> resourcesByUser;
    @Schema(description = "Pages per form")
    private List<StatisticsCount> pagesByForm;
    @Schema(description = "Forms per layout")
    private List<StatisticsCount> formsByLayout;
    @Schema(description = "Forms using each component")
    private List<StatisticsCount> formsByComponent;
    @Schema(description = "Pages per parent path (the page path without its last segment)")
    private List<StatisticsCount> pagesByPath;
    @Schema(description = "Site files per file path")
    private List<StatisticsCount> filesByPath;
}

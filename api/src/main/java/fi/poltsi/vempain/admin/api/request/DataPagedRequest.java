package fi.poltsi.vempain.admin.api.request;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Paged request of the data set listing ({@code POST /content-management/data/paged}); a plain {@code PagedRequest} body is accepted as well.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paged data set request")
public class DataPagedRequest extends PagedRequest {

    @Schema(description = "Only data sets of this type (exact, case insensitive), for example time_series or tabulated", example = "time_series",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String type;

    @Schema(description = "Data set field the search is restricted to: identifier, type or description; without it the search covers all three",
            example = "identifier", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String filterColumn;

    @Schema(description = "When true, only data sets that were modified after their last release to the website are listed: both timestamps are set and "
            + "the release is older than the modification. Data sets never released or never modified since are left out.", example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean modifiedAfterPublish;
}

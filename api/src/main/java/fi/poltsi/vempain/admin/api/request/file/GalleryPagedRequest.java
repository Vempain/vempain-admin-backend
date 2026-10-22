package fi.poltsi.vempain.admin.api.request.file;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Paged request of the lightweight gallery list ({@code POST /content-management/galleries/paged-list}); a plain {@code PagedRequest} body is still accepted.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paged gallery list request")
public class GalleryPagedRequest extends PagedRequest {

    @Schema(description = "When true, only galleries that were modified after their last release to the website are listed: both timestamps are set and "
            + "the release is older than the modification. Galleries never released or never modified since are left out.", example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean modifiedAfterPublish;
}

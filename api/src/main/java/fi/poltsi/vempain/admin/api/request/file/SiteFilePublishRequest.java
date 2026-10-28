package fi.poltsi.vempain.admin.api.request.file;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request payload containing the IDs of the site files to publish")
public class SiteFilePublishRequest {

    @NotEmpty
    @Schema(description = "Site file IDs to publish", example = "[1,2,3]")
    private List<Long> fileIds;
}

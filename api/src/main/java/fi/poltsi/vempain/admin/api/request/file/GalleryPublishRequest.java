package fi.poltsi.vempain.admin.api.request.file;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request payload containing gallery IDs to publish")
public class GalleryPublishRequest {

    @NotEmpty
    @Schema(description = "Gallery IDs to publish", example = "[1,2,3]")
    private List<Long> galleryIds;
}

package fi.poltsi.vempain.admin.api.response.file;

import java.time.Instant;

import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Lightweight gallery response for lists and selectors")
public class FileGroupListResponse extends AbstractResponse {

    @Schema(description = "Gallery short name", example = "Summer holiday")
    private String shortName;
    @Schema(description = "Gallery description", example = "Photos from the summer holiday")
    private String description;
    @Schema(description = "Number of files associated with the gallery", example = "42")
    private long fileCount;
    @Schema(description = "Date when the gallery was last published to the website, null if never", example = "2022-05-13T16:03:44Z")
    private Instant published;
}

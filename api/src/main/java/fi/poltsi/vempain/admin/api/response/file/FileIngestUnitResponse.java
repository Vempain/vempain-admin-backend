package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * The subset of an admin unit that another service needs in order to offer it as an ACL grantee of ingested resources. Membership is resolved by the admin
 * backend when the ACL is checked, so the members are deliberately not listed here.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestUnitResponse", description = "Admin unit that can be granted privileges on ingested site files and galleries")
public class FileIngestUnitResponse {

    @Schema(description = "Unit ID", example = "3")
    private long id;
    @Schema(description = "Unit name", example = "Editors")
    private String name;
    @Schema(description = "Unit description", example = "Everyone who edits the site content")
    private String description;
}

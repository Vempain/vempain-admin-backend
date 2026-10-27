package fi.poltsi.vempain.admin.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * How many resources of one type a user has created.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Resources of one type created by one user")
public class StatisticsUserCount {

    @Schema(description = "Id of the creating user account", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private long userId;
    @Schema(description = "Nick of the user, null when the account no longer exists", example = "admin")
    private String nick;
    @Schema(description = "Resource type: page, gallery, site_file, component, form or layout", example = "page", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceType;
    @Schema(description = "Resources of that type created by the user", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private long count;
}

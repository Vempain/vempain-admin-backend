package fi.poltsi.vempain.admin.api.site.response;

import java.time.Instant;

import fi.poltsi.vempain.admin.api.site.WebSiteConfigurationTypeEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Web site configuration response")
public class WebSiteConfigurationResponse extends WebSiteConfigurationRequest {

    @Schema(description = "Configuration key", example = "site.title")
    String configKey;

    @Schema(description = "Type of the configuration", example = "STRING")
    WebSiteConfigurationTypeEnum configType;

    @Schema(description = "Default value of the configuration", example = "My Vempain Site")
    String configDefault;

    @Schema(description = "Current value of the configuration", example = "Holiday Photos Site")
    String configValue;

    @Schema(description = "Nick of the admin user who created the configuration, null for the seeded rows", example = "admin")
    String creator;

    @Schema(description = "When the configuration was created, null for the seeded rows", example = "2026-01-05T08:00:00Z")
    Instant created;

    @Schema(description = "Nick of the admin user who last updated the value, null when never updated", example = "admin")
    String modifier;

    @Schema(description = "When the value was last updated, null when never updated", example = "2026-01-06T08:00:00Z")
    Instant modified;
}

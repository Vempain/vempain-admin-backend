package fi.poltsi.vempain.admin.api.site.response;

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
}

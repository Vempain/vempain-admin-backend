package fi.poltsi.vempain.admin.api.site.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.admin.api.site.WebSiteConfigurationTypeEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder
@Jacksonized
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

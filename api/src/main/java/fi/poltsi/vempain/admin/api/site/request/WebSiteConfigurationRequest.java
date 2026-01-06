package fi.poltsi.vempain.admin.api.site.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@SuperBuilder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Web site configuration request, used only for updating configuration values")
@NoArgsConstructor
@AllArgsConstructor
public class WebSiteConfigurationRequest {

	@Schema(description = "Unique identifier of the configuration", example = "1")
	long id;

	@Schema(description = "Current value of the configuration", example = "Holiday Photos Site")
	String configValue;
}

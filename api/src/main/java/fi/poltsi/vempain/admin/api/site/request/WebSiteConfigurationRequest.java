package fi.poltsi.vempain.admin.api.site.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Web site configuration request, used only for updating configuration values")
public class WebSiteConfigurationRequest {

	@Schema(description = "Unique identifier of the configuration", example = "1")
	long id;

	@Schema(description = "Current value of the configuration", example = "Holiday Photos Site")
	String configValue;
}

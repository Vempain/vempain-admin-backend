package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting a form including the components assigned to it")
public class FormResponse extends AbstractResponse {
	@Schema(description = "Form name", example = "My form")
	private String                  name;
	@Schema(description = "Layout ID", example = "1")
	private Long                    layoutId;
	@Schema(description = "List of Component Responses", example = "[Array of ComponentResponse items]")
	@Valid
	private List<ComponentResponse> components;
}

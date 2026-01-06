package fi.poltsi.vempain.admin.api.request.file;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Short format to be used when we want to populate select options which use value-label pairs")
public class OptionItems {
	@Schema(description = "Value, usually the ID", example = "123")
	private long value;
	@Schema(description = "Displayed label, usually the name of the item", example = "Option 1")
	private String label;
}

package fi.poltsi.vempain.admin.api.request.file;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

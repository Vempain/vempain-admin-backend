package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Pageation information")
public class PageableResponse {
	@Schema(description = "Current number of the pageable", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 0)
	private long number;
	@Schema(description = "How many elements there are altogether", example = "14", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 0)
	private long totalElements;
	@Schema(description = "How many pageables there are altogether", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 0)
	private int totalPageables;
	@Schema(description = "Page number of the current page list", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 0)
	private int currentPageable;
	@Schema(description = "Number of elements in the current pageable", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
	@Min(value = 1)
	private int size;
}

package fi.poltsi.vempain.admin.api.site.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paginated response for site resources")
@NoArgsConstructor
@AllArgsConstructor
public class WebSiteResourcePageResponse {
	@Schema(description = "Current page number (0-indexed)", example = "0")
	int pageNumber;

	@Schema(description = "Requested page size", example = "25")
	int pageSize;

	@Schema(description = "Total amount of pages", example = "10")
	long totalPages;

	@Schema(description = "Total amount of elements", example = "200")
	long totalElements;

	@ArraySchema(schema = @Schema(implementation = WebSiteResourceResponse.class))
	List<WebSiteResourceResponse> items;
}



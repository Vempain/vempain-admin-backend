package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Paginated gallery listing response")
public class GalleryPageResponse {
	@Schema(description = "Current page number (0-indexed)", example = "0")
	int pageNumber;

	@Schema(description = "Requested page size", example = "25")
	int pageSize;

	@Schema(description = "Total amount of pages", example = "4")
	long totalPages;

	@Schema(description = "Total amount of galleries matching the criteria", example = "87")
	long totalElements;

	@ArraySchema(schema = @Schema(implementation = GalleryResponse.class))
	List<GalleryResponse> items;
}


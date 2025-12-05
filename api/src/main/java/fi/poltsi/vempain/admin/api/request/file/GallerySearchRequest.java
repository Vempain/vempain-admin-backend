package fi.poltsi.vempain.admin.api.request.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@Schema(description = "Parameters for searching galleries")
public class GallerySearchRequest {
	@Schema(description = "Zero-based page number", example = "0")
	@Builder.Default
	int page = 0;

	@Schema(description = "Page size", example = "25")
	@Builder.Default
	int size = 25;

	@Schema(description = "Sort field: id, short_name, description", example = "id")
	@Builder.Default
	String sort = "id";

	@Schema(description = "Sort direction", example = "asc")
	@Builder.Default
	String direction = "asc";

	@Schema(description = "Search term applied to gallery metadata and related files", example = "matkailu")
	String search;

	@Schema(description = "If true, the search is case-sensitive", example = "false")
	@Builder.Default
	boolean caseSensitive = false;
}

